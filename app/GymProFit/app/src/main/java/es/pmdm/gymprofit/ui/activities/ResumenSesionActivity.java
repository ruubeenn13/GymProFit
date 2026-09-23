package es.pmdm.gymprofit.ui.activities;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.logro.Logro;
import es.pmdm.gymprofit.model.logro.UsuarioLogro;
import es.pmdm.gymprofit.model.sesion.SesionEntrenamiento;
import es.pmdm.gymprofit.model.usuario.UsuarioEstadisticas;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.LogroApi;
import es.pmdm.gymprofit.network.SesionApi;
import es.pmdm.gymprofit.network.UsuarioApi;
import es.pmdm.gymprofit.ui.adapters.LogroAdapter;
import es.pmdm.gymprofit.utils.FechaUtils;
import es.pmdm.gymprofit.utils.NotificationHelper;
import es.pmdm.gymprofit.utils.PreferencesManager;

// ============================================================
// ResumenSesionActivity — pantalla de resumen tras registrar/consultar una sesión.
// Muestra los datos de la sesión de entrenamiento, las estadísticas globales del
// usuario y los logros desbloqueados, combinando 4 llamadas asíncronas a la API
// que se sincronizan mediante un contador atómico antes de pintar la UI.
// ============================================================
public class ResumenSesionActivity extends AppCompatActivity {

    /**
     * Extra booleano: la sesión no tiene rutina, es un entrenamiento libre (GP-057).
     *
     * <p>Lo manda quien abre esta pantalla porque ya lo sabe. Aquí se vuelve a pedir
     * la sesión a la API, pero hasta que responde el rótulo tiene que decir algo, y
     * sin el aviso diría «sin rutina asociada» un instante antes de corregirse.
     */
    public static final String EXTRA_ENTRENAMIENTO_LIBRE = "entrenamientoLibre";

    // Aplica la escala de fuente global de la app (agranda todo el texto uniformemente).
    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(es.pmdm.gymprofit.utils.ScaleUtils.wrap(newBase));
    }

    private TextView tvFecha, tvDuracion, tvRutina, tvValoracion, tvNotas, tvCompletada;
    private View layoutValoracion;
    private View layoutNotas;
    private TextView tvStatSesiones, tvStatCompletadas, tvStatMinutos, tvStatEjercicios;
    private TextView tvStatRacha, tvStatMejorRacha;
    private View layoutCelebracion;
    private TextView tvVolumenHero;
    private RecyclerView rvLogros;
    private TextView tvLogrosVacio;

    private PreferencesManager prefsManager;
    // Interfaz Retrofit tipada del dominio sesiones (etapa 2)
    private final SesionApi sesionApi = ApiClient.service(SesionApi.class);
    // Interfaz Retrofit tipada del dominio usuarios (etapa 2)
    private final UsuarioApi usuarioApi = ApiClient.service(UsuarioApi.class);
    // Interfaz Retrofit tipada del dominio logros (etapa 2)
    private final LogroApi logroApi = ApiClient.service(LogroApi.class);
    // Contador de llamadas asíncronas pendientes (sesión, estadísticas, logros totales y desbloqueados)
    private final AtomicInteger pendientes = new AtomicInteger(4);

    private SesionEntrenamiento sesion;
    private UsuarioEstadisticas estadisticas;
    private List<Logro> todosLogros = new ArrayList<>();
    private Set<Integer> desbloqueados = new HashSet<>();
    private ArrayList<String> nuevosLogros = new ArrayList<>();
    // Indica si se llegó desde el registro de una sesión (para lanzar notificaciones)
    private boolean fromRegistrar = false;
    // Aviso de quien abre la pantalla: la sesión no tiene rutina (GP-057). Lo confirma
    // después la propia sesión, cuando llega de la API.
    private boolean entrenamientoLibre = false;
    // Nombre de la rutina que traía el Intent, si lo traía. Aquí no se resuelve: el
    // mapa id→nombre lo tiene la pantalla de la que se viene.
    private String rutinaNombre;

    // Inicializa la actividad: aplica tema/idioma, recupera extras del intent
    // y dispara las 4 cargas asíncronas necesarias para el resumen.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefsManager = new PreferencesManager(this);
        prefsManager.applyTheme();
        setContentView(R.layout.activity_resumen_sesion);

        int sesionId = getIntent().getIntExtra("sesionId", -1);
        if (sesionId == -1) { finish(); return; }

        String rutinaNombre = getIntent().getStringExtra("rutinaNombre");
        entrenamientoLibre = getIntent().getBooleanExtra(EXTRA_ENTRENAMIENTO_LIBRE, false);
        ArrayList<String> extras = getIntent().getStringArrayListExtra("nuevosLogros");
        if (extras != null) {
            nuevosLogros = extras;
            fromRegistrar = true;
        }

        inicializarVistas(rutinaNombre);

        int usuarioId = prefsManager.getUsuarioId();
        cargarVolumen(sesionId);
        cargarSesion(sesionId);
        cargarEstadisticas(usuarioId);
        cargarTodosLogros();
        cargarLogrosDesbloqueados(usuarioId);
    }

    /**
     * Carga los kilos movidos en la sesión y los pinta como el número grande.
     * <p>
     * Va por su cuenta y no entra en el contador de las otras cuatro llamadas: si el
     * volumen tarda o falla, el resto del resumen se pinta igual. Cuando no hay pesos
     * registrados el bloque entero desaparece, porque un cero enorme celebra lo
     * contrario de lo que se pretende.
     *
     * @param sesionId sesión que se acaba de terminar o consultar.
     */
    private void cargarVolumen(int sesionId) {
        sesionApi.getVolumenSesion(sesionId).enqueue(new ApiCallback<Map<String, Double>>() {
            @Override
            public void onOk(Map<String, Double> cuerpo) {
                Double kilos = cuerpo == null ? null : cuerpo.get("volumenKg");
                if (kilos == null || kilos <= 0) {
                    layoutCelebracion.setVisibility(View.GONE);
                    return;
                }
                layoutCelebracion.setVisibility(View.VISIBLE);
                // Sin decimales: a esta escala el gramo no dice nada y estorba al n\u00famero.
                tvVolumenHero.setText(String.format(Locale.getDefault(), "%,d", Math.round(kilos)));
            }
            @Override
            public void onFail(int code, String message) {
                layoutCelebracion.setVisibility(View.GONE);
            }
        });
    }

    // Vincula las vistas del layout y muestra el nombre de la rutina asociada (si existe).
    private void inicializarVistas(String rutinaNombre) {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        tvFecha      = findViewById(R.id.tvFechaResumen);
        tvDuracion   = findViewById(R.id.tvDuracionResumen);
        tvRutina     = findViewById(R.id.tvRutinaResumen);
        tvValoracion = findViewById(R.id.tvValoracionResumen);
        layoutValoracion = findViewById(R.id.layoutValoracionResumen);
        layoutNotas  = findViewById(R.id.layoutNotasResumen);
        tvNotas      = findViewById(R.id.tvNotasResumen);
        tvCompletada = findViewById(R.id.tvCompletadaResumen);

        tvStatSesiones   = findViewById(R.id.tvStatSesiones);
        tvStatCompletadas = findViewById(R.id.tvStatCompletadas);
        tvStatMinutos    = findViewById(R.id.tvStatMinutos);
        tvStatEjercicios = findViewById(R.id.tvStatEjercicios);
        tvStatRacha      = findViewById(R.id.tvStatRacha);
        tvStatMejorRacha = findViewById(R.id.tvStatMejorRacha);

        layoutCelebracion = findViewById(R.id.layoutCelebracion);
        tvVolumenHero     = findViewById(R.id.tvVolumenHero);

        rvLogros      = findViewById(R.id.rvLogrosResumen);
        tvLogrosVacio = findViewById(R.id.tvLogrosVacioResumen);
        rvLogros.setLayoutManager(new LinearLayoutManager(this));

        this.rutinaNombre = rutinaNombre;
        pintarRutina();
    }

    /**
     * Escribe el rótulo de la rutina. Tres casos, no dos (GP-057):
     * <ul>
     *   <li>entrenamiento libre: la sesión no tiene rutina, y eso es un estado válido
     *       del modelo desde la primera migración, no un dato que falte;</li>
     *   <li>rutina con nombre: el que venga de quien abrió la pantalla;</li>
     *   <li>rutina sin nombre resoluble (archivada): «sin rutina asociada».</li>
     * </ul>
     *
     * <p>Se llama dos veces: al montar la vista con el aviso del Intent, y otra vez
     * cuando llega la sesión de la API, que es la que manda.
     */
    private void pintarRutina() {
        if (entrenamientoLibre) {
            tvRutina.setText(getString(R.string.sesiones_entrenamiento_libre));
        } else if (rutinaNombre != null && !rutinaNombre.isEmpty()) {
            tvRutina.setText(rutinaNombre);
        } else {
            tvRutina.setText(getString(R.string.sesiones_sin_rutina));
        }
    }

    // Obtiene los datos de la sesión de entrenamiento por su id (ya deserializados por Gson).
    private void cargarSesion(int sesionId) {
        sesionApi.getPorId(sesionId).enqueue(new ApiCallback<SesionEntrenamiento>() {
            @Override public void onOk(SesionEntrenamiento body) {
                sesion = body;
                comprobarYMostrar();
            }
            @Override public void onFail(int code, String message) { comprobarYMostrar(); }
        });
    }

    // Obtiene las estadísticas globales de entrenamiento del usuario (ya deserializadas por Gson).
    private void cargarEstadisticas(int usuarioId) {
        usuarioApi.getEstadisticas(usuarioId).enqueue(new ApiCallback<UsuarioEstadisticas>() {
            @Override public void onOk(UsuarioEstadisticas body) {
                estadisticas = body;
                comprobarYMostrar();
            }
            @Override public void onFail(int code, String message) { comprobarYMostrar(); }
        });
    }

    // Obtiene el catálogo completo de logros disponibles en la app (ya deserializado por Gson).
    private void cargarTodosLogros() {
        logroApi.getLogros().enqueue(new ApiCallback<List<Logro>>() {
            @Override public void onOk(List<Logro> lista) {
                if (lista != null) todosLogros = lista;
                comprobarYMostrar();
            }
            @Override public void onFail(int code, String message) { comprobarYMostrar(); }
        });
    }

    // Obtiene el conjunto de ids de logros ya desbloqueados por el usuario
    // (extrae el logroId de cada relación UsuarioLogro, igual que LogrosActivity).
    private void cargarLogrosDesbloqueados(int usuarioId) {
        logroApi.getLogrosDeUsuario(usuarioId).enqueue(new ApiCallback<List<UsuarioLogro>>() {
            @Override public void onOk(List<UsuarioLogro> lista) {
                Set<Integer> ids = new HashSet<>();
                if (lista != null) {
                    for (UsuarioLogro ul : lista) ids.add(ul.getLogroId());
                }
                desbloqueados = ids;
                comprobarYMostrar();
            }
            @Override public void onFail(int code, String message) {
                desbloqueados = new HashSet<>();
                comprobarYMostrar();
            }
        });
    }

    // Decrementa el contador de llamadas pendientes; cuando llegan todas a 0
    // pinta el contenido (los callbacks Retrofit ya entregan en el hilo de UI).
    private void comprobarYMostrar() {
        if (pendientes.decrementAndGet() == 0) {
            mostrarContenido();
        }
    }

    // Rellena la UI con los datos de la sesión, las estadísticas y los logros
    // desbloqueados; si viene de registrar la sesión, dispara notificaciones locales.
    private void mostrarContenido() {
        if (sesion != null) {
            // La sesión que responde la API manda sobre el aviso del Intent (GP-057):
            // si llega con rutina_id nulo es un entrenamiento libre, lo dijera quien
            // lo dijera al abrir esta pantalla.
            entrenamientoLibre = sesion.esEntrenamientoLibre();
            pintarRutina();

            String fecha = sesion.getFechaInicio();
            tvFecha.setText(fecha == null || fecha.isEmpty()
                    ? "—" : FechaUtils.formatearFechaHora(fecha));
            tvDuracion.setText(getString(R.string.sesiones_min, sesion.getDuracionMinutos()));

            // La valoración es un campo desde GP-070, y puede no estar: no valorar es
            // un caso normal y la fila se queda oculta.
            Integer valoracion = sesion.getValoracion();
            if (valoracion != null) {
                tvValoracion.setText(getString(R.string.resumen_valoracion_fmt, valoracion));
                layoutValoracion.setVisibility(View.VISIBLE);
            }

            // OJO con el null: hasta GP-070 las notas nunca venían vacías porque la app
            // metía dentro la valoración. Ahora una sesión sin notas trae null, y esto
            // reventaba la pantalla entera al volver de guardar.
            String notas = sesion.getNotas();
            if (notas != null && !notas.isEmpty()) {
                tvNotas.setText(notas);
                layoutNotas.setVisibility(View.VISIBLE);
            }
            tvCompletada.setVisibility(sesion.isCompletada() ? View.VISIBLE : View.GONE);
        }

        if (estadisticas != null) {
            tvStatSesiones.setText(String.valueOf(estadisticas.getTotalSesiones()));
            tvStatCompletadas.setText(String.valueOf(estadisticas.getSesionesCompletadas()));
            tvStatMinutos.setText(String.valueOf(estadisticas.getTotalMinutosEntrenados()));
            tvStatEjercicios.setText(String.valueOf(estadisticas.getTotalEjerciciosRealizados()));
            tvStatRacha.setText(String.valueOf(estadisticas.getRachaActualDias()));
            tvStatMejorRacha.setText(String.valueOf(estadisticas.getMejorRachaDias()));
        }

        List<Logro> filtrados = new ArrayList<>();
        for (Logro l : todosLogros) {
            if (desbloqueados.contains(l.getId())) filtrados.add(l);
        }

        if (fromRegistrar && sesion != null) {
            NotificationHelper.notificarSesionCompletada(this, sesion.getDuracionMinutos());
            if (!nuevosLogros.isEmpty()) {
                NotificationHelper.notificarLogrosDesbloqueados(this, nuevosLogros);
            }
        }

        if (filtrados.isEmpty()) {
            tvLogrosVacio.setVisibility(View.VISIBLE);
            rvLogros.setVisibility(View.GONE);
        } else {
            tvLogrosVacio.setVisibility(View.GONE);
            rvLogros.setVisibility(View.VISIBLE);
            rvLogros.setAdapter(new LogroAdapter(filtrados, desbloqueados));
        }
    }
}
