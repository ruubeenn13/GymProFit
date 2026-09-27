package es.pmdm.gymprofit.ui.activities;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.PageDTO;
import es.pmdm.gymprofit.model.ejercicio.Ejercicio;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.EjercicioApi;
import es.pmdm.gymprofit.ui.adapters.EjercicioAdapter;
import es.pmdm.gymprofit.utils.EjercicioNavHelper;
import es.pmdm.gymprofit.utils.LoadingDialog;
import es.pmdm.gymprofit.utils.PaginacionScrollListener;
import es.pmdm.gymprofit.utils.PreferencesManager;
import es.pmdm.gymprofit.utils.UiFeedback;

// ============================================================
// EjerciciosActivity — la biblioteca de ejercicios (GP-105).
//
// Era la pestaña Ejercicios; ahora se abre desde «Biblioteca de ejercicios» de
// Entrenar, ya filtrada por una zona (EXTRA_GRUPO) o con el teclado abierto en el
// buscador (EXTRA_BUSCAR). Por dentro es lo de siempre: búsqueda y filtros en el
// SERVIDOR (/ejercicios/buscar paginado), con debounce y scroll infinito.
// ============================================================
public class EjerciciosActivity extends AppCompatActivity {

    /** Grupo muscular de la API con el que abrir ya filtrada (PECHO, ESPALDA…). */
    public static final String EXTRA_GRUPO = "grupo";
    /** Abrir con el foco y el teclado en el buscador. */
    public static final String EXTRA_BUSCAR = "buscar";

    private static final int TAM_PAGINA = 30;
    private static final long DEBOUNCE_MS = 400;

    // Aplica la escala de fuente global de la app.
    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(es.pmdm.gymprofit.utils.ScaleUtils.wrap(newBase));
    }

    private RecyclerView rvEjercicios;
    private EjercicioAdapter adapter;
    private TextInputEditText etBuscar;
    private ChipGroup chipGroupFiltros;
    private TextView tvEmpty;

    private String queryActual = "";
    private String grupoActual = null;
    private int paginaActual = 0;
    private boolean cargando = false;
    private boolean ultimaPagina = false;

    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private Runnable debounceRunnable;

    private final EjercicioApi api = ApiClient.service(EjercicioApi.class);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new PreferencesManager(this).applyTheme();
        setContentView(R.layout.activity_ejercicios);

        ((MaterialToolbar) findViewById(R.id.toolbar)).setNavigationOnClickListener(v -> finish());
        rvEjercicios = findViewById(R.id.rvEjercicios);
        etBuscar = findViewById(R.id.etBuscar);
        chipGroupFiltros = findViewById(R.id.chipGroupFiltros);
        tvEmpty = findViewById(R.id.tvEmpty);

        configurarRecyclerView();
        // El chip de la zona con la que se entra se marca antes de escuchar los chips,
        // para que la primera carga ya salga filtrada y no se pida dos veces.
        marcarGrupoInicial(getIntent().getStringExtra(EXTRA_GRUPO));
        configurarBuscador();
        configurarChips();
        cargarEjercicios();

        if (savedInstanceState == null && getIntent().getBooleanExtra(EXTRA_BUSCAR, false)) {
            etBuscar.requestFocus();
            etBuscar.post(() -> {
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (imm != null) imm.showSoftInput(etBuscar, InputMethodManager.SHOW_IMPLICIT);
            });
        }
    }

    // RecyclerView del catálogo con navegación al detalle y scroll infinito.
    private void configurarRecyclerView() {
        adapter = new EjercicioAdapter(new ArrayList<>(), e -> EjercicioNavHelper.abrir(this, e));
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rvEjercicios.setLayoutManager(layoutManager);
        rvEjercicios.setAdapter(adapter);
        rvEjercicios.addOnScrollListener(new PaginacionScrollListener(layoutManager) {
            @Override protected void cargarMas() { cargarPagina(paginaActual + 1); }
            @Override protected boolean isCargando() { return cargando; }
            @Override protected boolean esUltimaPagina() { return ultimaPagina; }
        });
    }

    // Reinicia la búsqueda desde la página 0 con los filtros actuales.
    private void cargarEjercicios() {
        paginaActual = 0;
        ultimaPagina = false;
        cargarPagina(0);
    }

    // Pide una página al servidor; la 0 reemplaza la lista (con spinner),
    // las siguientes se añaden al final en silencio (scroll infinito).
    private void cargarPagina(int pagina) {
        cargando = true;
        if (pagina == 0) LoadingDialog.show(this);
        api.buscar(queryActual.isEmpty() ? null : queryActual, grupoActual, null, pagina, TAM_PAGINA)
                .enqueue(new ApiCallback<PageDTO<Ejercicio>>() {
                    @Override
                    public void onOk(PageDTO<Ejercicio> resultado) {
                        cargando = false;
                        if (pagina == 0) LoadingDialog.hide(EjerciciosActivity.this);
                        if (resultado == null || isFinishing()) return;
                        paginaActual = resultado.getPage();
                        ultimaPagina = resultado.isLast();
                        List<Ejercicio> lista = resultado.getContent() != null
                                ? resultado.getContent() : new ArrayList<>();
                        if (pagina == 0) adapter.setEjercicios(lista);
                        else adapter.addEjercicios(lista);
                        actualizarEstadoVacio();
                    }

                    @Override
                    public void onFail(int code, String message) {
                        cargando = false;
                        if (pagina == 0) LoadingDialog.hide(EjerciciosActivity.this);
                        if (isFinishing()) return;
                        UiFeedback.toastError(EjerciciosActivity.this, code, message);
                        actualizarEstadoVacio();
                    }
                });
    }

    private void actualizarEstadoVacio() {
        boolean vacio = adapter.getItemCount() == 0;
        tvEmpty.setVisibility(vacio ? View.VISIBLE : View.GONE);
        rvEjercicios.setVisibility(vacio ? View.GONE : View.VISIBLE);
    }

    // Buscador con debounce: lanza la búsqueda en servidor al dejar de teclear.
    private void configurarBuscador() {
        etBuscar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                queryActual = s.toString().trim();
                if (debounceRunnable != null) debounceHandler.removeCallbacks(debounceRunnable);
                debounceRunnable = () -> cargarEjercicios();
                debounceHandler.postDelayed(debounceRunnable, DEBOUNCE_MS);
            }
        });
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

            cargarEjercicios();
        });
    }

    @Override
    protected void onDestroy() {
        if (debounceRunnable != null) debounceHandler.removeCallbacks(debounceRunnable);
        super.onDestroy();
    }
}
