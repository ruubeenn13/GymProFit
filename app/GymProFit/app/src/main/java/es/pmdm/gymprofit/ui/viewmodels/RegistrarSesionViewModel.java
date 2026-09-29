package es.pmdm.gymprofit.ui.viewmodels;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import es.pmdm.gymprofit.model.rutina.Rutina;
import es.pmdm.gymprofit.model.rutina.RutinaEjercicio;
import es.pmdm.gymprofit.model.sesion.SesionEntrenamiento;
import es.pmdm.gymprofit.network.RegistroSesionRepositorio;
import es.pmdm.gymprofit.ui.adapters.EjercicioPesoAdapter;
import es.pmdm.gymprofit.utils.AvisoDescartar;
import es.pmdm.gymprofit.utils.Numeros;
import es.pmdm.gymprofit.utils.Valoracion;

// ============================================================
// RegistrarSesionViewModel — el borrador de Registrar sesión, fuente de verdad (GP-016).
//
// Antes lo tecleado vivía en la Activity y en los ítems del adaptador, y la clave de
// idempotencia en un campo: al girar la pantalla, o si Android mataba la app en segundo
// plano, se perdían las series y un reintento posterior salía con otra clave.
//
// Ahora todo el borrador vive aquí y se escribe en el SavedStateHandle, que sobrevive a
// las dos cosas: la rutina elegida, las series TAL CUAL se escribieron (texto, no
// números: «72,» a medio teclear también), la duración, las notas, la valoración, la
// clave del intento y si había un guardado en vuelo. Las listas de rutinas NO: se
// vuelven a pedir, porque no son del usuario y no se pierde nada al repetirlas.
//
// La idempotencia de GP-006 no cambia: la clave nace al pulsar Guardar, el reintento la
// reusa y solo se limpia con el éxito. Qué pasa si se edita tras un fallo es de GP-012.
// ============================================================
public class RegistrarSesionViewModel extends ViewModel implements EjercicioPesoAdapter.Editor {

    // Claves del borrador en el SavedStateHandle.
    static final String K_INICIADO = "borrador_iniciado";
    static final String K_RUTINA = "borrador_rutina";
    static final String K_EJERCICIOS = "borrador_ejercicios";
    static final String K_EJERCICIOS_DE = "borrador_ejercicios_de";
    static final String K_DURACION = "borrador_duracion";
    static final String K_NOTAS = "borrador_notas";
    static final String K_VALORACION = "borrador_valoracion";
    static final String K_CLAVE = "borrador_clave";
    static final String K_EN_VUELO = "borrador_en_vuelo";

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
    public enum ResultadoGuardar { ENVIADO, YA_EN_CURSO, FALTA_DURACION, DURACION_INVALIDA }

    /** Un fallo de red tal cual llega, para que la pantalla lo traduzca con UiFeedback. */
    public static final class Fallo {
        public final int codigo;
        public final String mensaje;

        Fallo(int codigo, String mensaje) {
            this.codigo = codigo;
            this.mensaje = mensaje;
        }
    }

    /**
     * Algo que se enseña una vez (un toast). Al girar, la Activity nueva vuelve a recibir
     * el último valor del LiveData; con esto lo encuentra ya consumido y no lo repite.
     */
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

    private final MutableLiveData<List<Rutina>> rutinas = new MutableLiveData<>();
    private final MutableLiveData<List<EjercicioPesoAdapter.Item>> ejercicios = new MutableLiveData<>();
    private final MutableLiveData<EstadoGuardado> estado = new MutableLiveData<>();
    private final MutableLiveData<Evento<Fallo>> errorEjercicios = new MutableLiveData<>();

    // La lista del borrador. Es la misma instancia que está en el handle y la que pinta
    // el adaptador: escribir en ella y volver a ponerla en el handle es guardar.
    private ArrayList<EjercicioPesoAdapter.Item> borrador;
    // De esta instancia del ViewModel: las rutinas se piden una vez por instancia (girar
    // no las repite; volver tras matar el proceso, sí).
    private boolean rutinasPedidas;
    // La rutina cuyos ejercicios se están pidiendo, para descartar respuestas viejas.
    private Integer rutinaPedida;

    /**
     * @param handle el estado guardado; vacío la primera vez, con el borrador al restaurar.
     * @param repo   las cuatro llamadas de la pantalla.
     */
    @SuppressWarnings("unchecked")
    public RegistrarSesionViewModel(@NonNull SavedStateHandle handle, @NonNull RegistroSesionRepositorio repo) {
        this.handle = handle;
        this.repo = repo;

        ArrayList<EjercicioPesoAdapter.Item> guardados = handle.get(K_EJERCICIOS);
        borrador = guardados != null ? guardados : new ArrayList<>();
        ejercicios.setValue(borrador);

        // Si el proceso murió con el guardado en vuelo, nadie va a recibir la respuesta.
        // No se da por guardado (no hay sesión que enseñar) ni por perdido (puede que
        // llegara): se ofrece reintentar con la misma clave, que no duplica.
        if (Boolean.TRUE.equals(handle.get(K_EN_VUELO))) {
            estado.setValue(new EstadoGuardado(Fase.FALLO, -1, null, true, null));
        } else {
            estado.setValue(EstadoGuardado.de(Fase.INACTIVO));
        }
    }

    /** Solo para los tests: lo que Android guardaría al matar el proceso. */
    SavedStateHandle handle() { return handle; }

    /**
     * Arranca la pantalla. Se llama en cada onCreate y solo hace lo que falte.
     *
     * @param rutinaDeEntrada la rutina con la que se abrió la pantalla, o null. Solo vale
     *                        la primera vez: al restaurar manda lo que eligió el usuario.
     */
    public void iniciar(@Nullable Integer rutinaDeEntrada) {
        if (!Boolean.TRUE.equals(handle.get(K_INICIADO))) {
            handle.set(K_INICIADO, true);
            handle.set(K_RUTINA, rutinaDeEntrada);
        }
        if (!rutinasPedidas) {
            rutinasPedidas = true;
            pedirRutinas();
        }
        // Con una rutina elegida y sin sus ejercicios (primera vez, o el proceso murió
        // mientras llegaban), se piden. Si ya están, restaurar no los vuelve a pedir: lo
        // tecleado está encima y una lista nueva lo pisaría.
        Integer rutina = getRutinaId();
        if (rutina != null && !rutina.equals(handle.get(K_EJERCICIOS_DE)) && !rutina.equals(rutinaPedida)) {
            pedirEjercicios(rutina);
        }
    }

    // Pide las rutinas del usuario, las propias y las de su programa. Las predefinidas
    // ya no: desde la 1.2.0 no hay ninguna, las sustituyen los programas (GP-074).
    private void pedirRutinas() {
        repo.rutinasDelUsuario(new RegistroSesionRepositorio.Respuesta<List<Rutina>>() {
            @Override public void ok(List<Rutina> lista) {
                rutinas.setValue(lista != null ? lista : new ArrayList<>());
            }
            @Override public void fallo(int codigo, String mensaje) {
                // Sin sus rutinas la sesión se puede guardar igual como entrenamiento
                // libre: el selector se queda solo con esa opción, como antes de GP-016.
                rutinas.setValue(new ArrayList<>());
            }
        });
    }

    /**
     * El usuario elige rutina en el selector (null = entrenamiento libre). Otra rutina
     * recarga sus ejercicios, como siempre; la misma que ya está cargada no hace nada.
     */
    public void elegirRutina(@Nullable Integer rutinaId) {
        if (Objects.equals(rutinaId, getRutinaId())
                && (rutinaId == null || rutinaId.equals(handle.get(K_EJERCICIOS_DE)) || rutinaId.equals(rutinaPedida))) {
            return;
        }
        handle.set(K_RUTINA, rutinaId);
        if (rutinaId == null) {
            rutinaPedida = null;
            ponerEjercicios(new ArrayList<>(), null);
        } else {
            pedirEjercicios(rutinaId);
        }
    }

    // Pide los ejercicios de una rutina y los convierte en el borrador vacío de la sesión.
    private void pedirEjercicios(int rutinaId) {
        rutinaPedida = rutinaId;
        repo.ejerciciosDeRutina(rutinaId, new RegistroSesionRepositorio.Respuesta<List<RutinaEjercicio>>() {
            @Override
            public void ok(List<RutinaEjercicio> lista) {
                // Una respuesta de una rutina que ya no está elegida llega tarde: se tira.
                if (!Integer.valueOf(rutinaId).equals(rutinaPedida)) return;
                rutinaPedida = null;
                ArrayList<EjercicioPesoAdapter.Item> items = new ArrayList<>();
                if (lista != null) {
                    for (RutinaEjercicio re : lista) {
                        if (re.getEjercicioId() == -1) continue;
                        String nombre = re.getNombreEjercicio();
                        items.add(new EjercicioPesoAdapter.Item(re.getEjercicioId(),
                                nombre != null && !nombre.isEmpty() ? nombre : null,
                                re.getSeries(), re.getRepeticiones()));
                    }
                }
                ponerEjercicios(items, rutinaId);
            }

            @Override
            public void fallo(int codigo, String mensaje) {
                if (!Integer.valueOf(rutinaId).equals(rutinaPedida)) return;
                rutinaPedida = null;
                // Sin ejercicios la sesión se puede guardar igual con duración y notas. Se
                // avisa: callarlo haría creer que la rutina no tiene ejercicios. No se marca
                // como cargada, así que volver a elegirla los pide otra vez.
                ponerEjercicios(new ArrayList<>(), null);
                errorEjercicios.setValue(new Evento<>(new Fallo(codigo, mensaje)));
            }
        });
    }

    // Sustituye el borrador de ejercicios entero (solo al cambiar de rutina).
    private void ponerEjercicios(ArrayList<EjercicioPesoAdapter.Item> items, @Nullable Integer deRutina) {
        borrador = items;
        handle.set(K_EJERCICIOS, borrador);
        handle.set(K_EJERCICIOS_DE, deRutina);
        ejercicios.setValue(borrador);
    }

    // ── Lo que teclea el usuario ──────────────────────────────

    /** Escribir un peso marca la serie como hecha (si no, cuatro series sin tocar el check serían «0 hechas»). */
    @Override
    public void peso(int ejercicio, int serie, String valor) {
        EjercicioPesoAdapter.Serie s = serie(ejercicio, serie);
        if (s == null) return;
        s.peso = valor;
        if (!valor.isEmpty()) s.completada = true;
        guardarBorrador();
    }

    @Override
    public void repeticiones(int ejercicio, int serie, String valor) {
        EjercicioPesoAdapter.Serie s = serie(ejercicio, serie);
        if (s == null) return;
        s.repeticiones = valor;
        guardarBorrador();
    }

    /** El check queda para lo contrario del peso: desmarcar una serie apuntada que no se terminó. */
    @Override
    public void alternarHecha(int ejercicio, int serie) {
        EjercicioPesoAdapter.Serie s = serie(ejercicio, serie);
        if (s == null) return;
        s.completada = !s.completada;
        guardarBorrador();
    }

    // La serie por índices, o null si el adaptador avisa de una fila que ya no existe.
    @Nullable
    private EjercicioPesoAdapter.Serie serie(int ejercicio, int serie) {
        if (ejercicio < 0 || ejercicio >= borrador.size()) return null;
        List<EjercicioPesoAdapter.Serie> series = borrador.get(ejercicio).realizadas;
        return serie >= 0 && serie < series.size() ? series.get(serie) : null;
    }

    // Vuelve a poner la lista en el handle. Es la misma instancia, pero así queda dicho
    // que ha cambiado; y sin LiveData detrás de esa clave, no repinta nada al teclear.
    private void guardarBorrador() {
        handle.set(K_EJERCICIOS, borrador);
    }

    public void setDuracion(String valor) { handle.set(K_DURACION, valor); }

    public void setNotas(String valor) { handle.set(K_NOTAS, valor); }

    public void setValoracion(float estrellas) { handle.set(K_VALORACION, estrellas); }

    // ── Lecturas ──────────────────────────────────────────────

    public LiveData<List<Rutina>> getRutinas() { return rutinas; }

    public LiveData<List<EjercicioPesoAdapter.Item>> getEjercicios() { return ejercicios; }

    public LiveData<EstadoGuardado> getEstadoGuardado() { return estado; }

    public LiveData<Evento<Fallo>> getErrorEjercicios() { return errorEjercicios; }

    @Nullable
    public Integer getRutinaId() { return handle.get(K_RUTINA); }

    @NonNull
    public String getDuracion() { return texto(K_DURACION); }

    @NonNull
    public String getNotas() { return texto(K_NOTAS); }

    public float getValoracion() {
        Float v = handle.get(K_VALORACION);
        return v != null ? v : 0f;
    }

    /** La clave del intento en curso, o null si no se ha pulsado Guardar desde el último éxito. */
    @Nullable
    public String getClave() { return handle.get(K_CLAVE); }

    private String texto(String clave) {
        String v = handle.get(clave);
        return v != null ? v : "";
    }

    /** Nombre de una rutina de la lista, o "" si no está (o es entrenamiento libre). */
    @NonNull
    public String nombreRutina(@Nullable Integer rutinaId) {
        List<Rutina> lista = rutinas.getValue();
        if (rutinaId == null || lista == null) return "";
        for (Rutina r : lista) {
            if (r.getId() == rutinaId) return r.getNombre() != null ? r.getNombre() : "";
        }
        return "";
    }

    /** Si hay algo apuntado que se perdería al salir (GP-098). */
    public boolean hayDatos() {
        return AvisoDescartar.sesionConDatos(getDuracion(), getNotas(), getValoracion(), borrador);
    }

    // ── Guardar ───────────────────────────────────────────────

    /**
     * Valida la duración y manda la sesión entera.
     *
     * <p>Con una petición ya en el aire no manda otra: un doble toque, o pulsar tras girar
     * la pantalla con el guardado en marcha, se queda en una sola. La clave se genera solo
     * si no había ninguna, así que un reintento reusa la del fallo.
     */
    public ResultadoGuardar guardar() {
        EstadoGuardado actual = estado.getValue();
        if (actual != null && (actual.fase == Fase.GUARDANDO || actual.fase == Fase.EXITO)) {
            return ResultadoGuardar.YA_EN_CURSO;
        }

        String dur = getDuracion().trim();
        if (dur.isEmpty()) return ResultadoGuardar.FALTA_DURACION;
        Integer minutos = Numeros.entero(dur, 1, 600);
        if (minutos == null) return ResultadoGuardar.DURACION_INVALIDA;

        if (getClave() == null) handle.set(K_CLAVE, UUID.randomUUID().toString());
        handle.set(K_EN_VUELO, true);
        estado.setValue(EstadoGuardado.de(Fase.GUARDANDO));

        repo.guardarCompleta(cuerpo(minutos), new RegistroSesionRepositorio.Respuesta<SesionEntrenamiento>() {
            @Override
            public void ok(SesionEntrenamiento creada) {
                handle.set(K_EN_VUELO, false);
                if (creada == null || creada.getId() <= 0) {
                    // Un 200 sin sesión dentro: no hay nada que enseñar en el resumen, y
                    // darlo por guardado sería mentir. Se trata como fallo y la clave sigue.
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

    // El cuerpo de POST /sesiones/completa con lo que hay en el borrador.
    private Map<String, Object> cuerpo(int minutos) {
        Map<String, Object> body = new HashMap<>();
        body.put("claveIdempotencia", getClave());
        Integer rutina = getRutinaId();
        if (rutina != null) body.put("rutinaId", rutina);
        body.put("fechaInicio", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(new Date()));
        body.put("duracionMinutos", minutos);
        body.put("completada", true);

        // La valoración viaja como campo (GP-070); sin estrellas no se manda y la base
        // guarda NULL, no un valor que el usuario no ha dado (GP-077).
        Integer valoracion = Valoracion.paraEnviar(getValoracion());
        if (valoracion != null) body.put("valoracion", valoracion);

        String notas = getNotas().trim();
        if (!notas.isEmpty()) body.put("notas", notas);

        body.put("ejercicios", ejerciciosDelCuerpo());
        return body;
    }

    // Los ejercicios sin ninguna serie que guardar se descartan: no se llegaron a hacer.
    private List<Map<String, Object>> ejerciciosDelCuerpo() {
        List<Map<String, Object>> lista = new ArrayList<>();
        for (EjercicioPesoAdapter.Item item : borrador) {
            List<Map<String, Object>> series = seriesDelCuerpo(item);
            if (series.isEmpty()) continue;
            Map<String, Object> ejercicio = new HashMap<>();
            ejercicio.put("ejercicioId", item.ejercicioId);
            ejercicio.put("repeticionesReales", item.repeticiones);
            ejercicio.put("series", series);
            lista.add(ejercicio);
        }
        return lista;
    }

    /**
     * El texto de cada serie pasa por {@link Numeros}, que acepta coma o punto y devuelve
     * null fuera de rango: un valor imposible se descarta, nunca se inventa otro.
     */
    private List<Map<String, Object>> seriesDelCuerpo(EjercicioPesoAdapter.Item item) {
        List<Map<String, Object>> series = new ArrayList<>();
        for (EjercicioPesoAdapter.Serie serie : item.realizadas) {
            Integer reps = Numeros.entero(serie.repeticiones, 0, 100);
            BigDecimal peso = Numeros.exacto(serie.peso, 0, 500);
            // Una serie en blanco es una serie que no se hizo.
            if (reps == null && peso == null) continue;
            Map<String, Object> fila = new HashMap<>();
            fila.put("numero", serie.numero);
            fila.put("repeticiones", reps != null ? reps : 0);
            if (peso != null) fila.put("peso", peso);
            fila.put("completada", serie.completada);
            series.add(fila);
        }
        return series;
    }
}
