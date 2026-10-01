package es.pmdm.gymprofit.envivo;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

import es.pmdm.gymprofit.model.envivo.SesionEnCurso;
import es.pmdm.gymprofit.model.envivo.UltimaVez;
import es.pmdm.gymprofit.model.rutina.RutinaEjercicio;
import es.pmdm.gymprofit.model.sesion.SesionEntrenamiento;

// ============================================================
// SesionEnCursoRepositorio — la sesión en vivo mientras dura, una sola fuente de verdad
// (GP-012).
//
// La observan la pantalla de la sesión, la barra sobre la navegación, la notificación,
// Inicio, Entrenar y el «+». Todo cambio pasa por aquí, se escribe al fichero de la
// cuenta (AlmacenSesion) y, si cambia algo que se ve fuera de la pantalla, se publica.
// Teclear un número se escribe al fichero pero no se publica: no cambia nada de lo que
// pintan los demás y repintar se llevaría el foco del campo.
//
// Una a la vez: empezar con otra en curso no la pisa, devuelve YA_HAY_OTRA y quien
// llama pregunta.
//
// Guardar (GP-006 + GP-012). La clave de idempotencia nace en el primer intento y se
// guarda con la sesión; desde ese momento la sesión NO se edita hasta guardarla o
// descartarla. Así un reintento manda exactamente lo mismo y nunca pisa un cambio, que
// es lo que GP-012 dejaba abierto. Si el proceso muere con el envío en el aire, al leer
// el fichero la sesión está en GUARDANDO y pasa a FALLO: no se sabe si llegó, y
// reintentar con la misma clave lo resuelve sin duplicar. Al abrir la app se reintenta
// sola una vez por proceso.
// ============================================================
public class SesionEnCursoRepositorio {

    /** Las llamadas que necesita. Interfaz para que los tests den una falsa. */
    public interface Red {
        /** Respuesta de una llamada. */
        interface Respuesta<T> {
            void ok(T valor);

            /** @param codigo HTTP, o -1 si no hubo respuesta. */
            void fallo(int codigo, String mensaje);
        }

        void ejerciciosDeRutina(int rutinaId, Respuesta<List<RutinaEjercicio>> r);

        void ultimaVez(List<Integer> ejercicioIds, Respuesta<List<UltimaVez>> r);

        void guardarCompleta(Map<String, Object> cuerpo, Respuesta<SesionEntrenamiento> r);
    }

    /** La hora, para que los tests la muevan. */
    public interface Reloj { long ahora(); }

    /** Lo que decide el descanso desde Ajustes (GP-013). */
    public interface AjustesDescanso {
        /** «Empezar el descanso al marcar una serie». */
        boolean empezarAlMarcar();

        /** «Descanso sin pauta», en segundos. */
        int sinPautaSegundos();
    }

    /** Lo que dice la pantalla al acabar un descanso: «¡A por la serie 3!». */
    public static final class FinDescanso {
        /** Ejercicio de la serie que toca, o null si no tiene nombre. */
        @Nullable public final String ejercicio;
        public final int ejercicioId;
        /** Número de la serie que toca; 0 si ya no queda ninguna. */
        public final int numero;
        /** Hasta cuándo se enseña. */
        public final long hastaMs;

        FinDescanso(@Nullable String ejercicio, int ejercicioId, int numero, long hastaMs) {
            this.ejercicio = ejercicio;
            this.ejercicioId = ejercicioId;
            this.numero = numero;
            this.hastaMs = hastaMs;
        }
    }

    /** Lo que dura en pantalla el aviso del fin del descanso si no se cierra antes. */
    public static final long FIN_VISIBLE_MS = 8000;

    /** Qué pasó al tocar «Empezar». */
    public enum Empezar {
        /** No había ninguna: empieza. */
        EMPEZADA,
        /** La que está en curso es de esta misma rutina: se vuelve a ella. */
        ES_LA_MISMA,
        /** Hay otra en curso: quien llama pregunta qué hacer. */
        YA_HAY_OTRA
    }

    /** Qué pasó al tocar «Guardar sesión». */
    public enum Guardar { ENVIADO, NADA_MARCADO, YA_EN_CURSO, NO_HAY }

    /** El final de un guardado, para enseñarlo una vez. */
    public static final class Resultado {
        /** La sesión creada, o null si falló. */
        @Nullable public final SesionEntrenamiento sesion;
        /** La sesión en curso tal cual se guardó (para el resumen). */
        @NonNull public final SesionEnCurso guardada;

        Resultado(@Nullable SesionEntrenamiento sesion, @NonNull SesionEnCurso guardada) {
            this.sesion = sesion;
            this.guardada = guardada;
        }

        public boolean ok() { return sesion != null; }
    }

    /** Algo que se consume una vez (el resumen, un aviso). */
    public static final class Evento<T> {
        private T valor;

        Evento(T valor) { this.valor = valor; }

        /** El valor la primera vez; null las siguientes. */
        @Nullable
        public T tomar() {
            T v = valor;
            valor = null;
            return v;
        }
    }

    private static final int MAX_POR_PETICION = 30;

    @Nullable private static SesionEnCursoRepositorio instancia;

    private final AlmacenSesion almacen;
    private final Red red;
    private final Reloj reloj;
    private final TimeZone zona;
    private final Locale locale;

    private final MutableLiveData<SesionEnCurso> sesion = new MutableLiveData<>(null);
    private final MutableLiveData<Evento<Resultado>> resultado = new MutableLiveData<>();

    private AjustesDescanso ajustesDescanso = new AjustesDescanso() {
        @Override public boolean empezarAlMarcar() { return true; }
        @Override public int sinPautaSegundos() { return LogicaDescanso.SIN_PAUTA_DEFECTO; }
    };
    // La alarma del fin del descanso, para cuando la app no está delante.
    private ProgramadorAlarma programador = new ProgramadorAlarma(new ProgramadorAlarma.Alarmas() {
        @Override public boolean puedeExactas() { return true; }
        @Override public void exacta(long cuandoMs) { }
        @Override public void ventana(long cuandoMs, long largoMs) { }
        @Override public void cancelar() { }
    });
    // El aviso del fin del descanso: solo en memoria, es de la pantalla y dura segundos.
    @Nullable private FinDescanso finDescanso;

    private int usuarioId = -1;
    // Una vez por proceso: «al abrir la app».
    private boolean reintentoAlAbrirHecho;
    private boolean pidiendoEjercicios;
    private boolean pidiendoUltimaVez;

    /** La de la app, con su fichero en files/sesion_en_curso. */
    @NonNull
    public static synchronized SesionEnCursoRepositorio get(@NonNull Context ctx) {
        if (instancia == null) {
            Context app = ctx.getApplicationContext();
            instancia = new SesionEnCursoRepositorio(
                    new AlmacenSesion(new File(app.getFilesDir(), "sesion_en_curso")),
                    new RedSesionEnVivo(), System::currentTimeMillis, TimeZone.getDefault(),
                    es.pmdm.gymprofit.utils.FechaUtils.localeDeLaApp(app));
            // Ajustes se leen cada vez: un cambio vale para la siguiente serie que se marque.
            es.pmdm.gymprofit.utils.PreferencesManager prefs = new es.pmdm.gymprofit.utils.PreferencesManager(app);
            instancia.setProgramador(new ProgramadorAlarma(new AlarmasDescanso(app)));
            instancia.setAjustesDescanso(new AjustesDescanso() {
                @Override public boolean empezarAlMarcar() { return prefs.getDescansoAlMarcar(); }
                @Override public int sinPautaSegundos() { return prefs.getDescansoSinPauta(); }
            });
        }
        return instancia;
    }

    public SesionEnCursoRepositorio(@NonNull AlmacenSesion almacen, @NonNull Red red, @NonNull Reloj reloj,
                                    @NonNull TimeZone zona, @NonNull Locale locale) {
        this.almacen = almacen;
        this.red = red;
        this.reloj = reloj;
        this.zona = zona;
        this.locale = locale;
    }

    // ── Cuenta ───────────────────────────────────────────────

    /**
     * Carga la sesión de la cuenta que ha entrado (o ninguna, con -1). Otra cuenta en el
     * mismo móvil tiene otro fichero y no ve la de esta.
     */
    public void usarCuenta(int usuarioId) {
        if (usuarioId == this.usuarioId) return;
        this.usuarioId = usuarioId;
        SesionEnCurso s = usuarioId > 0 ? almacen.leer(usuarioId) : null;
        if (s != null && s.guardado == SesionEnCurso.Guardado.GUARDANDO) {
            // El proceso murió con el envío en el aire: no se sabe si llegó.
            s.guardado = SesionEnCurso.Guardado.FALLO;
            s.codigoFallo = -1;
            almacen.escribir(s);
        }
        if (s != null && LogicaDescanso.caducado(s.descanso, reloj.ahora())) {
            // Acabó hace rato con la app fuera (GP-144): se quita sin avisar.
            s.descanso = null;
            almacen.escribir(s);
        }
        poner(s);
    }

    public int getUsuarioId() { return usuarioId; }

    /** Quien pone y quita la alarma del fin del descanso. */
    public void setProgramador(@NonNull ProgramadorAlarma p) {
        programador = p;
        SesionEnCurso s = actual();
        programador.sincronizar(s != null ? s.descanso : null, reloj.ahora());
    }

    /** El permiso de alarmas exactas puede haber cambiado: la alarma puesta se rehace si toca. */
    public void revisarPermisoAlarma() { programador.revisarPermiso(); }

    /** De dónde sale el descanso sin pauta y si empieza al marcar (Ajustes). */
    public void setAjustesDescanso(@NonNull AjustesDescanso a) { ajustesDescanso = a; }

    /**
     * Borra la sesión en curso de una cuenta que se ha eliminado (GP-008): lo que se
     * borra de verdad no puede quedarse en un fichero del móvil.
     */
    public void borrarDeCuenta(int usuarioId) {
        if (usuarioId <= 0) return;
        almacen.borrar(usuarioId);
        if (usuarioId == this.usuarioId) poner(null);
    }

    // ── Lecturas ─────────────────────────────────────────────

    public LiveData<SesionEnCurso> getSesion() { return sesion; }

    public LiveData<Evento<Resultado>> getResultado() { return resultado; }

    @Nullable
    public SesionEnCurso actual() { return sesion.getValue(); }

    public boolean hay() { return actual() != null; }

    /** Si se puede tocar: hay sesión y no se ha intentado guardar todavía. */
    public boolean editable() {
        SesionEnCurso s = actual();
        return s != null && s.claveIdempotencia == null;
    }

    public long ahora() { return reloj.ahora(); }

    // ── Empezar y descartar ──────────────────────────────────

    /**
     * Empieza una sesión, con el reloj en marcha desde ya. Sin rutina, empieza vacía.
     *
     * @return EMPEZADA, o ES_LA_MISMA / YA_HAY_OTRA si ya había una (que no se toca).
     */
    @NonNull
    public Empezar empezar(@Nullable Integer rutinaId, @Nullable String rutinaNombre,
                           @Nullable String programaNombre) {
        SesionEnCurso enCurso = actual();
        if (enCurso != null) {
            return rutinaId != null && rutinaId.equals(enCurso.rutinaId) ? Empezar.ES_LA_MISMA : Empezar.YA_HAY_OTRA;
        }
        if (usuarioId <= 0) return Empezar.YA_HAY_OTRA;
        SesionEnCurso s = new SesionEnCurso();
        s.usuarioId = usuarioId;
        s.rutinaId = rutinaId;
        s.rutinaNombre = rutinaNombre;
        s.programaNombre = programaNombre;
        s.inicioMs = reloj.ahora();
        s.carga = rutinaId != null ? SesionEnCurso.Carga.CARGANDO : SesionEnCurso.Carga.LISTA;
        publicar(s);
        if (rutinaId != null) cargarEjercicios();
        return Empezar.EMPEZADA;
    }

    /** Tira la sesión en curso: se borra del móvil y deja de verse en todas partes. */
    public void descartar() {
        if (usuarioId > 0) almacen.borrar(usuarioId);
        finDescanso = null;
        poner(null);
    }

    /** Pide los ejercicios de la rutina, si aún no están (primera vez o tras un fallo). */
    public void cargarEjercicios() {
        SesionEnCurso s = actual();
        if (s == null || s.rutinaId == null || s.carga == SesionEnCurso.Carga.LISTA || pidiendoEjercicios) return;
        pidiendoEjercicios = true;
        s.carga = SesionEnCurso.Carga.CARGANDO;
        publicar(s);
        final SesionEnCurso pedida = s;
        red.ejerciciosDeRutina(s.rutinaId, new Red.Respuesta<List<RutinaEjercicio>>() {
            @Override public void ok(List<RutinaEjercicio> lista) {
                pidiendoEjercicios = false;
                if (actual() != pedida) return;
                if (lista != null) {
                    for (RutinaEjercicio re : lista) {
                        if (re.getEjercicioId() <= 0) continue;
                        pedida.ejercicios.add(deRutina(pedida, re));
                    }
                }
                pedida.carga = SesionEnCurso.Carga.LISTA;
                publicar(pedida);
                pedirUltimaVez();
            }

            @Override public void fallo(int codigo, String mensaje) {
                pidiendoEjercicios = false;
                if (actual() != pedida) return;
                // La pantalla enseña el error con reintentar; el reloj sigue.
                pedida.carga = SesionEnCurso.Carga.ERROR;
                publicar(pedida);
            }
        });
    }

    private static SesionEnCurso.Ejercicio deRutina(SesionEnCurso s, RutinaEjercicio re) {
        SesionEnCurso.Ejercicio e = new SesionEnCurso.Ejercicio();
        e.id = s.siguienteId++;
        e.ejercicioId = re.getEjercicioId();
        e.nombre = re.getNombreEjercicio() != null && !re.getNombreEjercicio().isEmpty() ? re.getNombreEjercicio() : null;
        e.seriesPauta = re.getSeries();
        e.repeticionesPauta = re.getRepeticiones();
        e.minimo = re.getRepeticionesMin();
        e.maximo = re.getRepeticionesMax();
        e.medida = re.getMedida();
        e.porLado = re.getPorLado();
        e.descanso = re.getTiempoDescanso();
        e.notasPauta = re.getNotas();
        e.deRutina = true;
        int series = Math.max(1, Math.min(SesionEnCurso.MAX_SERIES, re.getSeries()));
        for (int i = 0; i < series; i++) e.series.add(nuevaSerie(s));
        return e;
    }

    private static SesionEnCurso.Serie nuevaSerie(SesionEnCurso s) {
        SesionEnCurso.Serie serie = new SesionEnCurso.Serie();
        serie.id = s.siguienteId++;
        return serie;
    }

    /**
     * Pregunta la última vez de los ejercicios que aún no la tienen (GP-014), de 30 en 30.
     * Sin red se queda sin preguntar y se vuelve a intentar al abrir la pantalla.
     */
    public void pedirUltimaVez() {
        SesionEnCurso s = actual();
        if (s == null || pidiendoUltimaVez) return;
        List<Integer> faltan = LogicaSesion.sinAnterior(s);
        if (faltan.isEmpty()) return;
        List<Integer> pedidos = new ArrayList<>(faltan.subList(0, Math.min(MAX_POR_PETICION, faltan.size())));
        pidiendoUltimaVez = true;
        final SesionEnCurso pedida = s;
        red.ultimaVez(pedidos, new Red.Respuesta<List<UltimaVez>>() {
            @Override public void ok(List<UltimaVez> lista) {
                pidiendoUltimaVez = false;
                if (actual() != pedida) return;
                LogicaSesion.aplicarUltimaVez(pedida, pedidos, lista);
                publicar(pedida);
                // Si eran más de 30, la siguiente tanda.
                pedirUltimaVez();
            }

            @Override public void fallo(int codigo, String mensaje) {
                // Se deja sin preguntar a propósito: la sesión sigue igual sin «Anterior»
                // (la columna se queda vacía, no dice «—»), y se vuelve a pedir la próxima
                // vez que se abra la pantalla. Avisar de algo que no impide entrenar sobra.
                pidiendoUltimaVez = false;
            }
        });
    }

    // ── Editar ───────────────────────────────────────────────

    @Nullable
    private SesionEnCurso editando() {
        return editable() ? actual() : null;
    }

    @Nullable
    private static SesionEnCurso.Ejercicio ejercicio(SesionEnCurso s, long ejercicioId) {
        for (SesionEnCurso.Ejercicio e : s.ejercicios) if (e.id == ejercicioId) return e;
        return null;
    }

    /** El ejercicio que contiene la serie, o null. */
    @Nullable
    public static SesionEnCurso.Ejercicio ejercicioDeSerie(@NonNull SesionEnCurso s, long serieId) {
        for (SesionEnCurso.Ejercicio e : s.ejercicios) {
            for (SesionEnCurso.Serie serie : e.series) if (serie.id == serieId) return e;
        }
        return null;
    }

    @Nullable
    private static SesionEnCurso.Serie serie(SesionEnCurso s, long serieId) {
        SesionEnCurso.Ejercicio e = ejercicioDeSerie(s, serieId);
        if (e == null) return null;
        for (SesionEnCurso.Serie serie : e.series) if (serie.id == serieId) return serie;
        return null;
    }

    /** Kilos tecleados. Se escriben al fichero sin repintar nada. */
    public void escribirPeso(long serieId, @NonNull String texto) {
        SesionEnCurso s = editando();
        SesionEnCurso.Serie serie = s != null ? serie(s, serieId) : null;
        if (serie == null || serie.peso.equals(texto)) return;
        serie.peso = texto;
        almacen.escribir(s);
    }

    public void escribirRepeticiones(long serieId, @NonNull String texto) {
        SesionEnCurso s = editando();
        SesionEnCurso.Serie serie = s != null ? serie(s, serieId) : null;
        if (serie == null || serie.repeticiones.equals(texto)) return;
        serie.repeticiones = texto;
        almacen.escribir(s);
    }

    public void escribirSegundos(long serieId, @NonNull String texto) {
        SesionEnCurso s = editando();
        SesionEnCurso.Serie serie = s != null ? serie(s, serieId) : null;
        if (serie == null || serie.segundos.equals(texto)) return;
        serie.segundos = texto;
        almacen.escribir(s);
    }

    /** Marca o desmarca una serie; marcar confirma las pistas (ver LogicaSesion). */
    @Nullable
    public LogicaSesion.Marcado marcar(long serieId) {
        return marcar(serieId, locale);
    }

    /**
     * Igual, con el idioma de la pantalla: la pista confirmada se escribe en el campo con
     * su separador decimal («57,5» o «57.5»).
     */
    @Nullable
    public LogicaSesion.Marcado marcar(long serieId, @NonNull Locale locale) {
        SesionEnCurso s = editando();
        SesionEnCurso.Ejercicio e = s != null ? ejercicioDeSerie(s, serieId) : null;
        if (e == null) return null;
        int indice = indiceDe(e, serieId);
        LogicaSesion.Marcado m = LogicaSesion.marcar(e, indice, locale);
        if (m == LogicaSesion.Marcado.HECHA) {
            alHacerSerie(s, serieId);
            publicar(s);
        } else if (m == LogicaSesion.Marcado.DESMARCADA) {
            // Desmarcar la serie que empezó el descanso lo quita: ya no se ha hecho.
            if (s.descanso != null && s.descanso.serieId == serieId) s.descanso = null;
            publicar(s);
        }
        return m;
    }

    private static int indiceDe(SesionEnCurso.Ejercicio e, long serieId) {
        for (int i = 0; i < e.series.size(); i++) if (e.series.get(i).id == serieId) return i;
        return -1;
    }

    /** Una serie vacía más al final del ejercicio, hasta 20. */
    public boolean anadirSerie(long ejercicioId) {
        SesionEnCurso s = editando();
        SesionEnCurso.Ejercicio e = s != null ? ejercicio(s, ejercicioId) : null;
        if (e == null || e.series.size() >= SesionEnCurso.MAX_SERIES) return false;
        e.series.add(nuevaSerie(s));
        publicar(s);
        return true;
    }

    /** Quita una serie. Un ejercicio sin series se queda, vacío, hasta que se quite él. */
    public boolean quitarSerie(long serieId) {
        SesionEnCurso s = editando();
        SesionEnCurso.Ejercicio e = s != null ? ejercicioDeSerie(s, serieId) : null;
        if (e == null) return false;
        e.series.remove(indiceDe(e, serieId));
        if (s.cronometro != null && s.cronometro.serieId == serieId) s.cronometro = null;
        publicar(s);
        return true;
    }

    /** Una serie quitada, para ponerla donde estaba con «Deshacer». */
    public static final class Quitada {
        final long ejercicioId;
        final int indice;
        final SesionEnCurso.Serie serie;

        Quitada(long ejercicioId, int indice, SesionEnCurso.Serie serie) {
            this.ejercicioId = ejercicioId;
            this.indice = indice;
            this.serie = serie;
        }
    }

    /** Quita una serie y devuelve lo necesario para deshacerlo, o null si no se pudo. */
    @Nullable
    public Quitada quitarSerieDeshacible(long serieId) {
        SesionEnCurso s = editando();
        SesionEnCurso.Ejercicio e = s != null ? ejercicioDeSerie(s, serieId) : null;
        if (e == null) return null;
        int indice = indiceDe(e, serieId);
        Quitada q = new Quitada(e.id, indice, e.series.get(indice));
        return quitarSerie(serieId) ? q : null;
    }

    /** «Deshacer»: la serie vuelve a su sitio, si su ejercicio sigue y cabe. */
    public boolean reponer(@NonNull Quitada q) {
        SesionEnCurso s = editando();
        SesionEnCurso.Ejercicio e = s != null ? ejercicio(s, q.ejercicioId) : null;
        if (e == null || e.series.size() >= SesionEnCurso.MAX_SERIES) return false;
        e.series.add(Math.min(q.indice, e.series.size()), q.serie);
        publicar(s);
        return true;
    }

    /** Quita el ejercicio de esta sesión. La rutina no se toca. */
    public boolean quitarEjercicio(long ejercicioId) {
        SesionEnCurso s = editando();
        SesionEnCurso.Ejercicio e = s != null ? ejercicio(s, ejercicioId) : null;
        if (e == null) return false;
        if (s.cronometro != null && ejercicioDeSerie(s, s.cronometro.serieId) == e) s.cronometro = null;
        s.ejercicios.remove(e);
        publicar(s);
        return true;
    }

    /** Un ejercicio de la biblioteca, para añadir. */
    public static final class Nuevo {
        public final int ejercicioId;
        @Nullable public final String nombre;

        public Nuevo(int ejercicioId, @Nullable String nombre) {
            this.ejercicioId = ejercicioId;
            this.nombre = nombre;
        }
    }

    /** Añade ejercicios al final, cada uno con 3 series vacías. La rutina no se toca. */
    public void anadirEjercicios(@NonNull List<Nuevo> nuevos) {
        SesionEnCurso s = editando();
        if (s == null || nuevos.isEmpty()) return;
        for (Nuevo n : nuevos) {
            SesionEnCurso.Ejercicio e = new SesionEnCurso.Ejercicio();
            e.id = s.siguienteId++;
            e.ejercicioId = n.ejercicioId;
            e.nombre = n.nombre;
            // La última vez de ese ejercicio ya se sabe si está en la sesión.
            for (SesionEnCurso.Ejercicio otro : s.ejercicios) {
                if (otro.ejercicioId == n.ejercicioId && otro.anteriorCargado) {
                    e.anteriorCargado = true;
                    e.anteriorFecha = otro.anteriorFecha;
                    e.anterior = new ArrayList<>(otro.anterior);
                    break;
                }
            }
            for (int i = 0; i < SesionEnCurso.SERIES_AL_ANADIR; i++) e.series.add(nuevaSerie(s));
            s.ejercicios.add(e);
        }
        publicar(s);
        pedirUltimaVez();
    }

    // ── Cronómetro ───────────────────────────────────────────

    /** Arranca el cronómetro de una serie por tiempo (uno a la vez). */
    public boolean empezarCronometro(long serieId) {
        SesionEnCurso s = editando();
        if (s == null || serie(s, serieId) == null) return false;
        SesionEnCurso.Cronometro c = new SesionEnCurso.Cronometro();
        c.serieId = serieId;
        c.inicioMs = reloj.ahora();
        s.cronometro = c;
        // Si se empieza a hacer una serie, el descanso ya ha acabado.
        s.descanso = null;
        publicar(s);
        return true;
    }

    /**
     * Mira el cronómetro en marcha: el aviso que toque, y a la hora lo para sin apuntar.
     */
    @NonNull
    public CronometroSerie.Aviso comprobarCronometro() {
        SesionEnCurso s = actual();
        if (s == null || s.cronometro == null) return CronometroSerie.Aviso.NINGUNO;
        SesionEnCurso.Ejercicio e = ejercicioDeSerie(s, s.cronometro.serieId);
        if (e == null) {
            s.cronometro = null;
            publicar(s);
            return CronometroSerie.Aviso.NINGUNO;
        }
        CronometroSerie.Aviso a = CronometroSerie.comprobar(s.cronometro, reloj.ahora(),
                LogicaSesion.minimoPauta(e), LogicaSesion.maximoPauta(e));
        if (a == CronometroSerie.Aviso.PARAR) {
            s.cronometro = null;
            publicar(s);
        } else if (a != CronometroSerie.Aviso.NINGUNO) {
            almacen.escribir(s);
        }
        return a;
    }

    /** «Parar y apuntar»: los segundos van a la serie y la serie queda hecha. */
    public boolean pararYApuntar() {
        SesionEnCurso s = editando();
        if (s == null || s.cronometro == null) return false;
        SesionEnCurso.Serie serie = serie(s, s.cronometro.serieId);
        int segundos = Math.min(LogicaSesion.MAX_SEGUNDOS, CronometroSerie.segundos(s.cronometro, reloj.ahora()));
        s.cronometro = null;
        if (serie == null || segundos < 1) {
            publicar(s);
            return false;
        }
        serie.segundos = LogicaSesion.tiempo(segundos);
        serie.hecha = true;
        alHacerSerie(s, serie.id);
        publicar(s);
        return true;
    }

    /** «Cancelar»: lo para sin apuntar nada. */
    public void cancelarCronometro() {
        SesionEnCurso s = actual();
        if (s == null || s.cronometro == null) return;
        s.cronometro = null;
        publicar(s);
    }

    // ── Descanso (GP-013) ────────────────────────────────────

    // Una serie hecha empieza el descanso con el de su ejercicio (o lo quita si era la
    // última que quedaba), si así está en Ajustes.
    private void alHacerSerie(SesionEnCurso s, long serieId) {
        finDescanso = null;
        if (ajustesDescanso.empezarAlMarcar()) {
            LogicaDescanso.empezar(s, serieId, reloj.ahora(), ajustesDescanso.sinPautaSegundos());
        } else if (LogicaDescanso.siguiente(s) == null) {
            s.descanso = null;
        }
    }

    /** «−15 s» o «+15 s». */
    public boolean ajustarDescanso(int segundos) {
        SesionEnCurso s = editando();
        if (s == null || !LogicaDescanso.ajustar(s, segundos, reloj.ahora())) return false;
        publicar(s);
        return true;
    }

    /** «Saltar»: se quita sin avisar. */
    public void saltarDescanso() {
        SesionEnCurso s = actual();
        if (s == null || s.descanso == null) return;
        s.descanso = null;
        publicar(s);
    }

    /**
     * Mira el descanso (lo hace el vigilante de la app, una vez por segundo, con alguna
     * pantalla delante). Si acaba de terminar devuelve TERMINA una sola vez, para que
     * quien mira avise, y deja «¡A por la serie N!» unos segundos. Si terminó hace rato,
     * se quita sin más.
     */
    @NonNull
    public LogicaDescanso.Estado comprobarDescanso() {
        SesionEnCurso s = actual();
        if (s == null) return LogicaDescanso.Estado.NINGUNO;
        long ahora = reloj.ahora();
        LogicaDescanso.Estado e = LogicaDescanso.estado(s.descanso, ahora);
        if (e == LogicaDescanso.Estado.TERMINA || e == LogicaDescanso.Estado.TERMINO_ANTES) {
            s.descanso = null;
            finDescanso = e == LogicaDescanso.Estado.TERMINA ? fin(s, ahora) : null;
            publicar(s);
        }
        return e;
    }

    /**
     * La alarma del fin del descanso ha sonado con la app fuera: se quita el descanso (no
     * volverá a sonar al abrirla) y se devuelve qué toca, para la notificación.
     *
     * @return qué toca, o null si no había un descanso terminado (se saltó, se empezó
     *         otro o ya se avisó).
     */
    @Nullable
    public FinDescanso terminarPorAlarma() {
        SesionEnCurso s = actual();
        long ahora = reloj.ahora();
        // Un segundo de gracia: la alarma puede despertar justo antes de la hora.
        if (s == null || s.descanso == null || s.descanso.finMs > ahora + 1000) return null;
        if (LogicaDescanso.caducado(s.descanso, ahora)) {
            // La alarma llega tarde de más (GP-144): se quita sin avisar.
            s.descanso = null;
            publicar(s);
            return null;
        }
        s.descanso = null;
        FinDescanso f = fin(s, ahora);
        publicar(s);
        return f;
    }

    private static FinDescanso fin(SesionEnCurso s, long ahora) {
        LogicaDescanso.Siguiente sig = LogicaDescanso.siguiente(s);
        return sig == null ? new FinDescanso(null, 0, 0, ahora + FIN_VISIBLE_MS)
                : new FinDescanso(sig.ejercicio.nombre, sig.ejercicio.ejercicioId, sig.numero, ahora + FIN_VISIBLE_MS);
    }

    /** El aviso del fin del descanso mientras dura en pantalla, o null. */
    @Nullable
    public FinDescanso finDescanso() {
        if (finDescanso != null && reloj.ahora() >= finDescanso.hastaMs) finDescanso = null;
        return finDescanso;
    }

    /** Su botón de cerrar. */
    public void cerrarFinDescanso() {
        finDescanso = null;
        SesionEnCurso s = actual();
        if (s != null) poner(s);
    }

    // ── La hoja de terminar ──────────────────────────────────

    /** Minutos ajustados a mano (1..600), o null para volver a los del reloj. */
    public void ajustarDuracion(@Nullable Integer minutos) {
        SesionEnCurso s = editando();
        if (s == null) return;
        s.duracionAjustada = minutos == null ? null
                : Math.max(1, Math.min(LogicaSesion.MAX_MINUTOS, minutos));
        almacen.escribir(s);
    }

    public void valorar(@Nullable Integer estrellas) {
        SesionEnCurso s = editando();
        if (s == null) return;
        s.valoracion = estrellas != null && estrellas >= 1 && estrellas <= 5 ? estrellas : null;
        almacen.escribir(s);
    }

    public void escribirNotas(@Nullable String notas) {
        SesionEnCurso s = editando();
        if (s == null) return;
        s.notas = notas;
        almacen.escribir(s);
    }

    /** Minutos que propone la hoja: los ajustados, o los del reloj. */
    public int minutosPropuestos() {
        SesionEnCurso s = actual();
        if (s == null) return 1;
        if (s.duracionEnviada != null) return s.duracionEnviada;
        return s.duracionAjustada != null ? s.duracionAjustada : LogicaSesion.minutosReloj(s.inicioMs, reloj.ahora());
    }

    // ── Guardar ──────────────────────────────────────────────

    /**
     * Primer intento de guardar. Nace la clave, se fija la duración y la sesión deja de
     * poder editarse. Sin ninguna serie marcada no hay nada que guardar.
     */
    @NonNull
    public Guardar guardar() {
        SesionEnCurso s = actual();
        if (s == null) return Guardar.NO_HAY;
        if (s.claveIdempotencia != null) return reintentar();
        if (LogicaSesion.totales(s).seriesHechas == 0) return Guardar.NADA_MARCADO;
        s.duracionEnviada = minutosPropuestos();
        s.claveIdempotencia = UUID.randomUUID().toString();
        s.cronometro = null;
        s.descanso = null;
        enviar(s);
        return Guardar.ENVIADO;
    }

    /** Reintenta con la misma clave y lo mismo que el primer intento. */
    @NonNull
    public Guardar reintentar() {
        SesionEnCurso s = actual();
        if (s == null) return Guardar.NO_HAY;
        if (s.claveIdempotencia == null) return guardar();
        if (s.guardado == SesionEnCurso.Guardado.GUARDANDO) return Guardar.YA_EN_CURSO;
        enviar(s);
        return Guardar.ENVIADO;
    }

    /**
     * Al abrir la app: una sesión que se intentó guardar y no se pudo se reintenta sola,
     * una vez por proceso. Si no llega, se queda como estaba, con «Reintentar».
     *
     * @return si se lanzó el reintento.
     */
    public boolean reintentarAlAbrir() {
        if (reintentoAlAbrirHecho) return false;
        SesionEnCurso s = actual();
        if (s == null || s.claveIdempotencia == null || s.guardado != SesionEnCurso.Guardado.FALLO) return false;
        reintentoAlAbrirHecho = true;
        enviar(s);
        return true;
    }

    private void enviar(SesionEnCurso s) {
        s.guardado = SesionEnCurso.Guardado.GUARDANDO;
        publicar(s);
        final SesionEnCurso enviada = s;
        red.guardarCompleta(LogicaSesion.cuerpo(s, s.claveIdempotencia, s.duracionEnviada, zona),
                new Red.Respuesta<SesionEntrenamiento>() {
                    @Override public void ok(SesionEntrenamiento creada) {
                        if (creada == null || creada.getId() <= 0) {
                            fallo(0, null);
                            return;
                        }
                        // El resultado va antes de quitar la sesión: la pantalla abre el
                        // resumen y no confunde el hueco con un «descartada».
                        resultado.setValue(new Evento<>(new Resultado(creada, enviada)));
                        // Si mientras tanto se descartó y empezó otra, esa no se toca.
                        if (actual() == enviada) {
                            almacen.borrar(enviada.usuarioId);
                            poner(null);
                        }
                    }

                    @Override public void fallo(int codigo, String mensaje) {
                        enviada.guardado = SesionEnCurso.Guardado.FALLO;
                        enviada.codigoFallo = codigo;
                        enviada.mensajeFallo = mensaje;
                        if (actual() == enviada) publicar(enviada);
                        resultado.setValue(new Evento<>(new Resultado(null, enviada)));
                    }
                });
    }

    // Escribe al fichero y avisa a quien observa.
    private void publicar(SesionEnCurso s) {
        almacen.escribir(s);
        poner(s);
    }

    // Avisa a quien observa y deja la alarma del descanso como pide la sesión: a su
    // hora, o quitada si ya no hay descanso (o sesión).
    private void poner(@Nullable SesionEnCurso s) {
        sesion.setValue(s);
        programador.sincronizar(s != null ? s.descanso : null, reloj.ahora());
    }
}
