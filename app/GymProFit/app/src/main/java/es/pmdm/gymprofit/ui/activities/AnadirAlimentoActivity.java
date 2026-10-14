package es.pmdm.gymprofit.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.PageDTO;
import es.pmdm.gymprofit.model.alimento.Alimento;
import es.pmdm.gymprofit.model.alimento.Favoritos;
import es.pmdm.gymprofit.model.comida.AlimentoComida;
import es.pmdm.gymprofit.model.comida.AnadirRespuesta;
import es.pmdm.gymprofit.model.comida.CantidadAnterior;
import es.pmdm.gymprofit.model.comida.ComidaReciente;
import es.pmdm.gymprofit.network.AlimentoApi;
import es.pmdm.gymprofit.network.AlimentoComidaApi;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.ComidaApi;
import es.pmdm.gymprofit.ui.adapters.BusquedaAlimentoAdapter;
import es.pmdm.gymprofit.utils.Anadidos;
import es.pmdm.gymprofit.utils.AnadirRapido;
import es.pmdm.gymprofit.utils.CantidadFicha;
import es.pmdm.gymprofit.utils.Cantidades;
import es.pmdm.gymprofit.utils.FechaUtils;
import es.pmdm.gymprofit.utils.GruposBusqueda;
import es.pmdm.gymprofit.utils.LoadingDialog;
import es.pmdm.gymprofit.utils.Movimiento;
import es.pmdm.gymprofit.utils.PaginacionScrollListener;
import es.pmdm.gymprofit.utils.PedidoCantidad;
import es.pmdm.gymprofit.utils.UIHelper;
import es.pmdm.gymprofit.utils.UiFeedback;
import es.pmdm.gymprofit.utils.VistaEstado;
import retrofit2.Call;

// ============================================================
// AnadirAlimentoActivity — Añadir, Buscar y Favoritos (tableros 4, 5 y 8; lotes 1.6.1 a 1.6.3)
//
// Cabecera con cerrar, «Añadir alimento» y la etiqueta de la comida (decisión 15), que
// abre «¿A qué comida?» con lo que lleva cada una ese día (ElegirComida).
// Debajo, el buscador y el botón naranja del escáner.
//   · Sin escribir (o con menos de 2 letras), dos pestañas (decisión 2): «Todo», con
//     «Recientes» (seis y «Ver todos») y «Habituales», y «Favoritos», con la propuesta
//     de uno nuevo arriba y «Los que más usas». La lista de «Todo» se guarda en memoria
//     y se enseña al instante al volver a entrar, refrescándola por detrás.
//   · Buscando: Tuyo, Básicos y Productos, y al final «¿No lo encuentras?», con
//     «Escanéalo» y «Créalo» (que lleva lo escrito al nombre, GP-181).
// Cada fila tiene un «+» que añade sin preguntar (decisión 1, AnadirRapido): la última
// cantidad o lo que propondría la ficha. Se vuelve ✓ al tocar y la API va detrás, de una
// en una por alimento (Anadidos); tocar el ✓ lo quita, exacto. El resto de la fila abre la
// ficha; si la fila está en ✓, con esa línea y «Actualizar».
// Añadir ya no cierra la pantalla: lo añadido aquí, desde la ficha, desde la hoja del
// escáner o tras crear un alimento se junta en la barra de abajo con «Hecho». «Hecho»,
// cerrar y atrás hacen lo mismo: volver, esperando a lo que aún viaje. Si se añadió a una
// sola comida y no es la de origen, al volver se dice a cuál (EXTRA_ANADIDO_A).
// Mantener pulsado un alimento propio da editar y eliminar (y, a un ADMIN, desactivar uno
// del catálogo); en Favoritos, «Quitar de favoritos».
// Lote 1.6.4: en «Todo», «Comidas recientes» (B1): hasta tres comidas para la de la
// etiqueta, que se vuelven a pedir al cambiarla. «Copiar» añade todos sus alimentos a la
// comida de la etiqueta y pasa a «Copiada»; lo copiado cuenta en la barra y sus filas
// quedan en ✓, cada una con su quitar exacto. Cada fila enseña el ✓ de la comida de la
// etiqueta (GP-185), y una fila en ✓ cuyo «+» aún viaja abre la ficha al llegar (GP-183).
// Lote 1.6.6:
//   · La tercera pestaña, «Mis platos» (tablero 9): los platos y los alimentos creados
//     (GET /alimentos/mios), con «Crear un plato» y «Crear un alimento». Lo añadido aquí
//     cuenta en la barra como en las otras. Mantener pulsado un plato, editarlo o eliminarlo.
//   · En modo ingredientes (EXTRA_MODO_INGREDIENTES, desde la pantalla del plato), el «+»
//     y la ficha («Añadir al plato») añaden al plato, no a una comida, y el ✓ lo quita: no
//     hay etiqueta de comida, ni «Comidas recientes», ni «Mis platos», ni platos en ninguna
//     lista. Abajo, «3 ingredientes» y «Hecho», que devuelve la lista (EXTRA_INGREDIENTES).
//     Nada va a la API hasta guardar el plato.
// Lote 1.6.7: arriba de «Mis platos», «Lo que te encaja» (LoQueTeEncaja): de tus platos,
// favoritos y recientes, lo que cabe en las kcal que te quedan, con lo que añadiría su «+».
// Se calcula aquí, con lo que Añadir ya pide (el día, lo tuyo, favoritos, «Todo», las
// comidas recientes y lo habitual en la comida de la etiqueta), cuando ha llegado todo; un
// atajo que falla cuenta como vacío. Mientras se está en la pantalla no cambia (lo que se
// toca no se mueve); al cambiar de comida se vuelve a calcular. Sin objetivo, sin el día,
// sin kcal que queden o sin nada que quepa, no ocupa sitio.
// ============================================================
public class AnadirAlimentoActivity extends BaseActivity {

    /** Extras: la comida (DESAYUNO…CENA) y el día (yyyy-MM-dd). */
    public static final String EXTRA_TIPO = "tipoComida";
    public static final String EXTRA_FECHA = "fecha";
    /** Resultado: la comida a la que se añadió, solo si es una y no es la de origen (decisión 15). */
    public static final String EXTRA_ANADIDO_A = "anadidoA";
    /** Lo añadido por la ficha o el escáner (Anadidos.Anadido en JSON), de vuelta aquí (1.6.3). */
    public static final String EXTRA_ANADIDO = "anadido";
    /** La ficha, el escáner y crear se abren desde Añadir: vuelven aquí, sin aviso de añadido. */
    public static final String EXTRA_EN_ANADIR = "enAnadir";
    /** Lote 1.6.6: elegir ingredientes de un plato en vez de añadir a una comida. */
    public static final String EXTRA_MODO_INGREDIENTES = "modoIngredientes";
    /** Los ingredientes del plato (lista de IngredienteElegido en JSON), de ida y de vuelta. */
    public static final String EXTRA_INGREDIENTES = "ingredientes";

    static final String[] TIPOS = {"DESAYUNO", "ALMUERZO", "COMIDA", "MERIENDA", "CENA"};
    private static final int[] ANADIR_A = {R.string.comida_anadir_desayuno, R.string.comida_anadir_almuerzo,
            R.string.comida_anadir_comida, R.string.comida_anadir_merienda, R.string.comida_anadir_cena};
    private static final int[] A_LA = {R.string.a_la_desayuno, R.string.a_la_almuerzo, R.string.a_la_comida,
            R.string.a_la_merienda, R.string.a_la_cena};

    private static final int TAM_PAGINA = 30;
    private static final long ESPERA_MS = 250;
    private static final long ESPERA_SALIENDO = 300;

    // La última lista sin texto, para enseñarla al instante la próxima vez (B1). Solo en
    // memoria: se pierde con el proceso, y entonces se carga como la primera vez.
    @Nullable private static List<Alimento> listaGuardada;
    @Nullable private static Favoritos favoritosGuardados;

    private String tipoComida;
    private String tipoInicial;
    private String fecha;
    // Las kcal de cada comida del día, para la hoja; null mientras carga o si falla.
    @Nullable private Map<String, Integer> kcalDia;

    private final AlimentoApi alimentoApi = ApiClient.service(AlimentoApi.class);
    private final ComidaApi comidaApi = ApiClient.service(ComidaApi.class);
    private final AlimentoComidaApi lineaApi = ApiClient.service(AlimentoComidaApi.class);
    private final es.pmdm.gymprofit.network.PlatoApi platoApi = ApiClient.service(es.pmdm.gymprofit.network.PlatoApi.class);
    private BusquedaAlimentoAdapter adapter;
    private VistaEstado estado;
    private View etiqueta;
    private TextInputEditText etBuscar;
    private MaterialButtonToggleGroup pestanas;
    private RecyclerView rv;
    private View barra;
    private NumberFormat nf;

    // Búsqueda en curso: la petición, su texto, la página y lo acumulado.
    @Nullable private Call<PageDTO<Alimento>> enCurso;
    private int turno;
    private String consulta = "";
    private int pagina;
    private boolean ultimaPagina = true;
    private boolean cargando;
    private final List<Alimento> resultados = new ArrayList<>();
    private boolean yaEntro;
    private boolean recientesTodos;

    // Comidas recientes (1.6.4): las de la comida de la etiqueta, null mientras llegan o si
    // fallan (entonces la sección no sale), y las copiadas, «comida|id».
    @Nullable private List<ComidaReciente> comidasRecientes;
    private final java.util.Set<String> copiadas = new java.util.HashSet<>();
    private int turnoRecientes;

    // Favoritos: la pestaña, lo cargado y si hay que volver a pedirlo.
    private boolean enFavoritos;
    @Nullable private Favoritos favoritos;
    private boolean favoritosViejos = true;
    private int turnoFavoritos;

    // «Mis platos» (1.6.6): la pestaña, lo cargado y el turno de la petición.
    private boolean enPlatos;
    @Nullable private es.pmdm.gymprofit.model.plato.LoMio loMio;
    private int turnoLoMio;

    // «Lo que te encaja» (1.6.7): lo que hace falta para calcularlo, si ya ha llegado (o
    // fallado), y el resultado de la comida de la etiqueta; null sin sección.
    @Nullable private List<es.pmdm.gymprofit.model.comida.Comida> comidasDia;
    private boolean diaListo, todoListo, favoritosListos, recientesListas, habitualesListos;
    // Lo que ya lleva la comida de la etiqueta ese día (1.6.8): no se vuelve a proponer.
    private final java.util.Set<Integer> yaApuntados = new java.util.HashSet<>();
    @Nullable private List<Alimento> habituales;
    private int turnoHabituales, turnoDia;
    private boolean encajaCalculado, encajaEntro;
    @Nullable private es.pmdm.gymprofit.utils.LoQueTeEncaja.Resultado encaja;

    // Modo ingredientes (1.6.6): los del plato, que se devuelven al salir.
    private boolean modoIngredientes;
    private es.pmdm.gymprofit.utils.IngredientesPlato ingredientes;

    // Lo añadido sin salir (1.6.3) y el nombre de cada clave, para decir qué ha fallado.
    private Anadidos anadidos;
    private final Map<String, String> nombres = new HashMap<>();
    private boolean saliendo;
    // La comida de la fila en ✓ cuya ficha se ha abierto con «Actualizar».
    private String tipoActualizando = "MERIENDA";

    private final Handler espera = new Handler(Looper.getMainLooper());
    private final Runnable buscarAhora = this::buscarDesdeCero;
    private final Runnable verSaliendo = () -> {
        if (!isDestroyed()) LoadingDialog.show(this, getString(R.string.comida_saliendo));
    };

    private ActivityResultLauncher<Intent> fichaLauncher;
    private ActivityResultLauncher<Intent> escanerLauncher;
    private ActivityResultLauncher<Intent> crearLauncher;
    private ActivityResultLauncher<Intent> editarLauncher;
    private ActivityResultLauncher<Intent> actualizarLauncher;
    private ActivityResultLauncher<Intent> ingredienteLauncher;
    private ActivityResultLauncher<Intent> platoLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_anadir_alimento);
        nf = NumberFormat.getIntegerInstance(FechaUtils.localeDeLaApp(this));

        tipoComida = getIntent().getStringExtra(EXTRA_TIPO);
        if (tipoComida == null || indiceTipo(tipoComida) < 0) tipoComida = "MERIENDA";
        fecha = getIntent().getStringExtra(EXTRA_FECHA);
        tipoInicial = tipoComida;
        modoIngredientes = getIntent().getBooleanExtra(EXTRA_MODO_INGREDIENTES, false);
        ingredientes = new es.pmdm.gymprofit.utils.IngredientesPlato(leerIngredientes(
                savedInstanceState != null ? savedInstanceState.getString(EXTRA_INGREDIENTES)
                        : getIntent().getStringExtra(EXTRA_INGREDIENTES)));
        if (savedInstanceState != null) {
            tipoComida = savedInstanceState.getString(EXTRA_TIPO, tipoComida);
            enFavoritos = savedInstanceState.getBoolean("favoritos");
            enPlatos = savedInstanceState.getBoolean("platos");
            recientesTodos = savedInstanceState.getBoolean("recientesTodos");
        }
        if (modoIngredientes) enPlatos = false;
        anadidos = new Anadidos(servidor(), oyente());

        // Lo añadido desde la ficha, el escáner o lo creado vuelve aquí: cuenta en la barra.
        fichaLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), this::alVolver);
        escanerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), this::alVolver);
        crearLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), this::alVolver);
        editarLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), r -> {
            refrescar();
            if (mirandoPlatos()) cargarLoMio();
        });
        actualizarLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                this::alVolverDeActualizar);
        // Lo que vuelve de la ficha o del escáner en modo ingredientes: entra en el plato.
        ingredienteLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                this::alVolverDeIngrediente);
        // De crear o editar un plato: lo añadido desde su hoja cuenta en la barra; la
        // pestaña se recarga.
        platoLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), r -> {
            loMio = null;
            alVolver(r);
            if (mirandoPlatos()) cargarLoMio();
        });

        findViewById(R.id.btnCerrarAnadir).setOnClickListener(v -> salir());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                salir();
            }
        });
        etiqueta = findViewById(R.id.etiquetaComida);
        etiqueta.setOnClickListener(v -> elegirComida());
        es.pmdm.gymprofit.ui.nutricion.ElegirComida.colocarEtiqueta(etiqueta, findViewById(R.id.filaTituloAnadir),
                findViewById(R.id.tvTituloAnadir), findViewById(R.id.ranuraEtiquetaDerecha),
                findViewById(R.id.ranuraEtiquetaDebajo));
        pintarComida();
        if (modoIngredientes) {
            // Sin comida a la que añadir: ni etiqueta, ni el día, ni comidas recientes.
            etiqueta.setVisibility(View.GONE);
            findViewById(R.id.ranuraEtiquetaDebajo).setVisibility(View.GONE);
            ((TextView) findViewById(R.id.tvTituloAnadir)).setText(R.string.ingredientes_titulo);
            ((TextView) findViewById(R.id.etBuscarAnadir)).setHint(R.string.ingredientes_buscar_hint);
            findViewById(R.id.btnPestanaPlatos).setVisibility(View.GONE);
        } else {
            cargarDia();
            cargarRecientes();
            cargarHabituales();
        }
        findViewById(R.id.btnEscanear).setOnClickListener(v -> abrirEscaner());

        barra = findViewById(R.id.barraHecho);
        findViewById(R.id.btnHecho).setOnClickListener(v -> salir());

        estado = new VistaEstado(findViewById(R.id.estadoAnadir));
        rv = findViewById(R.id.rvAnadir);
        LinearLayoutManager lm = new LinearLayoutManager(this);
        rv.setLayoutManager(lm);
        adapter = new BusquedaAlimentoAdapter(acciones());
        rv.setAdapter(adapter);
        rv.addOnScrollListener(new PaginacionScrollListener(lm) {
            @Override protected void cargarMas() { siguientePagina(); }
            @Override protected boolean isCargando() { return cargando; }
            @Override protected boolean esUltimaPagina() { return ultimaPagina || mirandoFavoritos() || mirandoPlatos(); }
        });

        pestanas = findViewById(R.id.pestanasAnadir);
        pestanas.check(enPlatos ? R.id.btnPestanaPlatos : enFavoritos ? R.id.btnPestanaFavoritos : R.id.btnPestanaTodo);
        pintarPestanas();
        pestanas.addOnButtonCheckedListener((g, id, marcado) -> {
            if (!marcado) return;
            enFavoritos = id == R.id.btnPestanaFavoritos;
            enPlatos = id == R.id.btnPestanaPlatos;
            pintarPestanas();
            pintarActual(false);
            if (enFavoritos) cargarFavoritos();
            if (enPlatos) cargarLoMio();
        });

        etBuscar = findViewById(R.id.etBuscarAnadir);
        etBuscar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override
            public void afterTextChanged(Editable s) {
                espera.removeCallbacks(buscarAhora);
                espera.postDelayed(buscarAhora, ESPERA_MS);
            }
        });
        etBuscar.setOnEditorActionListener((v, accion, ev) -> {
            if (accion != EditorInfo.IME_ACTION_SEARCH) return false;
            espera.removeCallbacks(buscarAhora);
            buscarDesdeCero();
            return true;
        });

        // Al instante lo de la última vez; por detrás, lo de ahora.
        if (listaGuardada != null) {
            resultados.clear();
            resultados.addAll(listaGuardada);
            yaEntro = true;
        }
        favoritos = favoritosGuardados;
        pintarActual(false);
        buscarDesdeCero();
        if (enFavoritos || !modoIngredientes) cargarFavoritos();
        if (enPlatos || !modoIngredientes) cargarLoMio();
        if (modoIngredientes) pintarBarra();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle out) {
        super.onSaveInstanceState(out);
        out.putString(EXTRA_TIPO, tipoComida);
        out.putBoolean("favoritos", enFavoritos);
        out.putBoolean("platos", enPlatos);
        out.putBoolean("recientesTodos", recientesTodos);
        out.putString(EXTRA_INGREDIENTES, new Gson().toJson(ingredientes.lista()));
    }

    @Override
    protected void onDestroy() {
        espera.removeCallbacks(buscarAhora);
        espera.removeCallbacks(verSaliendo);
        LoadingDialog.hide(this);
        if (enCurso != null) enCurso.cancel();
        super.onDestroy();
    }

    // ── La comida ───────────────────────────────────────────────────────────

    static int indiceTipo(String tipo) {
        for (int i = 0; i < TIPOS.length; i++) if (TIPOS[i].equals(tipo)) return i;
        return -1;
    }

    /** «Añadir a la merienda», «Add to snack»… para la comida que toca. */
    static int textoAnadirA(String tipo) {
        int i = indiceTipo(tipo);
        return ANADIR_A[i < 0 ? 3 : i];
    }

    /** «a la merienda», «to snack»… */
    static int aLa(String tipo) {
        int i = indiceTipo(tipo);
        return A_LA[i < 0 ? 3 : i];
    }

    private void pintarComida() {
        es.pmdm.gymprofit.ui.nutricion.ElegirComida.pintarEtiqueta(etiqueta, tipoComida);
    }

    // La etiqueta abre «¿A qué comida?»; elegir cambia la etiqueta y lo que dice el «+».
    private void elegirComida() {
        es.pmdm.gymprofit.ui.nutricion.ElegirComida.abrirHoja(this, fecha, tipoComida, kcalDia, t -> {
            boolean cambia = !t.equals(tipoComida);
            tipoComida = t;
            pintarComida();
            // La etiqueta puede cambiar de ancho («Almuerzo» frente a «Cena»).
            etiqueta.requestLayout();
            // Cada fila enseña el ✓ de esta comida (GP-185), y TalkBack del «+» dice a cuál va.
            adapter.notifyDataSetChanged();
            // Las comidas recientes son para la comida de la etiqueta: se vuelven a pedir.
            // «Lo que te encaja» también es de esa comida: se recalcula cuando llegue todo.
            if (cambia) {
                olvidarEncaja();
                cargarDia();
                cargarRecientes();
                cargarHabituales();
            }
        });
    }

    // Lo que lleva cada comida del día, para la hoja. Sin esto la hoja funciona igual,
    // solo que sin las kcal: por eso un fallo no se enseña (y se reintenta al volver).
    private void cargarDia() {
        int usuarioId = prefsManager.getUsuarioId();
        if (usuarioId == -1) return;
        int miTurno = ++turnoDia;
        diaListo = false;
        comidaApi.getDeUsuarioFecha(usuarioId, dia())
                .enqueue(new ApiCallback<List<es.pmdm.gymprofit.model.comida.Comida>>() {
                    @Override
                    public void onOk(List<es.pmdm.gymprofit.model.comida.Comida> lista) {
                        if (miTurno != turnoDia || isDestroyed()) return;
                        comidasDia = lista != null ? lista : new ArrayList<>();
                        cargarYaApuntados(miTurno);
                        Map<String, Integer> m = new HashMap<>();
                        if (lista != null) {
                            for (es.pmdm.gymprofit.model.comida.Comida c : lista) {
                                m.put(c.getTipoComida(), c.getTotalCalorias());
                            }
                        }
                        kcalDia = m;
                    }

                    @Override
                    public void onFail(int code, String message) {
                        // Se ignora a propósito: la hoja sigue sirviendo para elegir, solo
                        // que sin decir lo que lleva cada comida; no hay nada que reintentar
                        // a la vista del usuario. Sin el día tampoco hay «Lo que te encaja»:
                        // no se sabe qué queda, y la sección simplemente no sale.
                        if (miTurno != turnoDia || isDestroyed()) return;
                        comidasDia = null;
                        diaListo = true;
                    }
                });
    }

    // Las líneas de la comida de la etiqueta, para no proponer en «Lo que te encaja» lo que
    // ya lleva (1.6.8). Sin esa comida no hay nada apuntado. Si falla, se propone como antes:
    // es un filtro, y sin él la sección sigue siendo cierta.
    private void cargarYaApuntados(int miTurno) {
        yaApuntados.clear();
        es.pmdm.gymprofit.model.comida.Comida esta = null;
        if (comidasDia != null) {
            for (es.pmdm.gymprofit.model.comida.Comida c : comidasDia) {
                if (tipoComida.equals(c.getTipoComida())) esta = c;
            }
        }
        if (esta == null || esta.getId() <= 0) {
            diaListo = true;
            quizaEncaja();
            return;
        }
        lineaApi.getDeComida(esta.getId()).enqueue(new ApiCallback<List<AlimentoComida>>() {
            @Override
            public void onOk(List<AlimentoComida> lineas) {
                if (miTurno != turnoDia || isDestroyed()) return;
                if (lineas != null) for (AlimentoComida l : lineas) yaApuntados.add(l.getAlimentoId());
                diaListo = true;
                quizaEncaja();
            }

            @Override
            public void onFail(int code, String message) {
                // Se ignora a propósito: sin las líneas se propone como en la 1.6.7.
                if (miTurno != turnoDia || isDestroyed()) return;
                diaListo = true;
                quizaEncaja();
            }
        });
    }

    private String dia() {
        return fecha != null ? fecha
                : new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new java.util.Date());
    }

    // ── Abrir ───────────────────────────────────────────────────────────────

    private void abrirFicha(Alimento alimento) {
        if (modoIngredientes) {
            // La ficha añade al plato («Añadir al plato») o, si ya está, cambia su cantidad.
            ingredienteLauncher.launch(FichaAlimentoActivity.paraIngrediente(this, alimento, ingredientes.de(alimento)));
            return;
        }
        String clave = clave(alimento);
        if (anadidos.marcado(clave)) {
            // Mientras el «+» aún viaja no hay línea que actualizar: se abre en cuanto llega
            // (GP-183). Si añadir falla, no se abre nada y el fallo ya se dice.
            anadidos.cuandoEste(clave, hecho -> {
                if (isDestroyed() || saliendo) return;
                tipoActualizando = hecho.getTipoComida();
                actualizarLauncher.launch(FichaAlimentoActivity.paraEditar(this, hecho.getLinea(),
                        hecho.getTipoComida(), fecha).putExtra(EXTRA_EN_ANADIR, true));
            });
            return;
        }
        fichaLauncher.launch(FichaAlimentoActivity.paraAnadir(this, alimento, tipoComida, fecha)
                .putExtra(EXTRA_EN_ANADIR, true));
    }

    private void abrirEscaner() {
        if (modoIngredientes) {
            ingredienteLauncher.launch(new Intent(this, EscanerActivity.class).putExtra(EXTRA_MODO_INGREDIENTES, true));
            return;
        }
        escanerLauncher.launch(new Intent(this, EscanerActivity.class)
                .putExtra(EXTRA_TIPO, tipoComida).putExtra(EXTRA_FECHA, fecha).putExtra(EXTRA_EN_ANADIR, true));
    }

    private void abrirCrear() {
        if (modoIngredientes) {
            // Sin comida: «Solo guardar», y al volver sale en lo tuyo para añadirlo al plato.
            crearLauncher.launch(new Intent(this, CrearAlimentoActivity.class)
                    .putExtra(CrearAlimentoActivity.EXTRA_NOMBRE, consulta));
            return;
        }
        // «Créalo» lleva lo que se buscaba al nombre (GP-181).
        crearLauncher.launch(new Intent(this, CrearAlimentoActivity.class)
                .putExtra(EXTRA_TIPO, tipoComida).putExtra(EXTRA_FECHA, fecha)
                .putExtra(CrearAlimentoActivity.EXTRA_NOMBRE, consulta)
                .putExtra(EXTRA_EN_ANADIR, true));
    }

    // De la ficha, el escáner o crear: lo añadido cuenta aquí; el corazón, se apunta.
    private void alVolver(ActivityResult r) {
        Intent datos = r.getData();
        String json = datos != null ? datos.getStringExtra(EXTRA_ANADIDO) : null;
        if (datos != null) apuntarFavorito(datos);
        if (json != null) {
            Anadidos.Anadido a = new Gson().fromJson(json, Anadidos.Anadido.class);
            nombres.put(Anadidos.clave(a.getTipoComida(), a.getAlimentoId(), null), a.getLinea().getNombreAlimento());
            anadidos.marcar(a);
            pintarBarra();
        } else if (r.getResultCode() == RESULT_OK || modoIngredientes) {
            // Lo creado sin añadir sale al refrescar.
            refrescar();
        }
        if (mirandoPlatos()) cargarLoMio();
        if (enFavoritos && favoritosViejos) cargarFavoritos();
    }

    // De la ficha o del escáner en modo ingredientes: el ingrediente entra en el plato (o
    // cambia su cantidad, si ya estaba).
    private void alVolverDeIngrediente(ActivityResult r) {
        Intent datos = r.getData();
        if (datos != null) apuntarFavorito(datos);
        String json = datos != null ? datos.getStringExtra(FichaAlimentoActivity.EXTRA_INGREDIENTE) : null;
        if (r.getResultCode() != RESULT_OK || json == null) return;
        es.pmdm.gymprofit.model.plato.IngredienteElegido i =
                new Gson().fromJson(json, es.pmdm.gymprofit.model.plato.IngredienteElegido.class);
        if (!ingredientes.tiene(i.getAlimento()) && ingredientes.lleno()) {
            UIHelper.mostrarToastError(this, getString(R.string.plato_ingredientes_maximo, maxIngredientes()));
            return;
        }
        ingredientes.poner(i);
        adapter.cambioMas(Anadidos.clave(tipoComida, i.getAlimento().getId(), i.getAlimento().getBarcode()));
        pintarBarra();
    }

    private static int maxIngredientes() {
        return es.pmdm.gymprofit.utils.CuentaPlato.INGREDIENTES_MAX;
    }

    @Nullable
    private static List<es.pmdm.gymprofit.model.plato.IngredienteElegido> leerIngredientes(@Nullable String json) {
        if (json == null) return null;
        es.pmdm.gymprofit.model.plato.IngredienteElegido[] lista =
                new Gson().fromJson(json, es.pmdm.gymprofit.model.plato.IngredienteElegido[].class);
        return lista != null ? java.util.Arrays.asList(lista) : null;
    }

    // De la ficha en «Actualizar», abierta desde una fila en ✓.
    private void alVolverDeActualizar(ActivityResult r) {
        Intent datos = r.getData();
        if (datos == null) return;
        apuntarFavorito(datos);
        String json = datos.getStringExtra(FichaAlimentoActivity.EXTRA_LINEA);
        if (r.getResultCode() == RESULT_OK && json != null) {
            AlimentoComida linea = new Gson().fromJson(json, AlimentoComida.class);
            String texto = datos.getStringExtra(FichaAlimentoActivity.EXTRA_TEXTO);
            anadidos.actualizar(tipoActualizando, linea.getAlimentoId(), linea, texto != null ? texto : "",
                    datos.getLongExtra(FichaAlimentoActivity.EXTRA_KCAL, 0));
            pintarBarra();
        }
        if (enFavoritos && favoritosViejos) cargarFavoritos();
    }

    // La ficha dice si el corazón cambió: lo que se ve aquí lo sigue.
    private void apuntarFavorito(@NonNull Intent datos) {
        if (!datos.hasExtra(FichaAlimentoActivity.EXTRA_FAVORITO)) return;
        int id = datos.getIntExtra(FichaAlimentoActivity.EXTRA_FAVORITO_ID, 0);
        boolean es = datos.getBooleanExtra(FichaAlimentoActivity.EXTRA_FAVORITO, false);
        for (Alimento a : resultados) if (id > 0 && a.getId() == id) a.setFavorito(es);
        favoritosViejos = true;
    }

    // ── Salir ───────────────────────────────────────────────────────────────

    // «Hecho», cerrar y atrás: todo está guardado o en camino. Se espera a lo que viaja y,
    // si tarda, se dice (como al salir de una comida, GP-179).
    private void salir() {
        if (modoIngredientes) {
            // Nada va a la API hasta guardar el plato: se devuelve la lista tal cual.
            setResult(RESULT_OK, new Intent().putExtra(EXTRA_INGREDIENTES, new Gson().toJson(ingredientes.lista())));
            finish();
            return;
        }
        if (saliendo) return;
        saliendo = true;
        if (anadidos.ocupado()) espera.postDelayed(verSaliendo, ESPERA_SALIENDO);
        anadidos.alTerminar(this::terminar);
    }

    private void terminar() {
        espera.removeCallbacks(verSaliendo);
        LoadingDialog.hide(this);
        if (isDestroyed()) return;
        if (!anadidos.huboCambios()) {
            setResult(RESULT_CANCELED);
            finish();
            return;
        }
        Intent datos = new Intent();
        java.util.Set<String> comidas = anadidos.resumen().comidas;
        if (comidas.size() == 1 && !comidas.contains(tipoInicial)) {
            datos.putExtra(EXTRA_ANADIDO_A, comidas.iterator().next());
        }
        setResult(RESULT_OK, datos);
        finish();
    }

    // ── El «+» ──────────────────────────────────────────────────────────────

    // La fila de ese alimento en la comida de la etiqueta (GP-185).
    private String clave(@NonNull Alimento a) {
        return Anadidos.clave(tipoComida, a.getId(), a.getBarcode());
    }

    private Anadidos.Pedido pedido(@NonNull Alimento a) {
        CantidadFicha c = AnadirRapido.cantidad(a);
        return new Anadidos.Pedido(a.getId(), a.getBarcode(), tipoComida,
                PedidoCantidad.anadir(a, c, dia(), tipoComida),
                AnadirRapido.texto(Cantidades.Formatos.de(this), FechaUtils.localeDeLaApp(this), c),
                AnadirRapido.kcal(a, c));
    }

    private void tocarMas(@NonNull Alimento a, @NonNull View boton, @NonNull ImageView icono) {
        if (modoIngredientes) {
            boolean antes = ingredientes.tiene(a);
            if (!antes && ingredientes.lleno()) {
                UIHelper.mostrarToastError(this, getString(R.string.plato_ingredientes_maximo, maxIngredientes()));
                return;
            }
            ingredientes.alternar(a, AnadirRapido.cantidad(a));
            BusquedaAlimentoAdapter.pintarMas(boton, icono, !antes);
            Movimiento.volverMas(boton, icono,
                    ContextCompat.getColor(this, antes ? R.color.gp_success_container : R.color.gp_surface_2),
                    ContextCompat.getColor(this, antes ? R.color.gp_surface_2 : R.color.gp_success_container));
            adapter.cambioMas(clave(a));
            pintarBarra();
            return;
        }
        String clave = clave(a);
        boolean antes = anadidos.marcado(clave);
        nombres.put(clave, a.getNombre());
        anadidos.tocar(pedido(a));
        // Al momento, sin esperar a la API (momento 15).
        BusquedaAlimentoAdapter.pintarMas(boton, icono, !antes);
        Movimiento.volverMas(boton, icono,
                ContextCompat.getColor(this, antes ? R.color.gp_success_container : R.color.gp_surface_2),
                ContextCompat.getColor(this, antes ? R.color.gp_surface_2 : R.color.gp_success_container));
    }

    private Anadidos.Servidor servidor() {
        return new Anadidos.Servidor() {
            @Override
            public void anadir(@NonNull Anadidos.Pedido pedido, @NonNull Anadidos.Respuesta<AnadirRespuesta> r) {
                comidaApi.anadir(pedido.cuerpo).enqueue(new ApiCallback<AnadirRespuesta>() {
                    @Override public void onOk(AnadirRespuesta respuesta) { r.ok(respuesta); }
                    @Override public void onFail(int code, String message) { r.fallo(code, message); }
                });
            }

            @Override
            public void deshacer(@NonNull Anadidos.Anadido a, @NonNull Anadidos.Respuesta<Void> r) {
                CantidadAnterior antes = a.getAnterior();
                if (antes == null) {
                    // La línea era nueva: se borra.
                    lineaApi.eliminar(a.getLinea().getId()).enqueue(new ApiCallback<Void>() {
                        @Override public void onOk(Void nada) { r.ok(null); }
                        @Override public void onFail(int code, String message) { r.fallo(code, message); }
                    });
                    return;
                }
                // Se había sumado: vuelve a la cantidad que tenía (mandan los gramos).
                Map<String, Object> cuerpo = new HashMap<>();
                cuerpo.put("cantidadGramos", decimal(antes.getCantidadGramos()));
                if (antes.getRacionId() != null && antes.getRaciones() != null) {
                    cuerpo.put("racionId", antes.getRacionId());
                    cuerpo.put("raciones", decimal(antes.getRaciones()));
                }
                lineaApi.patchLinea(a.getLinea().getId(), cuerpo).enqueue(new ApiCallback<AlimentoComida>() {
                    @Override public void onOk(AlimentoComida linea) { r.ok(null); }
                    @Override public void onFail(int code, String message) { r.fallo(code, message); }
                });
            }
        };
    }

    private static BigDecimal decimal(double valor) {
        return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros();
    }

    private Anadidos.Oyente oyente() {
        return new Anadidos.Oyente() {
            @Override
            public void cambio(@NonNull String clave) {
                if (isDestroyed()) return;
                adapter.cambioMas(clave);
                pintarBarra();
            }

            @Override
            public void fallo(@NonNull String clave, boolean alAnadir, int code, @Nullable String message) {
                if (isDestroyed()) return;
                String nombre = nombres.containsKey(clave) ? nombres.get(clave) : "";
                String porque = UiFeedback.mensaje(AnadirAlimentoActivity.this, code, message);
                UIHelper.mostrarToastError(AnadirAlimentoActivity.this, getString(
                        alAnadir ? R.string.anadir_fallo_anadir : R.string.anadir_fallo_quitar, nombre, porque));
            }
        };
    }

    // ── La barra de «Hecho» ─────────────────────────────────────────────────

    private void pintarBarra() {
        if (modoIngredientes) {
            pintarBarraIngredientes();
            return;
        }
        Anadidos.Resumen r = anadidos.resumen();
        if (r.cuantos == 0) {
            barra.setVisibility(View.GONE);
            rv.setPadding(rv.getPaddingLeft(), rv.getPaddingTop(), rv.getPaddingRight(), dp(24));
            return;
        }
        String texto = r.comidas.size() == 1
                ? getResources().getQuantityString(R.plurals.barra_anadidos_a, r.cuantos, r.cuantos,
                getString(aLa(r.comidas.iterator().next())))
                : getResources().getQuantityString(R.plurals.barra_anadidos, r.cuantos, r.cuantos);
        ((TextView) findViewById(R.id.tvResumenHecho)).setText(texto);
        String kcal = getString(R.string.barra_kcal, nf.format(r.kcal));
        ((TextView) findViewById(R.id.tvKcalHecho)).setText(kcal);
        findViewById(R.id.textosBarraHecho).setContentDescription(texto + ", " + kcal);
        if (barra.getVisibility() != View.VISIBLE) {
            barra.setVisibility(View.VISIBLE);
            // Sube con el primer añadido (ENTRA, 40 dp, como el tablero).
            Movimiento.entrarUna(barra, 0, Movimiento.ENTRA, 40);
        }
        barra.post(() -> rv.setPadding(rv.getPaddingLeft(), rv.getPaddingTop(), rv.getPaddingRight(),
                barra.getHeight() + dp(24)));
    }

    // En modo ingredientes: «3 ingredientes», lo que suman y «Hecho».
    private void pintarBarraIngredientes() {
        int n = ingredientes.cuantos();
        if (n == 0) {
            barra.setVisibility(View.GONE);
            rv.setPadding(rv.getPaddingLeft(), rv.getPaddingTop(), rv.getPaddingRight(), dp(24));
            return;
        }
        String texto = getResources().getQuantityString(R.plurals.plato_ingredientes, n, n);
        ((TextView) findViewById(R.id.tvResumenHecho)).setText(texto);
        String kcal = getString(R.string.plato_kcal_en_total, nf.format(ingredientes.total().kcal));
        ((TextView) findViewById(R.id.tvKcalHecho)).setText(kcal);
        findViewById(R.id.textosBarraHecho).setContentDescription(texto + ", " + kcal);
        if (barra.getVisibility() != View.VISIBLE) {
            barra.setVisibility(View.VISIBLE);
            Movimiento.entrarUna(barra, 0, Movimiento.ENTRA, 40);
        }
        barra.post(() -> rv.setPadding(rv.getPaddingLeft(), rv.getPaddingTop(), rv.getPaddingRight(),
                barra.getHeight() + dp(24)));
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    // ── Comidas recientes (1.6.4, B1) ───────────────────────────────────────

    // Las de la comida de la etiqueta. Mientras llegan, o si fallan, la sección no sale y
    // lo demás sigue igual: es un atajo, no hay nada que reintentar a la vista.
    private void cargarRecientes() {
        int miTurno = ++turnoRecientes;
        recientesListas = false;
        String tipo = tipoComida;
        if (comidasRecientes != null) {
            comidasRecientes = null;
            if (!mirandoFavoritos() && consulta.isEmpty() && yaEntro) pintarActual(false);
        }
        comidaApi.recientes(dia(), tipo).enqueue(new ApiCallback<List<ComidaReciente>>() {
            @Override
            public void onOk(List<ComidaReciente> lista) {
                if (miTurno != turnoRecientes || isDestroyed()) return;
                comidasRecientes = lista != null && !lista.isEmpty() ? lista : null;
                recientesListas = true;
                quizaEncaja();
                if (comidasRecientes != null && !mirandoFavoritos() && consulta.isEmpty() && yaEntro) {
                    pintarActual(false);
                }
            }

            @Override
            public void onFail(int code, String message) {
                // Se ignora a propósito: sin la sección, Añadir sirve igual (B1). Para «Lo
                // que te encaja» cuenta como que ayer no hubo nada.
                if (miTurno != turnoRecientes || isDestroyed()) return;
                recientesListas = true;
                quizaEncaja();
            }
        });
    }

    private String claveCopiada(@NonNull ComidaReciente c) {
        return tipoComida + "|" + c.getId();
    }

    // «Copiar»: pasa a «Copiada» al momento; la API va detrás. Lo copiado cuenta en la barra
    // y sus filas quedan en ✓. Si falla, vuelve a «Copiar» y se dice.
    private void copiar(@NonNull ComidaReciente c) {
        String clave = claveCopiada(c);
        if (copiadas.contains(clave)) return;
        String tipo = tipoComida;
        copiadas.add(clave);
        adapter.cambioComida(c.getId());
        Movimiento.vibrar(rv, Movimiento.Vibracion.LIGERA);
        anadidos.empezarAparte();
        Map<String, Object> cuerpo = new HashMap<>();
        cuerpo.put("comidaId", c.getId());
        cuerpo.put("fecha", dia());
        cuerpo.put("tipoComida", tipo);
        comidaApi.copiar(cuerpo).enqueue(new ApiCallback<es.pmdm.gymprofit.model.comida.CopiaRespuesta>() {
            @Override
            public void onOk(es.pmdm.gymprofit.model.comida.CopiaRespuesta r) {
                if (!isDestroyed() && r != null) {
                    for (es.pmdm.gymprofit.model.comida.CopiaRespuesta.Copiada x : r.getLineas()) {
                        marcarCopiada(c, tipo, x);
                    }
                    pintarBarra();
                }
                anadidos.terminarAparte();
            }

            @Override
            public void onFail(int code, String message) {
                if (!isDestroyed()) {
                    copiadas.remove(clave);
                    adapter.cambioComida(c.getId());
                    UIHelper.mostrarToastError(AnadirAlimentoActivity.this, getString(R.string.reciente_fallo,
                            es.pmdm.gymprofit.ui.nutricion.CopiarComida.nombre(AnadirAlimentoActivity.this, c),
                            UiFeedback.mensaje(AnadirAlimentoActivity.this, code, message)));
                }
                anadidos.terminarAparte();
            }
        });
    }

    // Un alimento copiado, como si se hubiera añadido aquí: en la barra cuenta lo copiado
    // (la cantidad y las kcal de la comida de origen), y su ✓ deja la línea como estaba.
    private void marcarCopiada(@NonNull ComidaReciente origen, @NonNull String tipo,
                               @NonNull es.pmdm.gymprofit.model.comida.CopiaRespuesta.Copiada x) {
        AlimentoComida linea = x.getLinea();
        if (linea == null) return;
        AlimentoComida deOrigen = null;
        for (AlimentoComida l : origen.getLineas()) if (l.getAlimentoId() == linea.getAlimentoId()) deOrigen = l;
        AlimentoComida cuanto = deOrigen != null ? deOrigen : linea;
        nombres.put(Anadidos.clave(tipo, linea.getAlimentoId(), null), linea.getNombreAlimento());
        anadidos.marcar(new Anadidos.Anadido(linea.getAlimentoId(), null, tipo, linea, x.getAnterior(),
                Cantidades.de(this, cuanto), cuanto.getCaloriasTotales()));
    }

    // ── Lo que dice cada fila ───────────────────────────────────────────────

    private String kcalYCantidad(String cantidad, long kcal) {
        return getString(R.string.fila_cantidad_kcal, cantidad, nf.format(kcal));
    }

    private String detalle(@NonNull Alimento a, @Nullable GruposBusqueda.Seccion s) {
        if (modoIngredientes) {
            es.pmdm.gymprofit.model.plato.IngredienteElegido i = ingredientes.de(a);
            if (i != null) {
                CantidadFicha c = i.cantidad();
                return kcalYCantidad(AnadirRapido.texto(Cantidades.Formatos.de(this), FechaUtils.localeDeLaApp(this), c),
                        AnadirRapido.kcal(a, c));
            }
            return BusquedaAlimentoAdapter.detalle(this, a);
        }
        // «Mis platos» (1.6.6): el plato dice qué es una ración y lo que vale; lo creado, lo
        // que añade su «+» y si lleva código.
        if (s == GruposBusqueda.Seccion.MIS_PLATOS && !anadidos.marcado(clave(a))) {
            return es.pmdm.gymprofit.utils.FilaPlato.de(this, a);
        }
        if (s == GruposBusqueda.Seccion.CREADOS && !anadidos.marcado(clave(a))) {
            CantidadFicha c = AnadirRapido.cantidad(a);
            String base = kcalYCantidad(AnadirRapido.texto(Cantidades.Formatos.de(this), FechaUtils.localeDeLaApp(this), c),
                    AnadirRapido.kcal(a, c));
            return a.getBarcode() != null && !a.getBarcode().trim().isEmpty()
                    ? getString(R.string.misplatos_con_codigo, base) : base;
        }
        String clave = clave(a);
        if (anadidos.marcado(clave)) {
            // En ✓, lo que se ha añadido.
            String texto = anadidos.texto(clave);
            Long kcal = anadidos.kcal(clave);
            if (texto != null && kcal != null) return kcalYCantidad(texto, kcal);
        }
        if (AnadirRapido.tieneUltima(a)) {
            CantidadFicha c = AnadirRapido.cantidad(a);
            String cantidad = AnadirRapido.texto(Cantidades.Formatos.de(this), FechaUtils.localeDeLaApp(this), c);
            long kcal = AnadirRapido.kcal(a, c);
            if (s == GruposBusqueda.Seccion.TUYO) {
                return getString(R.string.fila_ultima_vez, cantidad, nf.format(kcal));
            }
            return kcalYCantidad(cantidad, kcal);
        }
        return BusquedaAlimentoAdapter.detalle(this, a);
    }

    private String etiquetaMas(@NonNull Alimento a) {
        if (modoIngredientes) {
            if (ingredientes.tiene(a)) return getString(R.string.ingrediente_quitar_a11y, a.getNombre());
            String cantidad = AnadirRapido.texto(Cantidades.Formatos.de(this), FechaUtils.localeDeLaApp(this),
                    AnadirRapido.cantidad(a));
            return getString(R.string.ingrediente_anadir_a11y, a.getNombre(), cantidad);
        }
        String clave = clave(a);
        if (anadidos.marcado(clave)) return getString(R.string.fila_check_a11y, a.getNombre());
        CantidadFicha c = AnadirRapido.cantidad(a);
        String cantidad = AnadirRapido.texto(Cantidades.Formatos.de(this), FechaUtils.localeDeLaApp(this), c);
        return getString(R.string.fila_mas_a11y, a.getNombre(), cantidad, getString(aLa(tipoComida)));
    }

    private BusquedaAlimentoAdapter.Acciones acciones() {
        return new BusquedaAlimentoAdapter.Acciones() {
            @Override public void abrir(@NonNull Alimento alimento) { abrirFicha(alimento); }

            @Override
            public boolean tieneOpciones(@NonNull Alimento a, @Nullable GruposBusqueda.Seccion s) {
                if (modoIngredientes) return false;
                return s == GruposBusqueda.Seccion.FAVORITOS || puedeOpciones(a);
            }

            @Override
            public void opciones(@NonNull Alimento a, @Nullable GruposBusqueda.Seccion s, @NonNull View fila) {
                if (s == GruposBusqueda.Seccion.FAVORITOS) menuFavorito(a, fila);
                else mostrarMenuContextualAlimento(a, fila);
            }

            @Override public void escanear() { abrirEscaner(); }
            @Override public void crear() { abrirCrear(); }

            @Override
            public void tocarMas(@NonNull Alimento a, @NonNull View boton, @NonNull ImageView icono) {
                AnadirAlimentoActivity.this.tocarMas(a, boton, icono);
            }

            @Override
            public boolean marcado(@NonNull Alimento a) {
                return modoIngredientes ? ingredientes.tiene(a) : anadidos.marcado(clave(a));
            }

            @NonNull @Override
            public String detalle(@NonNull Alimento a, @Nullable GruposBusqueda.Seccion s) {
                return AnadirAlimentoActivity.this.detalle(a, s);
            }

            @NonNull @Override
            public String etiquetaMas(@NonNull Alimento a) {
                return AnadirAlimentoActivity.this.etiquetaMas(a);
            }

            @Override
            public void verTodos() {
                recientesTodos = true;
                pintarActual(false);
            }

            @Override public void aceptarPropuesta(@NonNull Favoritos.Propuesta p) { responderPropuesta(p, true); }
            @Override public void rechazarPropuesta(@NonNull Favoritos.Propuesta p) { responderPropuesta(p, false); }

            @Override public void copiar(@NonNull ComidaReciente c) { AnadirAlimentoActivity.this.copiar(c); }

            @Override
            public boolean copiada(@NonNull ComidaReciente c) {
                return copiadas.contains(claveCopiada(c));
            }

            @NonNull @Override
            public String aLaComida() {
                return getString(aLa(tipoComida));
            }

            @Override public void crearPlato() { abrirCrearPlato(); }

            @Override public void crearAlimento() { abrirCrear(); }

            @NonNull @Override
            public String tipoComida() {
                return tipoComida;
            }
        };
    }

    // ── Buscar ──────────────────────────────────────────────────────────────

    // Tras editar o borrar un alimento propio, o al volver de crear sin añadir.
    private void refrescar() {
        buscarDesdeCero();
    }

    private boolean mirandoFavoritos() {
        return enFavoritos && consulta.isEmpty();
    }

    private boolean mirandoPlatos() {
        return enPlatos && consulta.isEmpty();
    }

    private void buscarDesdeCero() {
        CharSequence texto = etBuscar.getText();
        consulta = GruposBusqueda.esBusqueda(texto) ? texto.toString().trim() : "";
        pagina = 0;
        ultimaPagina = true;
        pintarActual(false);
        pedir(0);
    }

    private void siguientePagina() {
        if (!ultimaPagina && !cargando) pedir(pagina + 1);
    }

    private void pedir(int numero) {
        if (enCurso != null) enCurso.cancel();
        int miTurno = ++turno;
        cargando = true;
        boolean buscando = !consulta.isEmpty();
        Call<PageDTO<Alimento>> llamada = alimentoApi.buscar(buscando ? consulta : null, null, numero, TAM_PAGINA);
        enCurso = llamada;
        llamada.enqueue(new ApiCallback<PageDTO<Alimento>>() {
            @Override
            public void onOk(PageDTO<Alimento> pag) {
                if (miTurno != turno || isDestroyed()) return;
                cargando = false;
                List<Alimento> contenido = pag != null && pag.getContent() != null ? pag.getContent() : new ArrayList<>();
                // Un plato no puede ser ingrediente de otro (DEC-047).
                if (modoIngredientes) contenido = GruposBusqueda.sinPlatos(contenido);
                if (numero == 0) resultados.clear();
                resultados.addAll(contenido);
                pagina = pag != null ? pag.getPage() : 0;
                ultimaPagina = pag == null || pag.isLast();
                if (!buscando && numero == 0) {
                    listaGuardada = new ArrayList<>(contenido);
                    todoListo = true;
                    quizaEncaja();
                }
                boolean primera = !yaEntro;
                yaEntro = true;
                if (!mirandoFavoritos() && !mirandoPlatos()) pintarActual(numero == 0 && primera);
            }

            @Override
            public void onFail(int code, String message) {
                // Una petición cancelada (otra búsqueda la ha sustituido) también llega
                // aquí: no es un error, y la nueva pintará lo suyo.
                if (miTurno != turno || isDestroyed()) return;
                cargando = false;
                if (numero == 0 && !buscando) {
                    // Sin «Todo», «Lo que te encaja» va sin recientes.
                    todoListo = true;
                    quizaEncaja();
                }
                if (numero > 0) {
                    UiFeedback.toastError(AnadirAlimentoActivity.this, code, message);
                    return;
                }
                if (mirandoFavoritos() || mirandoPlatos()) return;
                yaEntro = true;
                adapter.poner(new ArrayList<>(), false);
                estado.error(VistaEstado.mensaje(AnadirAlimentoActivity.this, R.string.anadir_error_lista, code, message),
                        AnadirAlimentoActivity.this::buscarDesdeCero);
            }
        });
    }

    // Lo que toca enseñar: con texto, la búsqueda; sin texto, la pestaña elegida.
    private void pintarActual(boolean cascada) {
        boolean buscando = !consulta.isEmpty();
        pestanas.setVisibility(buscando ? View.GONE : View.VISIBLE);
        if (mirandoFavoritos()) {
            pintarFavoritos();
            return;
        }
        if (mirandoPlatos()) {
            pintarPlatos();
            return;
        }
        if (!yaEntro) {
            // Aún no ha llegado nada que enseñar.
            adapter.poner(new ArrayList<>(), false);
            estado.cargando();
            return;
        }
        List<GruposBusqueda.Elemento> lista = GruposBusqueda.de(resultados, buscando, recientesTodos,
                buscando ? null : comidasRecientes);
        adapter.poner(lista, cascada);
        if (lista.isEmpty()) estado.vacio(R.string.anadir_vacio);
        else estado.oculto();
    }

    // ── Favoritos ───────────────────────────────────────────────────────────

    // El corazón de la pestaña, relleno cuando está elegida (tablero 8). Con letra grande,
    // tres pestañas no caben con su icono (1.6.6): sin icono y hasta dos líneas, sin cortar.
    private void pintarPestanas() {
        if (getResources().getConfiguration().fontScale >= 1.3f) {
            for (int id : new int[]{R.id.btnPestanaTodo, R.id.btnPestanaFavoritos, R.id.btnPestanaPlatos}) {
                com.google.android.material.button.MaterialButton b =
                        (com.google.android.material.button.MaterialButton) findViewById(id);
                b.setIcon(null);
                b.setMaxLines(2);
                b.setEllipsize(null);
                b.setPaddingRelative(dp(4), b.getPaddingTop(), dp(4), b.getPaddingBottom());
            }
            return;
        }
        ((com.google.android.material.button.MaterialButton) findViewById(R.id.btnPestanaFavoritos))
                .setIconResource(enFavoritos ? R.drawable.ic_ms_favorite_fill : R.drawable.ic_ms_favorite);
    }

    // ── Mis platos (1.6.6) ──────────────────────────────────────────────────

    private void cargarLoMio() {
        int miTurno = ++turnoLoMio;
        if (loMio == null && mirandoPlatos()) {
            adapter.poner(new ArrayList<>(), false);
            estado.cargando();
        }
        platoApi.loMio().enqueue(new ApiCallback<es.pmdm.gymprofit.model.plato.LoMio>() {
            @Override
            public void onOk(es.pmdm.gymprofit.model.plato.LoMio m) {
                if (miTurno != turnoLoMio || isDestroyed()) return;
                loMio = m != null ? m : new es.pmdm.gymprofit.model.plato.LoMio();
                if (!quizaEncaja() && mirandoPlatos()) pintarPlatos();
            }

            @Override
            public void onFail(int code, String message) {
                if (miTurno != turnoLoMio || isDestroyed() || !mirandoPlatos()) return;
                if (loMio != null) {
                    // Con algo ya en pantalla, se avisa y se deja lo que había.
                    UiFeedback.toastError(AnadirAlimentoActivity.this, code, message);
                    return;
                }
                adapter.poner(new ArrayList<>(), false);
                estado.error(VistaEstado.mensaje(AnadirAlimentoActivity.this, R.string.misplatos_error, code, message),
                        AnadirAlimentoActivity.this::cargarLoMio);
            }
        });
    }

    private void pintarPlatos() {
        if (loMio == null) {
            adapter.poner(new ArrayList<>(), false);
            estado.cargando();
            return;
        }
        List<GruposBusqueda.Elemento> lista = GruposBusqueda.misPlatos(loMio.getPlatos(), loMio.getAlimentos(),
                modoIngredientes ? null : encaja);
        // La sección entra en cascada la primera vez que sale (momento 16); al volver, ya está.
        boolean entra = !encajaEntro && encaja != null;
        if (entra) encajaEntro = true;
        adapter.poner(lista, entra ? GruposBusqueda.filasEncaja(lista) : -1);
        estado.oculto();
    }

    // ── «Lo que te encaja» (1.6.7) ──────────────────────────────────────────

    // Lo habitual en la comida de la etiqueta («Lo sueles tomar en…»). Si falla, cuenta
    // como que no hay nada habitual: la sección sale igual, ordenada por la proteína.
    private void cargarHabituales() {
        int miTurno = ++turnoHabituales;
        habitualesListos = false;
        comidaApi.habituales(tipoComida).enqueue(new ApiCallback<List<Alimento>>() {
            @Override
            public void onOk(List<Alimento> lista) {
                if (miTurno != turnoHabituales || isDestroyed()) return;
                habituales = lista;
                habitualesListos = true;
                quizaEncaja();
            }

            @Override
            public void onFail(int code, String message) {
                // Se ignora a propósito: es un orden, no un dato que falte a la vista.
                if (miTurno != turnoHabituales || isDestroyed()) return;
                habituales = null;
                habitualesListos = true;
                quizaEncaja();
            }
        });
    }

    // Al cambiar de comida: lo calculado era de la otra.
    private void olvidarEncaja() {
        encaja = null;
        encajaCalculado = false;
        encajaEntro = false;
        if (mirandoPlatos() && loMio != null) pintarPlatos();
    }

    /**
     * Calcula «Lo que te encaja» si ya ha llegado todo y aún no se ha hecho para esta
     * comida, y lo pinta si se está mirando «Mis platos».
     *
     * @return true si ha pintado la pestaña.
     */
    private boolean quizaEncaja() {
        if (modoIngredientes || encajaCalculado) return false;
        if (!diaListo || !todoListo || !favoritosListos || !recientesListas || !habitualesListos || loMio == null) {
            return false;
        }
        encajaCalculado = true;
        encaja = calcularEncaja();
        if (!mirandoPlatos()) return false;
        pintarPlatos();
        return true;
    }

    @Nullable
    private es.pmdm.gymprofit.utils.LoQueTeEncaja.Resultado calcularEncaja() {
        es.pmdm.gymprofit.utils.ResultadoNutricional objetivo =
                es.pmdm.gymprofit.utils.DiaNutricion.objetivo(prefsManager);
        if (objetivo == null || comidasDia == null || loMio == null) return null;
        es.pmdm.gymprofit.utils.DiaNutricion d = es.pmdm.gymprofit.utils.DiaNutricion.de(comidasDia, objetivo);
        List<Alimento> recientes = new ArrayList<>();
        if (listaGuardada != null) for (Alimento a : listaGuardada) if ("TUYO".equals(a.getGrupo())) recientes.add(a);
        java.util.Set<Integer> suele = new java.util.HashSet<>();
        if (habituales != null) for (Alimento a : habituales) suele.add(a.getId());
        java.util.Set<Integer> ayer = new java.util.HashSet<>();
        ComidaReciente deAyer = es.pmdm.gymprofit.utils.ComidasRecientes.deAyer(comidasRecientes, tipoComida, dia());
        if (deAyer != null) for (AlimentoComida l : deAyer.getLineas()) ayer.add(l.getAlimentoId());
        return es.pmdm.gymprofit.utils.LoQueTeEncaja.de(d.kcalRestantes(), d.objetivoProteinas - d.proteinas,
                loMio.getPlatos(), favoritos != null ? favoritos.getFavoritos() : null, recientes, suele, ayer,
                yaApuntados);
    }

    // «Crear un plato»: su hoja añade una ración a la comida de la etiqueta (o no).
    private void abrirCrearPlato() {
        platoLauncher.launch(new Intent(this, CrearPlatoActivity.class)
                .putExtra(EXTRA_TIPO, tipoComida).putExtra(EXTRA_FECHA, fecha));
    }

    private void cargarFavoritos() {
        int miTurno = ++turnoFavoritos;
        if (favoritos == null && mirandoFavoritos()) {
            adapter.poner(new ArrayList<>(), false);
            estado.cargando();
        }
        alimentoApi.favoritos().enqueue(new ApiCallback<Favoritos>() {
            @Override
            public void onOk(Favoritos f) {
                if (miTurno != turnoFavoritos || isDestroyed()) return;
                favoritos = f != null ? f : new Favoritos();
                favoritosGuardados = favoritos;
                favoritosViejos = false;
                favoritosListos = true;
                quizaEncaja();
                if (mirandoFavoritos()) pintarFavoritos();
            }

            @Override
            public void onFail(int code, String message) {
                if (miTurno != turnoFavoritos || isDestroyed()) return;
                // Para «Lo que te encaja», sin favoritos.
                favoritosListos = true;
                quizaEncaja();
                if (!mirandoFavoritos()) return;
                if (favoritos != null) {
                    // Con algo ya en pantalla, se avisa y se deja lo que había.
                    UiFeedback.toastError(AnadirAlimentoActivity.this, code, message);
                    return;
                }
                adapter.poner(new ArrayList<>(), false);
                estado.error(VistaEstado.mensaje(AnadirAlimentoActivity.this, R.string.favoritos_error, code, message),
                        AnadirAlimentoActivity.this::cargarFavoritos);
            }
        });
    }

    private void pintarFavoritos() {
        if (favoritos == null) {
            adapter.poner(new ArrayList<>(), false);
            estado.cargando();
            return;
        }
        List<Alimento> deFavoritos = modoIngredientes ? GruposBusqueda.sinPlatos(favoritos.getFavoritos())
                : favoritos.getFavoritos();
        Favoritos.Propuesta propuesta = favoritos.getPropuesta();
        if (modoIngredientes && propuesta != null && propuesta.getAlimento() != null && propuesta.getAlimento().esPlato()) {
            propuesta = null;
        }
        List<GruposBusqueda.Elemento> lista = GruposBusqueda.favoritos(deFavoritos, propuesta);
        adapter.poner(lista, false);
        if (deFavoritos.isEmpty() && propuesta == null) estado.vacio(R.string.favoritos_vacio);
        else estado.oculto();
    }

    // Aceptar (el corazón) o rechazar (la equis): la tarjeta se va al tocar y, si la API
    // falla, vuelve y se dice. Aceptada, el alimento entra en la lista resaltado.
    private void responderPropuesta(@NonNull Favoritos.Propuesta p, boolean aceptar) {
        if (favoritos == null) return;
        Favoritos antes = favoritos;
        int id = p.getAlimento().getId();
        List<Alimento> lista = new ArrayList<>(antes.getFavoritos());
        if (aceptar) {
            // Entra en la lista al momento, resaltado (momento 19): con ella ya no hay
            // pestaña vacía que decir (GP-184). La API confirma detrás.
            p.getAlimento().setFavorito(true);
            lista.add(0, p.getAlimento());
            adapter.resaltar(id);
            Movimiento.vibrar(rv, Movimiento.Vibracion.LIGERA);
        }
        favoritos = new Favoritos(lista, null);
        pintarFavoritos();
        Runnable bien = () -> {
            if (isDestroyed() || !aceptar) return;
            cargarFavoritos();
        };
        java.util.function.BiConsumer<Integer, String> mal = (code, message) -> {
            if (isDestroyed()) return;
            if (aceptar) p.getAlimento().setFavorito(false);
            favoritos = antes;
            if (mirandoFavoritos()) pintarFavoritos();
            UIHelper.mostrarToastError(AnadirAlimentoActivity.this, getString(R.string.propuesta_fallo,
                    UiFeedback.mensaje(AnadirAlimentoActivity.this, code, message)));
        };
        if (aceptar) {
            alimentoApi.marcarFavorito(id).enqueue(new ApiCallback<Alimento>() {
                @Override public void onOk(Alimento a) { bien.run(); }
                @Override public void onFail(int code, String message) { mal.accept(code, message); }
            });
        } else {
            alimentoApi.rechazarPropuesta(id).enqueue(new ApiCallback<Void>() {
                @Override public void onOk(Void nada) { favoritosGuardados = favoritos; }
                @Override public void onFail(int code, String message) { mal.accept(code, message); }
            });
        }
    }

    // Mantener pulsado en Favoritos: «Quitar de favoritos». Se va al momento y, si falla,
    // vuelve y se dice.
    private void menuFavorito(@NonNull Alimento alimento, @NonNull View fila) {
        List<UIHelper.MenuAction> acciones = new ArrayList<>();
        acciones.add(new UIHelper.MenuAction(R.drawable.ic_ms_favorite, getString(R.string.favoritos_quitar), true,
                () -> quitarFavorito(alimento)));
        UIHelper.mostrarMenuAnclado(this, fila, alimento.getNombre(), acciones);
    }

    private void quitarFavorito(@NonNull Alimento alimento) {
        if (favoritos == null) return;
        Favoritos antes = favoritos;
        List<Alimento> quedan = new ArrayList<>(antes.getFavoritos());
        quedan.remove(alimento);
        favoritos = new Favoritos(quedan, antes.getPropuesta());
        pintarFavoritos();
        for (Alimento a : resultados) if (a.getId() == alimento.getId()) a.setFavorito(false);
        alimentoApi.quitarFavorito(alimento.getId()).enqueue(new ApiCallback<Void>() {
            @Override
            public void onOk(Void nada) {
                favoritosGuardados = favoritos;
            }

            @Override
            public void onFail(int code, String message) {
                if (isDestroyed()) return;
                favoritos = antes;
                if (mirandoFavoritos()) pintarFavoritos();
                UIHelper.mostrarToastError(AnadirAlimentoActivity.this, getString(R.string.favorito_fallo_quitar,
                        UiFeedback.mensaje(AnadirAlimentoActivity.this, code, message)));
            }
        });
    }

    // ── Mantener pulsado: lo de siempre ─────────────────────────────────────

    private boolean puedeOpciones(Alimento alimento) {
        if (alimento.esExterno()) return false;
        boolean esAdmin = "ROLE_ADMIN".equals(prefsManager.getRol());
        boolean esPropio = alimento.getUsuarioId() != null && alimento.getUsuarioId() == prefsManager.getUsuarioId();
        return esPropio || esAdmin;
    }

    // Construye el menú contextual (editar/desactivar/eliminar) según el rol y si es propio o predefinido
    private void mostrarMenuContextualAlimento(Alimento alimento, View anchorView) {
        boolean esAdmin = "ROLE_ADMIN".equals(prefsManager.getRol());
        boolean esPredefinido = alimento.getUsuarioId() == null;

        List<UIHelper.MenuAction> actions = new ArrayList<>();
        boolean plato = alimento.esPlato();
        actions.add(new UIHelper.MenuAction(R.drawable.ic_ms_edit,
                getString(plato ? R.string.plato_menu_editar : R.string.alimento_editar),
                () -> mostrarDialogoEditarAlimento(alimento)));
        if (esAdmin && esPredefinido) {
            actions.add(new UIHelper.MenuAction(R.drawable.ic_ms_visibility_off, getString(R.string.comida_desactivar_alimento),
                    () -> UIHelper.mostrarDialogoConIcono(this,
                            getString(R.string.comida_desactivar_alimento),
                            getString(R.string.alimento_desactivar_confirmar),
                            R.drawable.ic_ms_visibility_off,
                            () -> desactivarAlimento(alimento))));
        }
        actions.add(new UIHelper.MenuAction(R.drawable.ic_ms_delete,
                getString(plato ? R.string.plato_menu_eliminar : R.string.alimento_eliminar), true,
                () -> UIHelper.mostrarDialogoConIcono(this,
                        getString(plato ? R.string.plato_menu_eliminar : R.string.alimento_eliminar),
                        getString(plato ? R.string.plato_eliminar_confirmar : R.string.alimento_eliminar_confirmar),
                        R.drawable.ic_ms_delete,
                        () -> eliminarAlimento(alimento))));
        UIHelper.mostrarMenuAnclado(this, anchorView, alimento.getNombre(), actions);
    }

    // Editar abre la pantalla de crear en modo edición (decisión 17): la misma etiqueta,
    // la ración y la categoría, en vez del diálogo de antes. Al volver, se recarga.
    private void mostrarDialogoEditarAlimento(Alimento alimento) {
        if (alimento.esPlato()) {
            // Un plato se edita en su pantalla (1.6.6), sin hoja al acabar.
            platoLauncher.launch(CrearPlatoActivity.paraEditar(this, alimento.getId()));
            return;
        }
        editarLauncher.launch(CrearAlimentoActivity.paraEditar(this, alimento.getId()));
    }

    // Desactiva un alimento predefinido (solo admin), sin borrarlo de la BD
    private void desactivarAlimento(Alimento alimento) {
        // Muestra el spinner modal mientras se desactiva el alimento predefinido
        LoadingDialog.show(this);
        alimentoApi.eliminar(alimento.getId()).enqueue(new ApiCallback<Void>() {
            @Override
            public void onOk(Void ignored) {
                // Oculta el spinner al confirmarse la desactivación
                LoadingDialog.hide(AnadirAlimentoActivity.this);
                refrescar();
            }
            @Override
            public void onFail(int code, String message) {
                // Oculta el spinner y mapea el código de error a un mensaje de usuario
                LoadingDialog.hide(AnadirAlimentoActivity.this);
                UiFeedback.toastError(AnadirAlimentoActivity.this, code, message);
            }
        });
    }

    // Elimina (lógicamente, desactivando) un alimento propio del usuario
    private void eliminarAlimento(Alimento alimento) {
        // Muestra el spinner modal mientras se elimina el alimento propio
        LoadingDialog.show(this);
        alimentoApi.eliminar(alimento.getId()).enqueue(new ApiCallback<Void>() {
            @Override
            public void onOk(Void ignored) {
                // Oculta el spinner al confirmarse la eliminación
                LoadingDialog.hide(AnadirAlimentoActivity.this);
                refrescar();
                if (mirandoPlatos()) cargarLoMio();
            }
            @Override
            public void onFail(int code, String message) {
                // Oculta el spinner y mapea el código de error a un mensaje de usuario
                LoadingDialog.hide(AnadirAlimentoActivity.this);
                UiFeedback.toastError(AnadirAlimentoActivity.this, code, message);
            }
        });
    }
}
