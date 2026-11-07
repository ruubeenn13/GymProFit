package es.pmdm.gymprofit.ui.viewmodels;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import java.util.UUID;

import es.pmdm.gymprofit.envivo.CabeceraEntreno;
import es.pmdm.gymprofit.envivo.LogicaRecordVivo;
import es.pmdm.gymprofit.envivo.LogicaSesion;
import es.pmdm.gymprofit.envivo.SesionEnCursoRepositorio;
import es.pmdm.gymprofit.model.envivo.SesionEnCurso;
import es.pmdm.gymprofit.model.envivo.UltimaVez;
import es.pmdm.gymprofit.model.programa.ProgramaQueSigue;
import es.pmdm.gymprofit.model.rutina.Rutina;
import es.pmdm.gymprofit.model.rutina.RutinaEjercicio;
import es.pmdm.gymprofit.model.sesion.SesionEntrenamiento;
import es.pmdm.gymprofit.model.sesion.VezRutina;
import es.pmdm.gymprofit.network.RegistroSesionRepositorio;
import es.pmdm.gymprofit.utils.Caras;
import es.pmdm.gymprofit.utils.HoyToca;
import es.pmdm.gymprofit.utils.OrdenRutinas;

// ============================================================
// RegistrarSesionViewModel — el borrador de «Apuntar entreno», fuente de verdad (GP-016,
// rehecho en GP-241).
//
// El borrador es una SesionEnCurso, el mismo modelo de la sesión en vivo: sus ejercicios con su
// pauta, sus series tal como se escribieron («72,» a medio teclear también), la última vez de cada
// uno (GP-014) y su mejor marca (GP-210). Así marcar, las pistas, el récord y lo que se manda a
// POST /sesiones/completa siguen las reglas de LogicaSesion y LogicaRecordVivo, no una copia.
// Va entero al SavedStateHandle con la fecha, la duración, la clave del intento y si había un
// guardado en vuelo: girar o que Android mate la app en segundo plano no pierden nada.
//
// Lo que no es del usuario no se guarda y se vuelve a pedir: sus rutinas activas (con sus últimas
// veces, GP-232), el programa que sigue y sus sesiones, que deciden la que toca hoy (HoyToca) y la
// duración de un entreno libre.
//
// Solo se guarda lo marcado (DEC-060 y la cabecera que suma al marcar). Sin nada marcado se guardan
// el día y la duración. La idempotencia de GP-006 no cambia: la clave nace al pulsar Guardar, el
// reintento la reusa y solo se limpia con el éxito.
// ============================================================
public class RegistrarSesionViewModel extends ViewModel {

    // Claves del borrador en el SavedStateHandle.
    static final String K_INICIADO = "borrador_iniciado";
    static final String K_SESION = "borrador_sesion";
    static final String K_ELEGIDA = "borrador_elegida";
    static final String K_EJERCICIOS_DE = "borrador_ejercicios_de";
    static final String K_DURACION = "borrador_duracion";
    static final String K_DURACION_TOCADA = "borrador_duracion_tocada";
    static final String K_CLAVE = "borrador_clave";
    static final String K_EN_VUELO = "borrador_en_vuelo";
    static final String K_FECHA = "borrador_fecha";
    static final String K_PLIEGUE = "borrador_pliegue";

    /** Los ejercicios de un entreno libre: no son de ninguna rutina. */
    static final int LIBRE = -1;

    /** La duración: de 5 en 5, de 5 a 300 minutos, y 45 si no hay de dónde sacarla. */
    public static final int DURACION_PASO = 5, DURACION_MIN = 5, DURACION_MAX = 300, DURACION_DEFECTO = 45;

    // La hora, para que los tests la fijen.
    java.util.function.LongSupplier ahora = System::currentTimeMillis;
    TimeZone zona = TimeZone.getDefault();

    /** En qué punto está el guardado. */
    public enum Fase {
        /** Nada en marcha: se puede editar y guardar. */
        INACTIVO,
        /** Hay una petición en el aire; otra pulsación de Guardar no manda nada. */
        GUARDANDO,
        /** No se guardó, o no se sabe: se ofrece reintentar con la misma clave. */
        FALLO,
        /** Guardada: la pantalla abre el resumen y se cierra. */
        EXITO,
        /** El resumen ya se abrió; el éxito no se vuelve a entregar. */
        TERMINADO
    }

    /** Qué pasó al pulsar Guardar. */
    public enum ResultadoGuardar { ENVIADO, YA_EN_CURSO }

    /** Cómo va algo que se pide a la red. */
    public enum Carga { CARGANDO, LISTA, ERROR }

    /** Por qué la rutina de la cabecera es la que es. */
    public enum Porque {
        /** La que toca en el programa que sigue. */
        PROGRAMA,
        /** La de «Mis rutinas» que más tiempo lleva sin hacerse. */
        MAS_TIEMPO,
        /** La eligió el usuario, o es un entreno libre: no se explica. */
        NINGUNO
    }

    /** El estado del guardado, inmutable: cada cambio es un objeto nuevo en el LiveData. */
    public static final class EstadoGuardado {
        public final Fase fase;
        /** Del fallo: código HTTP, -1 si fue la red, 0 si la respuesta vino sin sesión. */
        public final int codigo;
        public final String mensaje;
        /**
         * El fallo es que el proceso murió con la petición en el aire: no se sabe si
         * llegó. Reintentar con la misma clave lo resuelve sin duplicar.
         */
        public final boolean interrumpido;
        /** La sesión creada, solo en EXITO. */
        public final SesionEntrenamiento sesion;

        private EstadoGuardado(Fase fase, int codigo, String mensaje, boolean interrumpido,
                               SesionEntrenamiento sesion) {
            this.fase = fase;
            this.codigo = codigo;
            this.mensaje = mensaje;
            this.interrumpido = interrumpido;
            this.sesion = sesion;
        }

        static EstadoGuardado de(Fase fase) { return new EstadoGuardado(fase, 0, null, false, null); }
    }

    private final SavedStateHandle handle;
    private final RegistroSesionRepositorio repo;

    private final MutableLiveData<EstadoGuardado> estado = new MutableLiveData<>();
    private final MutableLiveData<Carga> datos = new MutableLiveData<>(Carga.CARGANDO);
    private final MutableLiveData<Carga> ejercicios = new MutableLiveData<>(Carga.LISTA);
    /** Avisa de que el borrador cambió (lo marcado, la rutina, la fecha…): la pantalla repinta. */
    private final MutableLiveData<Integer> cambios = new MutableLiveData<>(0);

    // El borrador. Es la misma instancia que está en el handle: tocarla y volver a ponerla es guardar.
    private SesionEnCurso borrador;

    // Lo de la red, de esta instancia: no se guarda, se vuelve a pedir tras matar el proceso.
    @Nullable private List<Rutina> rutinas;
    @Nullable private ProgramaQueSigue seguido;
    @Nullable private List<SesionEntrenamiento> sesiones;
    private boolean seguidoListo;
    @Nullable private HoyToca.Eleccion hoy;
    private boolean datosPedidos;
    private int pendientesDatos;
    private boolean falloDatos;
    // La rutina cuyos ejercicios se están pidiendo, para descartar respuestas viejas.
    @Nullable private Integer rutinaPedida;
    private boolean pidiendoAnterior, pidiendoMarcas;

    /**
     * @param handle el estado guardado; vacío la primera vez, con el borrador al restaurar.
     * @param repo   las llamadas de la pantalla.
     */
    public RegistrarSesionViewModel(@NonNull SavedStateHandle handle, @NonNull RegistroSesionRepositorio repo) {
        this.handle = handle;
        this.repo = repo;
        SesionEnCurso guardado = handle.get(K_SESION);
        borrador = guardado != null ? guardado : new SesionEnCurso();
        handle.set(K_SESION, borrador);

        // Si el proceso murió con el guardado en vuelo, nadie va a recibir la respuesta. No se da
        // por guardado (no hay sesión que enseñar) ni por perdido (puede que llegara): se ofrece
        // reintentar con la misma clave, que no duplica.
        if (Boolean.TRUE.equals(handle.get(K_EN_VUELO))) {
            estado.setValue(new EstadoGuardado(Fase.FALLO, -1, null, true, null));
        } else {
            estado.setValue(EstadoGuardado.de(Fase.INACTIVO));
        }
    }

    /** Solo para los tests: lo que Android guardaría al matar el proceso. */
    SavedStateHandle handle() { return handle; }

    // ── Arrancar ──────────────────────────────────────────────

    /**
     * Arranca la pantalla. Se llama en cada onCreate y solo hace lo que falte.
     *
     * @param rutinaDeEntrada la rutina con la que se abrió la pantalla (desde el detalle de una
     *                        rutina), o null para que se elija la que toca hoy. Solo vale la
     *                        primera vez: al restaurar manda lo que eligió el usuario.
     */
    public void iniciar(@Nullable Integer rutinaDeEntrada) {
        if (!Boolean.TRUE.equals(handle.get(K_INICIADO))) {
            handle.set(K_INICIADO, true);
            if (rutinaDeEntrada != null) {
                handle.set(K_ELEGIDA, true);
                borrador.rutinaId = rutinaDeEntrada;
                guardar(false);
            }
        }
        if (!datosPedidos) pedirDatos();
        Integer rutina = borrador.rutinaId;
        if (Boolean.TRUE.equals(handle.get(K_ELEGIDA)) && rutina != null
                && !rutina.equals(handle.get(K_EJERCICIOS_DE)) && !rutina.equals(rutinaPedida)) {
            pedirEjercicios(rutina);
        }
        // Los ejercicios ya estaban, pero la última vez o la marca se quedaron a medio pedir.
        pedirAnterior();
    }

    /** Vuelve a pedir lo que falló: los datos de la cabecera o los ejercicios de la rutina. */
    public void reintentar() {
        if (Carga.ERROR == datos.getValue()) pedirDatos();
        Integer rutina = borrador.rutinaId;
        if (Carga.ERROR == ejercicios.getValue() && rutina != null) pedirEjercicios(rutina);
    }

    // Las rutinas activas, el programa que sigue y las sesiones, a la vez.
    private void pedirDatos() {
        datosPedidos = true;
        falloDatos = false;
        pendientesDatos = 3;
        rutinas = null;
        sesiones = null;
        seguidoListo = false;
        datos.setValue(Carga.CARGANDO);
        repo.rutinasDelUsuario(new RegistroSesionRepositorio.Respuesta<List<Rutina>>() {
            @Override public void ok(List<Rutina> lista) {
                rutinas = lista != null ? lista : new ArrayList<>();
                datoListo();
            }
            @Override public void fallo(int codigo, String mensaje) { falloDeDatos(); }
        });
        repo.sesiones(new RegistroSesionRepositorio.Respuesta<List<SesionEntrenamiento>>() {
            @Override public void ok(List<SesionEntrenamiento> lista) {
                sesiones = lista != null ? lista : new ArrayList<>();
                datoListo();
            }
            @Override public void fallo(int codigo, String mensaje) { falloDeDatos(); }
        });
        repo.programaSeguido(new RegistroSesionRepositorio.Respuesta<ProgramaQueSigue>() {
            @Override public void ok(ProgramaQueSigue s) {
                seguido = s;
                seguidoListo = true;
                datoListo();
            }
            @Override public void fallo(int codigo, String mensaje) {
                // Sin saber el programa, la que toca se decide con «Mis rutinas», como en Inicio:
                // se apunta igual y la rutina se puede cambiar a mano. El 404 es no seguir ninguno.
                seguido = null;
                seguidoListo = true;
                datoListo();
            }
        });
    }

    private void falloDeDatos() {
        if (falloDatos) return;
        falloDatos = true;
        datos.setValue(Carga.ERROR);
        publicar();
    }

    // Con las tres respuestas: la que toca hoy y, si nadie ha elegido aún, se pone esa.
    private void datoListo() {
        if (falloDatos || --pendientesDatos > 0) return;
        hoy = HoyToca.elegir(seguido, rutinas, sesiones);
        if (!Boolean.TRUE.equals(handle.get(K_ELEGIDA))) {
            handle.set(K_ELEGIDA, true);
            ponerRutina(hoy != null ? hoy.rutina.getId() : null);
        }
        if (handle.get(K_DURACION) == null) handle.set(K_DURACION, duracionDeEntrada());
        datos.setValue(Carga.LISTA);
        publicar();
    }

    // ── La rutina ─────────────────────────────────────────────

    /**
     * Elige la rutina (null = entreno libre). Lo marcado de la anterior se pierde: quien llama
     * pregunta antes si había algo ({@link #marcadas()}). La misma que ya está no hace nada.
     */
    public void elegirRutina(@Nullable Integer rutinaId) {
        if (Objects.equals(rutinaId, borrador.rutinaId) && Boolean.TRUE.equals(handle.get(K_ELEGIDA))) return;
        handle.set(K_ELEGIDA, true);
        ponerRutina(rutinaId);
        // La duración de entrada es la de la rutina: cambia con ella mientras no se haya tocado.
        if (!Boolean.TRUE.equals(handle.get(K_DURACION_TOCADA)) && datos.getValue() == Carga.LISTA) {
            handle.set(K_DURACION, duracionDeEntrada());
        }
        publicar();
    }

    private void ponerRutina(@Nullable Integer rutinaId) {
        borrador.rutinaId = rutinaId;
        Rutina r = rutina();
        borrador.rutinaNombre = r != null ? r.getNombre() : null;
        borrador.ejercicios = new ArrayList<>();
        handle.set(K_PLIEGUE, new HashMap<Long, Boolean>());
        if (rutinaId == null) {
            rutinaPedida = null;
            handle.set(K_EJERCICIOS_DE, LIBRE);
            ejercicios.setValue(Carga.LISTA);
            guardar(false);
        } else {
            handle.set(K_EJERCICIOS_DE, null);
            guardar(false);
            pedirEjercicios(rutinaId);
        }
    }

    // Pide los ejercicios de una rutina y los pone en el borrador con sus series vacías.
    private void pedirEjercicios(int rutinaId) {
        rutinaPedida = rutinaId;
        ejercicios.setValue(Carga.CARGANDO);
        repo.ejerciciosDeRutina(rutinaId, new RegistroSesionRepositorio.Respuesta<List<RutinaEjercicio>>() {
            @Override
            public void ok(List<RutinaEjercicio> lista) {
                // Una respuesta de una rutina que ya no está elegida llega tarde: se tira.
                if (!Integer.valueOf(rutinaId).equals(rutinaPedida)) return;
                rutinaPedida = null;
                List<SesionEnCurso.Ejercicio> nuevos = new ArrayList<>();
                if (lista != null) {
                    for (RutinaEjercicio re : lista) {
                        if (re.getEjercicioId() == -1) continue;
                        nuevos.add(SesionEnCursoRepositorio.deRutina(borrador, re));
                    }
                }
                borrador.ejercicios = nuevos;
                handle.set(K_EJERCICIOS_DE, rutinaId);
                ejercicios.setValue(Carga.LISTA);
                guardar(true);
                pedirAnterior();
            }

            @Override
            public void fallo(int codigo, String mensaje) {
                if (!Integer.valueOf(rutinaId).equals(rutinaPedida)) return;
                rutinaPedida = null;
                // Se dice en el sitio de los ejercicios, con «Reintentar». No se marca como cargada,
                // así que reintentar (o volver a elegirla) los pide otra vez.
                ejercicios.setValue(Carga.ERROR);
                publicar();
            }
        });
    }

    /** Añade ejercicios del catálogo a un entreno libre, cada uno con 3 series vacías. */
    public void anadirEjercicios(@NonNull List<SesionEnCursoRepositorio.Nuevo> nuevos) {
        if (nuevos.isEmpty()) return;
        for (SesionEnCursoRepositorio.Nuevo n : nuevos) {
            SesionEnCurso.Ejercicio e = new SesionEnCurso.Ejercicio();
            e.id = borrador.siguienteId++;
            e.ejercicioId = n.ejercicioId;
            e.nombre = n.nombre;
            for (int i = 0; i < SesionEnCurso.SERIES_AL_ANADIR; i++) {
                e.series.add(SesionEnCursoRepositorio.nuevaSerie(borrador));
            }
            borrador.ejercicios.add(e);
        }
        guardar(true);
        pedirAnterior();
    }

    // La última vez de los ejercicios que no la tienen y, después, su mejor marca.
    private void pedirAnterior() {
        if (pidiendoAnterior) return;
        List<Integer> faltan = LogicaSesion.sinAnterior(borrador);
        if (faltan.isEmpty()) {
            pedirMarcas();
            return;
        }
        pidiendoAnterior = true;
        repo.ultimaVez(faltan, new RegistroSesionRepositorio.Respuesta<List<UltimaVez>>() {
            @Override public void ok(List<UltimaVez> lista) {
                pidiendoAnterior = false;
                LogicaSesion.aplicarUltimaVez(borrador, faltan, lista);
                guardar(true);
                pedirMarcas();
            }
            @Override public void fallo(int codigo, String mensaje) {
                // Sin la última vez se apunta igual: las pistas salen vacías y «Todo como la última
                // vez» no sale. Se vuelve a pedir al volver a la pantalla (iniciar).
                pidiendoAnterior = false;
            }
        });
    }

    private void pedirMarcas() {
        if (pidiendoMarcas) return;
        List<Integer> faltan = LogicaRecordVivo.sinMarca(borrador);
        if (faltan.isEmpty()) return;
        pidiendoMarcas = true;
        repo.mejoresMarcas(faltan, new RegistroSesionRepositorio.Respuesta<Map<Integer, SesionEnCurso.Marca>>() {
            @Override public void ok(Map<Integer, SesionEnCurso.Marca> marcas) {
                pidiendoMarcas = false;
                LogicaRecordVivo.aplicarMarcas(borrador, faltan, marcas);
                guardar(true);
            }
            @Override public void fallo(int codigo, String mensaje) {
                // Sin la marca no se sabe si una serie es récord: no se celebra ninguno, que es lo
                // seguro (el resumen los dirá igual, porque los calcula la API al guardar).
                pidiendoMarcas = false;
            }
        });
    }

    // ── Lo que hace el usuario ────────────────────────────────

    public void escribirPeso(long serieId, @NonNull String texto) {
        SesionEnCurso.Serie s = serie(serieId);
        if (s == null || texto.equals(s.peso)) return;
        s.peso = texto;
        guardar(s.hecha);
    }

    public void escribirRepeticiones(long serieId, @NonNull String texto) {
        SesionEnCurso.Serie s = serie(serieId);
        if (s == null || texto.equals(s.repeticiones)) return;
        s.repeticiones = texto;
        guardar(s.hecha);
    }

    public void escribirSegundos(long serieId, @NonNull String texto) {
        SesionEnCurso.Serie s = serie(serieId);
        if (s == null || texto.equals(s.segundos)) return;
        s.segundos = texto;
        guardar(s.hecha);
    }

    /**
     * Marca o desmarca una serie, como en la sesión en vivo (LogicaSesion.marcar): un campo vacío
     * se queda con lo de la última vez; sin ella, unas repeticiones vacías no se marcan.
     */
    @NonNull
    public LogicaSesion.Marcado marcar(long serieId, @NonNull Locale idioma) {
        SesionEnCurso.Ejercicio e = SesionEnCursoRepositorio.ejercicioDeSerie(borrador, serieId);
        if (e == null) return LogicaSesion.Marcado.NO_VALIDO;
        int indice = -1;
        for (int i = 0; i < e.series.size(); i++) if (e.series.get(i).id == serieId) indice = i;
        LogicaSesion.Marcado m = LogicaSesion.marcar(e, indice, idioma);
        guardar(true);
        return m;
    }

    /**
     * Si queda alguna serie sin marcar que tuvo su igual la última vez: solo entonces sale «Todo
     * como la última vez» (sin última vez no sale; con todo ya rellenado, tampoco).
     */
    public boolean hayUltimaVez() {
        for (SesionEnCurso.Ejercicio e : borrador.ejercicios) {
            for (int i = 0; i < e.series.size(); i++) {
                SesionEnCurso.SerieAnterior a = LogicaSesion.anterior(e, i + 1);
                if (!e.series.get(i).hecha && a != null
                        && (a.repeticiones > 0 || (a.segundos != null && a.segundos > 0))) return true;
            }
        }
        return false;
    }

    /**
     * Si algún ejercicio tuvo última vez: entonces el sitio de «Todo como la última vez» se guarda
     * aunque ya no quede nada que rellenar, para que la lista no salte al irse el botón.
     */
    public boolean tuvoUltimaVez() {
        for (SesionEnCurso.Ejercicio e : borrador.ejercicios) if (!e.anterior.isEmpty()) return true;
        return false;
    }

    /**
     * «Todo como la última vez»: cada serie sin marcar que tuvo su igual la última vez se queda con
     * aquellos valores y se marca. Las ya marcadas no se tocan.
     *
     * @return cuántas series se marcaron.
     */
    public int comoLaUltimaVez(@NonNull Locale idioma) {
        int n = 0;
        for (SesionEnCurso.Ejercicio e : borrador.ejercicios) {
            boolean tiempo = LogicaSesion.porTiempo(e);
            for (int i = 0; i < e.series.size(); i++) {
                SesionEnCurso.Serie s = e.series.get(i);
                SesionEnCurso.SerieAnterior a = LogicaSesion.anterior(e, i + 1);
                if (s.hecha || a == null) continue;
                if (tiempo) {
                    if (a.segundos == null || a.segundos <= 0) continue;
                    s.segundos = LogicaSesion.tiempo(a.segundos);
                } else {
                    if (a.repeticiones <= 0) continue;
                    s.repeticiones = String.valueOf(a.repeticiones);
                    s.peso = pesoDe(a.peso, idioma);
                }
                s.hecha = true;
                n++;
            }
            if (n > 0) pliegue().remove(e.id);
        }
        guardar(true);
        return n;
    }

    private static String pesoDe(@Nullable String exacto, @NonNull Locale idioma) {
        if (exacto == null) return "";
        try {
            java.math.BigDecimal b = new java.math.BigDecimal(exacto);
            return b.signum() > 0 ? LogicaSesion.kilos(b, idioma) : "";
        } catch (NumberFormatException e) {
            return "";
        }
    }

    /** Cinco minutos más o menos, sin salirse de 5 a 300. */
    public void sumarDuracion(int minutos) {
        int nueva = Math.max(DURACION_MIN, Math.min(DURACION_MAX, getDuracion() + minutos));
        handle.set(K_DURACION, nueva);
        handle.set(K_DURACION_TOCADA, true);
        publicar();
    }

    /** Toca una cara: la marca, o la quita si ya estaba (Caras.alTocar). */
    public void tocarCara(int valor) {
        borrador.valoracion = Caras.alTocar(borrador.valoracion, valor);
        guardar(true);
    }

    public void setNotas(@NonNull String notas) {
        if (notas.equals(borrador.notas == null ? "" : borrador.notas)) return;
        borrador.notas = notas;
        guardar(false);
    }

    /** Abre o pliega un ejercicio. */
    public void alternarPliegue(long ejercicioId) {
        SesionEnCurso.Ejercicio e = ejercicio(ejercicioId);
        if (e == null) return;
        pliegue().put(ejercicioId, !abierto(e));
        guardar(true);
    }

    /**
     * Si un ejercicio va abierto: los empezados siempre; el primero y los que se abrieron, también.
     * Los que no se han empezado van plegados, con «Sin empezar».
     */
    public boolean abierto(@NonNull SesionEnCurso.Ejercicio e) {
        if (empezado(e)) return true;
        Boolean elegido = pliegue().get(e.id);
        if (elegido != null) return elegido;
        return !borrador.ejercicios.isEmpty() && borrador.ejercicios.get(0).id == e.id;
    }

    /** Si tiene alguna serie marcada. */
    public static boolean empezado(@NonNull SesionEnCurso.Ejercicio e) {
        for (SesionEnCurso.Serie s : e.series) if (s.hecha) return true;
        return false;
    }

    // ── La fecha (GP-012: apuntar un entrenamiento hecho) ─────

    /**
     * El día del entrenamiento que se apunta, «yyyy-MM-dd». Nunca futuro: un día posterior a hoy
     * no se acepta.
     *
     * @return si se aceptó.
     */
    public boolean setFecha(@NonNull String dia) {
        if (dia.compareTo(hoy()) > 0) return false;
        handle.set(K_FECHA, dia);
        publicar();
        return true;
    }

    /** El día del entrenamiento, «yyyy-MM-dd»; hoy por defecto. */
    @NonNull
    public String getFecha() {
        String f = handle.get(K_FECHA);
        return f != null && f.compareTo(hoy()) <= 0 ? f : hoy();
    }

    /** Hoy, «yyyy-MM-dd», en la zona del móvil. */
    @NonNull
    public String hoy() {
        return dia(0);
    }

    /** Ayer, «yyyy-MM-dd». */
    @NonNull
    public String ayer() {
        return dia(-1);
    }

    private String dia(int desplazamiento) {
        Calendar c = Calendar.getInstance(zona, Locale.US);
        c.setTimeInMillis(ahora.getAsLong());
        c.add(Calendar.DAY_OF_MONTH, desplazamiento);
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        f.setTimeZone(zona);
        return f.format(c.getTime());
    }

    /**
     * El inicio de un entrenamiento que se apunta hecho: ese día, a la hora de ahora menos la
     * duración. Si eso cae antes de medianoche, cae el día anterior: es lo que pasó.
     */
    @NonNull
    static String fechaInicio(@NonNull String dia, long ahoraMs, int minutos, @NonNull TimeZone zona) {
        return LogicaSesion.fechaApi(inicioMs(dia, ahoraMs, minutos, zona), zona);
    }

    static long inicioMs(@NonNull String dia, long ahoraMs, int minutos, @NonNull TimeZone zona) {
        Calendar c = Calendar.getInstance(zona, Locale.US);
        c.setTimeInMillis(ahoraMs);
        c.set(Calendar.YEAR, Integer.parseInt(dia.substring(0, 4)));
        c.set(Calendar.MONTH, Integer.parseInt(dia.substring(5, 7)) - 1);
        c.set(Calendar.DAY_OF_MONTH, Integer.parseInt(dia.substring(8, 10)));
        c.set(Calendar.MILLISECOND, 0);
        c.add(Calendar.MINUTE, -minutos);
        return c.getTimeInMillis();
    }

    // ── Lecturas ──────────────────────────────────────────────

    public LiveData<EstadoGuardado> getEstadoGuardado() { return estado; }

    /** Las rutinas, el programa y las sesiones: carga, error o listos. */
    public LiveData<Carga> getDatos() { return datos; }

    /** Los ejercicios de la rutina elegida. */
    public LiveData<Carga> getCargaEjercicios() { return ejercicios; }

    /** Cambia con cada cambio del borrador: es la señal de repintar. */
    public LiveData<Integer> getCambios() { return cambios; }

    /** El borrador, para pintarlo. No se toca desde fuera: cada cambio pasa por aquí. */
    @NonNull
    public SesionEnCurso getBorrador() { return borrador; }

    @Nullable
    public Integer getRutinaId() { return borrador.rutinaId; }

    /** Si ya se sabe qué se apunta (la rutina o libre): hasta entonces la cabecera espera. */
    public boolean rutinaDecidida() { return Boolean.TRUE.equals(handle.get(K_ELEGIDA)); }

    /** La rutina elegida, de la lista de activas; null en un entreno libre o si aún no está. */
    @Nullable
    public Rutina rutina() {
        Integer id = borrador.rutinaId;
        if (id == null || rutinas == null) return null;
        for (Rutina r : rutinas) if (r.getId() == id) return r;
        return null;
    }

    /** El nombre de la rutina elegida: el de la lista, o el que se guardó al elegirla. */
    @Nullable
    public String nombreRutina() {
        Rutina r = rutina();
        return r != null ? r.getNombre() : borrador.rutinaNombre;
    }

    /** La última vez de la rutina elegida, o null (primera vez o libre). */
    @Nullable
    public VezRutina ultimaVez() {
        Rutina r = rutina();
        if (r == null || r.getUltimas().isEmpty()) return null;
        return r.getUltimas().get(r.getUltimas().size() - 1);
    }

    /** Por qué la rutina es la que es: solo se dice si es la que toca hoy. */
    @NonNull
    public Porque porque() {
        if (hoy == null || borrador.rutinaId == null || hoy.rutina.getId() != borrador.rutinaId) return Porque.NINGUNO;
        return hoy.delPrograma ? Porque.PROGRAMA : Porque.MAS_TIEMPO;
    }

    /** Si el usuario tiene alguna rutina activa (sin ninguna, la hoja no tiene nada que elegir). */
    public boolean hayRutinas() {
        return rutinas != null && !rutinas.isEmpty();
    }

    /** Las rutinas en el orden de las hojas (OrdenRutinas), con la que toca hoy primero. */
    @NonNull
    public OrdenRutinas.Orden orden() {
        return OrdenRutinas.ordenar(rutinas, seguido, hoy != null ? hoy.rutina.getId() : null);
    }

    /** Lo que suma la cabecera con lo marcado ahora. */
    @NonNull
    public CabeceraEntreno cabecera() {
        return CabeceraEntreno.de(borrador.ejercicios, borrador.rutinaId != null, ultimaVez());
    }

    /** Los minutos: los de entrada hasta que se tocan. */
    public int getDuracion() {
        Integer d = handle.get(K_DURACION);
        return d != null ? d : duracionDeEntrada();
    }

    @NonNull
    public String getNotas() { return borrador.notas != null ? borrador.notas : ""; }

    /** La valoración, de 1 a 5, o null sin valorar (GP-077). */
    @Nullable
    public Integer getValoracion() { return borrador.valoracion; }

    /** Series marcadas: con alguna, cambiar de rutina pregunta antes. */
    public int marcadas() {
        return LogicaSesion.totales(borrador).seriesHechas;
    }

    /** La clave del intento en curso, o null si no se ha pulsado Guardar desde el último éxito. */
    @Nullable
    public String getClave() { return handle.get(K_CLAVE); }

    /** Si hay algo apuntado que se perdería al salir (GP-098): algo marcado, la nota o la cara. */
    public boolean hayDatos() {
        return marcadas() > 0 || !getNotas().trim().isEmpty() || Caras.valida(borrador.valoracion);
    }

    // La duración de entrada: la de la rutina (las de un programa no la traen: la del programa que
    // sigue); libre, la del último entreno; si no, 45.
    private int duracionDeEntrada() {
        Rutina r = rutina();
        if (r != null && r.getDuracionMinutos() > 0) return redondear(r.getDuracionMinutos());
        if (r != null && r.esDePrograma() && seguido != null && seguido.getMinutos() > 0) {
            return redondear(seguido.getMinutos());
        }
        if (borrador.rutinaId == null) {
            SesionEntrenamiento ultima = ultimaSesion();
            if (ultima != null && ultima.getDuracionMinutos() > 0) return redondear(ultima.getDuracionMinutos());
        }
        return DURACION_DEFECTO;
    }

    // Al múltiplo de 5 más cercano, de 5 a 300: es el paso de − y +.
    static int redondear(int minutos) {
        int r = Math.round(minutos / (float) DURACION_PASO) * DURACION_PASO;
        return Math.max(DURACION_MIN, Math.min(DURACION_MAX, r));
    }

    @Nullable
    private SesionEntrenamiento ultimaSesion() {
        if (sesiones == null) return null;
        SesionEntrenamiento ultima = null;
        for (SesionEntrenamiento s : sesiones) {
            if (s.getFechaInicio() == null) continue;
            if (ultima == null || s.getFechaInicio().compareTo(ultima.getFechaInicio()) > 0) ultima = s;
        }
        return ultima;
    }

    // ── Guardar ───────────────────────────────────────────────

    /**
     * Manda lo marcado. Con una petición ya en el aire no manda otra: un doble toque, o pulsar
     * tras girar la pantalla con el guardado en marcha, se queda en una sola. La clave se genera
     * solo si no había ninguna, así que un reintento reusa la del fallo.
     */
    public ResultadoGuardar guardar() {
        EstadoGuardado actual = estado.getValue();
        if (actual != null && (actual.fase == Fase.GUARDANDO || actual.fase == Fase.EXITO)) {
            return ResultadoGuardar.YA_EN_CURSO;
        }
        if (getClave() == null) handle.set(K_CLAVE, UUID.randomUUID().toString());
        handle.set(K_EN_VUELO, true);
        estado.setValue(EstadoGuardado.de(Fase.GUARDANDO));

        repo.guardarCompleta(cuerpo(), new RegistroSesionRepositorio.Respuesta<SesionEntrenamiento>() {
            @Override
            public void ok(SesionEntrenamiento creada) {
                handle.set(K_EN_VUELO, false);
                if (creada == null || creada.getId() <= 0) {
                    // Un 200 sin sesión dentro: no hay nada que enseñar en el resumen, y darlo por
                    // guardado sería mentir. Se trata como fallo y la clave sigue.
                    estado.setValue(new EstadoGuardado(Fase.FALLO, 0, null, false, null));
                    return;
                }
                // El intento terminó: el siguiente entrenamiento será otro intento.
                handle.set(K_CLAVE, null);
                estado.setValue(new EstadoGuardado(Fase.EXITO, 0, null, false, creada));
            }

            @Override
            public void fallo(int codigo, String mensaje) {
                // No se toca nada del borrador ni la clave: el reintento manda lo mismo.
                handle.set(K_EN_VUELO, false);
                estado.setValue(new EstadoGuardado(Fase.FALLO, codigo, mensaje, false, null));
            }
        });
        return ResultadoGuardar.ENVIADO;
    }

    /** «Ahora no» en el aviso de fallo: se cierra y todo sigue ahí, clave incluida. */
    public void descartarFallo() {
        handle.set(K_EN_VUELO, false);
        estado.setValue(EstadoGuardado.de(Fase.INACTIVO));
    }

    /** La pantalla ya abrió el resumen: el éxito no se vuelve a entregar. */
    public void resumenAbierto() {
        estado.setValue(EstadoGuardado.de(Fase.TERMINADO));
    }

    // El cuerpo de POST /sesiones/completa: el de la sesión en vivo (solo lo marcado), con el
    // inicio en el día elegido a la hora de ahora menos la duración (GP-012).
    private Map<String, Object> cuerpo() {
        int minutos = getDuracion();
        borrador.inicioMs = inicioMs(getFecha(), ahora.getAsLong(), minutos, zona);
        return LogicaSesion.cuerpo(borrador, getClave(), minutos, zona);
    }

    // ── Apoyo ─────────────────────────────────────────────────

    @Nullable
    private SesionEnCurso.Serie serie(long serieId) {
        for (SesionEnCurso.Ejercicio e : borrador.ejercicios) {
            for (SesionEnCurso.Serie s : e.series) if (s.id == serieId) return s;
        }
        return null;
    }

    @Nullable
    private SesionEnCurso.Ejercicio ejercicio(long id) {
        for (SesionEnCurso.Ejercicio e : borrador.ejercicios) if (e.id == id) return e;
        return null;
    }

    @NonNull
    private HashMap<Long, Boolean> pliegue() {
        HashMap<Long, Boolean> p = handle.get(K_PLIEGUE);
        if (p == null) {
            p = new HashMap<>();
            handle.set(K_PLIEGUE, p);
        }
        return p;
    }

    // Vuelve a poner el borrador en el handle (la misma instancia: así queda dicho que cambió) y,
    // si cambia lo que se ve, avisa a la pantalla. Teclear no repinta: se llevaría el foco.
    private void guardar(boolean repintar) {
        handle.set(K_SESION, borrador);
        handle.set(K_PLIEGUE, pliegue());
        if (repintar) publicar();
    }

    private void publicar() {
        Integer n = cambios.getValue();
        cambios.setValue(n == null ? 1 : n + 1);
    }

}
