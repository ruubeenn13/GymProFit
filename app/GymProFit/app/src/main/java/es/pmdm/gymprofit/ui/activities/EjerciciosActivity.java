package es.pmdm.gymprofit.ui.activities;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.PageDTO;
import es.pmdm.gymprofit.model.ejercicio.Ejercicio;
import es.pmdm.gymprofit.model.ejercicio.EjercicioMasHecho;
import es.pmdm.gymprofit.model.record.Record;
import es.pmdm.gymprofit.model.record.Records;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.EjercicioApi;
import es.pmdm.gymprofit.network.RecordApi;
import es.pmdm.gymprofit.network.SesionApi;
import es.pmdm.gymprofit.ui.adapters.BibliotecaCabeceraAdapter;
import es.pmdm.gymprofit.ui.adapters.EjercicioAdapter;
import es.pmdm.gymprofit.utils.EjercicioNavHelper;
import es.pmdm.gymprofit.utils.LoadingDialog;
import es.pmdm.gymprofit.utils.Marcas;
import es.pmdm.gymprofit.utils.PaginacionScrollListener;
import es.pmdm.gymprofit.utils.PreferencesManager;
import es.pmdm.gymprofit.utils.UiFeedback;

// ============================================================
// EjerciciosActivity — la biblioteca de ejercicios (GP-105, rehecha en GP-216, tablero 29).
//
// Se abre desde Entrenar, ya filtrada por una zona (EXTRA_GRUPO) o con el teclado abierto en
// el buscador (EXTRA_BUSCAR). Arriba, fijos, el buscador y los chips de grupo; debajo, una sola
// lista: «Los que más haces» (con la marca de /records, solo sin filtros), cuántos hay y las
// filas. Búsqueda y filtros en el SERVIDOR (/ejercicios/buscar paginado, ya ordenado por
// nombre), con debounce y scroll infinito.
// ============================================================
public class EjerciciosActivity extends AppCompatActivity {

    /** Grupo muscular de la API con el que abrir ya filtrada (PECHO, ESPALDA…). */
    public static final String EXTRA_GRUPO = "grupo";
    /** Abrir con el foco y el teclado en el buscador. */
    public static final String EXTRA_BUSCAR = "buscar";

    private static final int TAM_PAGINA = 30;
    private static final int MAS_HECHOS = 8;
    private static final long DEBOUNCE_MS = 400;

    // Aplica la escala de fuente global de la app.
    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(es.pmdm.gymprofit.utils.ScaleUtils.wrap(newBase));
    }

    private RecyclerView rvEjercicios;
    private EjercicioAdapter adapter;
    private BibliotecaCabeceraAdapter cabecera;
    private EditText etBuscar;
    private View btnLimpiar;
    private ChipGroup chipGroupFiltros;
    private TextView tvEmpty;

    private String queryActual = "";
    private String grupoActual = null;
    private int paginaActual = 0;
    private boolean cargando = false;
    private boolean ultimaPagina = false;
    // Cada búsqueda nueva invalida las respuestas de las anteriores (dos tecleos seguidos).
    private int generacion = 0;

    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private Runnable debounceRunnable;

    private final EjercicioApi api = ApiClient.service(EjercicioApi.class);
    private final SesionApi sesionApi = ApiClient.service(SesionApi.class);
    private final RecordApi recordApi = ApiClient.service(RecordApi.class);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new PreferencesManager(this).applyTheme();
        setContentView(R.layout.activity_ejercicios);

        ((MaterialToolbar) findViewById(R.id.toolbar)).setNavigationOnClickListener(v -> finish());
        rvEjercicios = findViewById(R.id.rvEjercicios);
        etBuscar = findViewById(R.id.etBuscar);
        btnLimpiar = findViewById(R.id.btnLimpiarBusqueda);
        chipGroupFiltros = findViewById(R.id.chipGroupFiltros);
        tvEmpty = findViewById(R.id.tvEmpty);

        configurarRecyclerView();
        // El chip de la zona con la que se entra se marca antes de escuchar los chips,
        // para que la primera carga ya salga filtrada y no se pida dos veces.
        marcarGrupoInicial(getIntent().getStringExtra(EXTRA_GRUPO));
        configurarBuscador();
        configurarChips();
        cabecera.setFiltrando(grupoActual != null);
        cargarEjercicios();
        cargarMasHaces();

        if (savedInstanceState == null && getIntent().getBooleanExtra(EXTRA_BUSCAR, false)) {
            etBuscar.requestFocus();
            etBuscar.post(() -> {
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (imm != null) imm.showSoftInput(etBuscar, InputMethodManager.SHOW_IMPLICIT);
            });
        }
    }

    // La cabecera y las filas en un solo RecyclerView, con navegación a la ficha y scroll infinito.
    private void configurarRecyclerView() {
        adapter = new EjercicioAdapter(new ArrayList<>(), (EjercicioAdapter.OnClickFilaListener)
                (e, fila) -> EjercicioNavHelper.abrir(this, fila, e));
        cabecera = new BibliotecaCabeceraAdapter((e, tarjeta) -> EjercicioNavHelper.abrir(this, tarjeta, e));
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rvEjercicios.setLayoutManager(layoutManager);
        rvEjercicios.setAdapter(new ConcatAdapter(cabecera, adapter));
        rvEjercicios.addOnScrollListener(new PaginacionScrollListener(layoutManager) {
            @Override protected void cargarMas() { cargarPagina(paginaActual + 1); }
            @Override protected boolean isCargando() { return cargando; }
            @Override protected boolean esUltimaPagina() { return ultimaPagina; }
        });
    }

    // Reinicia la búsqueda desde la página 0 con los filtros actuales.
    private void cargarEjercicios() {
        generacion++;
        paginaActual = 0;
        ultimaPagina = false;
        cargarPagina(0);
    }

    // Pide una página al servidor; la 0 reemplaza la lista (con spinner),
    // las siguientes se añaden al final en silencio (scroll infinito).
    private void cargarPagina(int pagina) {
        cargando = true;
        final int miGeneracion = generacion;
        if (pagina == 0) LoadingDialog.show(this);
        api.buscar(queryActual.isEmpty() ? null : queryActual, grupoActual, null, pagina, TAM_PAGINA)
                .enqueue(new ApiCallback<PageDTO<Ejercicio>>() {
                    @Override
                    public void onOk(PageDTO<Ejercicio> resultado) {
                        if (pagina == 0) LoadingDialog.hide(EjerciciosActivity.this);
                        if (miGeneracion != generacion) return;
                        cargando = false;
                        if (resultado == null || (isFinishing() || isDestroyed())) return;
                        paginaActual = resultado.getPage();
                        ultimaPagina = resultado.isLast();
                        List<Ejercicio> lista = resultado.getContent() != null
                                ? resultado.getContent() : new ArrayList<>();
                        if (pagina == 0) adapter.setEjercicios(lista);
                        else adapter.addEjercicios(lista);
                        cabecera.setCuantos(resultado.getTotalElements());
                        actualizarEstadoVacio();
                    }

                    @Override
                    public void onFail(int code, String message) {
                        if (pagina == 0) LoadingDialog.hide(EjerciciosActivity.this);
                        if (miGeneracion != generacion) return;
                        cargando = false;
                        if ((isFinishing() || isDestroyed())) return;
                        UiFeedback.toastError(EjerciciosActivity.this, code, message);
                        actualizarEstadoVacio();
                    }
                });
    }

    private void actualizarEstadoVacio() {
        tvEmpty.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }

    // «Los que más haces»: los ids que da la API, cada ejercicio del catálogo y la marca de
    // /records. Es un atajo: si algo falla la lista de abajo funciona igual, así que no se avisa.
    private void cargarMasHaces() {
        sesionApi.masHechos(MAS_HECHOS).enqueue(new ApiCallback<List<EjercicioMasHecho>>() {
            @Override public void onOk(List<EjercicioMasHecho> lista) {
                if ((isFinishing() || isDestroyed()) || lista == null || lista.isEmpty()) return;
                completarMasHaces(lista);
            }
            @Override public void onFail(int code, String message) {
                // Se ignora a propósito: sin el atajo la Biblioteca se usa igual.
            }
        });
    }

    private void completarMasHaces(List<EjercicioMasHecho> ids) {
        final Map<Integer, Ejercicio> ejercicios = new HashMap<>();
        final Map<Integer, Record> marcas = new HashMap<>();
        // Cada ejercicio, más /records: se pinta cuando han vuelto todas las respuestas.
        final AtomicInteger pendientes = new AtomicInteger(ids.size() + 1);
        final Runnable unaMenos = () -> {
            if (pendientes.decrementAndGet() == 0) pintarMasHaces(ids, ejercicios, marcas);
        };
        recordApi.getRecords(null).enqueue(new ApiCallback<Records>() {
            @Override public void onOk(Records r) {
                if (r != null) {
                    for (Record x : r.getRecords()) {
                        if (!marcas.containsKey(x.getEjercicioId())) marcas.put(x.getEjercicioId(), x);
                    }
                }
                unaMenos.run();
            }
            @Override public void onFail(int code, String message) {
                // Sin las marcas, la tarjeta dice las veces que se ha hecho.
                unaMenos.run();
            }
        });
        for (EjercicioMasHecho m : ids) {
            api.getPorId(m.getEjercicioId()).enqueue(new ApiCallback<Ejercicio>() {
                @Override public void onOk(Ejercicio e) {
                    if (e != null) ejercicios.put(m.getEjercicioId(), e);
                    unaMenos.run();
                }
                @Override public void onFail(int code, String message) {
                    // Ese ejercicio se salta; los demás se enseñan.
                    unaMenos.run();
                }
            });
        }
    }

    private void pintarMasHaces(List<EjercicioMasHecho> ids, Map<Integer, Ejercicio> ejercicios,
                                Map<Integer, Record> marcas) {
        if ((isFinishing() || isDestroyed())) return;
        List<BibliotecaCabeceraAdapter.MasHaces> tarjetas = new ArrayList<>();
        for (EjercicioMasHecho m : ids) {
            Ejercicio e = ejercicios.get(m.getEjercicioId());
            if (e == null) continue;
            Record r = marcas.get(m.getEjercicioId());
            tarjetas.add(new BibliotecaCabeceraAdapter.MasHaces(e,
                    r != null ? Marcas.texto(this, r) : null, m.getVeces()));
        }
        cabecera.setMasHaces(tarjetas);
    }

    // Buscador con debounce: lanza la búsqueda en servidor al dejar de teclear.
    private void configurarBuscador() {
        etBuscar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                queryActual = s.toString().trim();
                btnLimpiar.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                cabecera.setBuscando(!queryActual.isEmpty());
                cabecera.setFiltrando(!queryActual.isEmpty() || grupoActual != null);
                if (debounceRunnable != null) debounceHandler.removeCallbacks(debounceRunnable);
                debounceRunnable = () -> cargarEjercicios();
                debounceHandler.postDelayed(debounceRunnable, DEBOUNCE_MS);
            }
        });
        etBuscar.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId != EditorInfo.IME_ACTION_SEARCH) return false;
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            return true;
        });
        btnLimpiar.setOnClickListener(v -> etBuscar.setText(""));
    }

    // Marca el chip del grupo con el que se abre la pantalla.
    private void marcarGrupoInicial(@Nullable String grupo) {
        if (grupo == null) return;
        int id;
        switch (grupo) {
            case "PECHO":   id = R.id.chipPecho; break;
            case "ESPALDA": id = R.id.chipEspalda; break;
            case "PIERNAS": id = R.id.chipPiernas; break;
            case "BRAZOS":  id = R.id.chipBrazos; break;
            case "HOMBROS": id = R.id.chipHombros; break;
            case "ABDOMEN": id = R.id.chipCore; break;
            default: return;
        }
        chipGroupFiltros.check(id);
        grupoActual = grupo;
        View chip = findViewById(id);
        chip.post(() -> chip.getParent().requestChildFocus(chip, chip));
    }

    // Chips de grupo muscular → filtro en servidor (nombre del enum de la API).
    private void configurarChips() {
        chipGroupFiltros.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;

            int id = checkedIds.get(0);

            if (id == R.id.chipPecho)        grupoActual = "PECHO";
            else if (id == R.id.chipEspalda) grupoActual = "ESPALDA";
            else if (id == R.id.chipPiernas) grupoActual = "PIERNAS";
            else if (id == R.id.chipBrazos)  grupoActual = "BRAZOS";
            else if (id == R.id.chipHombros) grupoActual = "HOMBROS";
            else if (id == R.id.chipCore)    grupoActual = "ABDOMEN";
            else                             grupoActual = null; // Todos

            cabecera.setFiltrando(!queryActual.isEmpty() || grupoActual != null);
            cargarEjercicios();
        });
    }

    @Override
    protected void onDestroy() {
        if (debounceRunnable != null) debounceHandler.removeCallbacks(debounceRunnable);
        super.onDestroy();
    }
}
