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

    // Favoritos: la pestaña, lo cargado y si hay que volver a pedirlo.
    private boolean enFavoritos;
    @Nullable private Favoritos favoritos;
    private boolean favoritosViejos = true;
    private int turnoFavoritos;

    // Lo añadido sin salir (1.6.3) y el nombre de cada clave, para decir qué ha fallado.
    private Anadidos anadidos;
    private final Map<String, String> nombres = new HashMap<>();
    private boolean saliendo;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_anadir_alimento);
        nf = NumberFormat.getIntegerInstance(FechaUtils.localeDeLaApp(this));

        tipoComida = getIntent().getStringExtra(EXTRA_TIPO);
        if (tipoComida == null || indiceTipo(tipoComida) < 0) tipoComida = "MERIENDA";
        fecha = getIntent().getStringExtra(EXTRA_FECHA);
        tipoInicial = tipoComida;
        if (savedInstanceState != null) {
            tipoComida = savedInstanceState.getString(EXTRA_TIPO, tipoComida);
            enFavoritos = savedInstanceState.getBoolean("favoritos");
            recientesTodos = savedInstanceState.getBoolean("recientesTodos");
        }
        anadidos = new Anadidos(servidor(), oyente());

        // Lo añadido desde la ficha, el escáner o lo creado vuelve aquí: cuenta en la barra.
        fichaLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), this::alVolver);
        escanerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), this::alVolver);
        crearLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), this::alVolver);
        editarLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                r -> refrescar());
        actualizarLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                this::alVolverDeActualizar);

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
        cargarDia();
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
            @Override protected boolean esUltimaPagina() { return ultimaPagina || mirandoFavoritos(); }
        });

        pestanas = findViewById(R.id.pestanasAnadir);
        pestanas.check(enFavoritos ? R.id.btnPestanaFavoritos : R.id.btnPestanaTodo);
        pintarPestanas();
        pestanas.addOnButtonCheckedListener((g, id, marcado) -> {
            if (!marcado) return;
            enFavoritos = id == R.id.btnPestanaFavoritos;
            pintarPestanas();
            pintarActual(false);
            if (enFavoritos) cargarFavoritos();
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
        if (enFavoritos) cargarFavoritos();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle out) {
        super.onSaveInstanceState(out);
        out.putString(EXTRA_TIPO, tipoComida);
        out.putBoolean("favoritos", enFavoritos);
        out.putBoolean("recientesTodos", recientesTodos);
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
            tipoComida = t;
            pintarComida();
            // La etiqueta puede cambiar de ancho («Almuerzo» frente a «Cena»).
            etiqueta.requestLayout();
            // TalkBack del «+» dice a qué comida va.
            adapter.notifyDataSetChanged();
        });
    }

    // Lo que lleva cada comida del día, para la hoja. Sin esto la hoja funciona igual,
    // solo que sin las kcal: por eso un fallo no se enseña (y se reintenta al volver).
    private void cargarDia() {
        int usuarioId = prefsManager.getUsuarioId();
        if (usuarioId == -1) return;
        comidaApi.getDeUsuarioFecha(usuarioId, dia())
                .enqueue(new ApiCallback<List<es.pmdm.gymprofit.model.comida.Comida>>() {
                    @Override
                    public void onOk(List<es.pmdm.gymprofit.model.comida.Comida> lista) {
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
                        // a la vista del usuario.
                    }
                });
    }

    private String dia() {
        return fecha != null ? fecha
                : new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new java.util.Date());
    }

    // ── Abrir ───────────────────────────────────────────────────────────────

    private void abrirFicha(Alimento alimento) {
        String clave = Anadidos.clave(alimento.getId(), alimento.getBarcode());
        if (anadidos.marcado(clave)) {
            Anadidos.Anadido hecho = anadidos.hecho(clave);
            // Mientras el «+» aún viaja no hay línea que actualizar: llega en un momento.
            if (hecho == null) return;
            actualizarLauncher.launch(FichaAlimentoActivity.paraEditar(this, hecho.getLinea(), hecho.getTipoComida(),
                    fecha).putExtra(EXTRA_EN_ANADIR, true));
            return;
        }
        fichaLauncher.launch(FichaAlimentoActivity.paraAnadir(this, alimento, tipoComida, fecha)
                .putExtra(EXTRA_EN_ANADIR, true));
    }

    private void abrirEscaner() {
        escanerLauncher.launch(new Intent(this, EscanerActivity.class)
                .putExtra(EXTRA_TIPO, tipoComida).putExtra(EXTRA_FECHA, fecha).putExtra(EXTRA_EN_ANADIR, true));
    }

    private void abrirCrear() {
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
            nombres.put(Anadidos.clave(a.getAlimentoId(), null), a.getLinea().getNombreAlimento());
            anadidos.marcar(a);
            pintarBarra();
        } else if (r.getResultCode() == RESULT_OK) {
            // Lo creado sin añadir sale al refrescar.
            refrescar();
        }
        if (enFavoritos && favoritosViejos) cargarFavoritos();
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
            anadidos.actualizar(linea.getAlimentoId(), linea, texto != null ? texto : "",
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

    private Anadidos.Pedido pedido(@NonNull Alimento a) {
        CantidadFicha c = AnadirRapido.cantidad(a);
        return new Anadidos.Pedido(a.getId(), a.getBarcode(), tipoComida,
                PedidoCantidad.anadir(a, c, dia(), tipoComida),
                AnadirRapido.texto(Cantidades.Formatos.de(this), FechaUtils.localeDeLaApp(this), c),
                AnadirRapido.kcal(a, c));
    }

    private void tocarMas(@NonNull Alimento a, @NonNull View boton, @NonNull ImageView icono) {
        String clave = Anadidos.clave(a.getId(), a.getBarcode());
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

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    // ── Lo que dice cada fila ───────────────────────────────────────────────

    private String kcalYCantidad(String cantidad, long kcal) {
        return getString(R.string.fila_cantidad_kcal, cantidad, nf.format(kcal));
    }

    private String detalle(@NonNull Alimento a, @Nullable GruposBusqueda.Seccion s) {
        String clave = Anadidos.clave(a.getId(), a.getBarcode());
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
        String clave = Anadidos.clave(a.getId(), a.getBarcode());
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
                return anadidos.marcado(Anadidos.clave(a.getId(), a.getBarcode()));
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
                if (numero == 0) resultados.clear();
                resultados.addAll(contenido);
                pagina = pag != null ? pag.getPage() : 0;
                ultimaPagina = pag == null || pag.isLast();
                if (!buscando && numero == 0) listaGuardada = new ArrayList<>(contenido);
                boolean primera = !yaEntro;
                yaEntro = true;
                if (!mirandoFavoritos()) pintarActual(numero == 0 && primera);
            }

            @Override
            public void onFail(int code, String message) {
                // Una petición cancelada (otra búsqueda la ha sustituido) también llega
                // aquí: no es un error, y la nueva pintará lo suyo.
                if (miTurno != turno || isDestroyed()) return;
                cargando = false;
                if (numero > 0) {
                    UiFeedback.toastError(AnadirAlimentoActivity.this, code, message);
                    return;
                }
                if (mirandoFavoritos()) return;
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
        if (!yaEntro) {
            // Aún no ha llegado nada que enseñar.
            adapter.poner(new ArrayList<>(), false);
            estado.cargando();
            return;
        }
        List<GruposBusqueda.Elemento> lista = GruposBusqueda.de(resultados, buscando, recientesTodos);
        adapter.poner(lista, cascada);
        if (lista.isEmpty()) estado.vacio(R.string.anadir_vacio);
        else estado.oculto();
    }

    // ── Favoritos ───────────────────────────────────────────────────────────

    // El corazón de la pestaña, relleno cuando está elegida (tablero 8).
    private void pintarPestanas() {
        ((com.google.android.material.button.MaterialButton) findViewById(R.id.btnPestanaFavoritos))
                .setIconResource(enFavoritos ? R.drawable.ic_ms_favorite_fill : R.drawable.ic_ms_favorite);
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
                if (mirandoFavoritos()) pintarFavoritos();
            }

            @Override
            public void onFail(int code, String message) {
                if (miTurno != turnoFavoritos || isDestroyed()) return;
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
        List<GruposBusqueda.Elemento> lista = GruposBusqueda.favoritos(favoritos.getFavoritos(), favoritos.getPropuesta());
        adapter.poner(lista, false);
        if (favoritos.getFavoritos().isEmpty() && favoritos.getPropuesta() == null) estado.vacio(R.string.favoritos_vacio);
        else estado.oculto();
    }

    // Aceptar (el corazón) o rechazar (la equis): la tarjeta se va al tocar y, si la API
    // falla, vuelve y se dice. Aceptada, el alimento entra en la lista resaltado.
    private void responderPropuesta(@NonNull Favoritos.Propuesta p, boolean aceptar) {
        if (favoritos == null) return;
        Favoritos antes = favoritos;
        favoritos = new Favoritos(antes.getFavoritos(), null);
        pintarFavoritos();
        int id = p.getAlimento().getId();
        if (aceptar) Movimiento.vibrar(rv, Movimiento.Vibracion.LIGERA);
        Runnable bien = () -> {
            if (isDestroyed() || !aceptar) return;
            adapter.resaltar(id);
            cargarFavoritos();
        };
        java.util.function.BiConsumer<Integer, String> mal = (code, message) -> {
            if (isDestroyed()) return;
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
        actions.add(new UIHelper.MenuAction(R.drawable.ic_ms_edit, getString(R.string.alimento_editar),
                () -> mostrarDialogoEditarAlimento(alimento)));
        if (esAdmin && esPredefinido) {
            actions.add(new UIHelper.MenuAction(R.drawable.ic_ms_visibility_off, getString(R.string.comida_desactivar_alimento),
                    () -> UIHelper.mostrarDialogoConIcono(this,
                            getString(R.string.comida_desactivar_alimento),
                            getString(R.string.alimento_desactivar_confirmar),
                            R.drawable.ic_ms_visibility_off,
                            () -> desactivarAlimento(alimento))));
        }
        actions.add(new UIHelper.MenuAction(R.drawable.ic_ms_delete, getString(R.string.alimento_eliminar), true,
                () -> UIHelper.mostrarDialogoConIcono(this,
                        getString(R.string.alimento_eliminar),
                        getString(R.string.alimento_eliminar_confirmar),
                        R.drawable.ic_ms_delete,
                        () -> eliminarAlimento(alimento))));
        UIHelper.mostrarMenuAnclado(this, anchorView, alimento.getNombre(), actions);
    }

    // Editar abre la pantalla de crear en modo edición (decisión 17): la misma etiqueta,
    // la ración y la categoría, en vez del diálogo de antes. Al volver, se recarga.
    private void mostrarDialogoEditarAlimento(Alimento alimento) {
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
