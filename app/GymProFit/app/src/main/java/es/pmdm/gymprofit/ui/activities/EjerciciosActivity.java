package es.pmdm.gymprofit.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
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
import es.pmdm.gymprofit.utils.UiFeedback;
// ============================================================
// EjerciciosActivity — pantalla del catálogo de ejercicios.
// Lista los ejercicios activos con buscador por texto y filtro por grupo
// muscular mediante chips. Búsqueda y filtros se resuelven en el SERVIDOR
// (/ejercicios/buscar paginado) con debounce y scroll infinito, para
// soportar catálogos grandes sin descargarlos enteros.
// ============================================================
public class EjerciciosActivity extends BaseActivity {

    // Tamaño de página del catálogo y retardo del debounce del buscador
    private static final int TAM_PAGINA = 30;
    private static final long DEBOUNCE_MS = 400;

    private BottomNavigationView bottomNavigationView;
    private RecyclerView rvEjercicios;
    private EjercicioAdapter adapter;
    private TextInputEditText etBuscar;
    private ChipGroup chipGroupFiltros;
    private TextView tvEmpty;

    // Estado de la búsqueda paginada en servidor
    private String queryActual = "";
    private String grupoActual = null;      // nombre del enum (PECHO...) o null = todos
    private int paginaActual = 0;
    private boolean cargando = false;
    private boolean ultimaPagina = false;

    // Debounce del buscador: pospone la petición hasta que el usuario deja de teclear
    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private Runnable debounceRunnable;

    // Interfaz Retrofit tipada del dominio ejercicios (cacheada por ApiClient).
    private final EjercicioApi api = ApiClient.service(EjercicioApi.class);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ejercicios);

        setupMenuButton();
        inicializarVistas();
        configurarRecyclerView();
        configurarBuscador();
        configurarChips();
        configurarNavegacion();
        cargarEjercicios();
    }

    // Enlaza las referencias a las vistas del layout.
    private void inicializarVistas() {
        rvEjercicios = findViewById(R.id.rvEjercicios);
        etBuscar = findViewById(R.id.etBuscar);
        chipGroupFiltros = findViewById(R.id.chipGroupFiltros);
        tvEmpty = findViewById(R.id.tvEmpty);
    }

    // Configura el RecyclerView del catálogo con navegación al detalle al pulsar
    // y scroll infinito que pide la siguiente página al acercarse al final.
    private void configurarRecyclerView() {
        adapter = new EjercicioAdapter(new ArrayList<>(),
                e -> EjercicioNavHelper.abrir(this, e));
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
                        if (resultado == null) return;
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
                        UiFeedback.toastError(EjerciciosActivity.this, code, message);
                        actualizarEstadoVacio();
                    }
                });
    }

    // Muestra u oculta el mensaje "no hay nada aún" según los ítems visibles del adapter.
    private void actualizarEstadoVacio() {
        boolean vacio = adapter.getItemCount() == 0;
        tvEmpty.setVisibility(vacio ? View.VISIBLE : View.GONE);
        rvEjercicios.setVisibility(vacio ? View.GONE : View.VISIBLE);
    }

    // Buscador con debounce: espera a que el usuario deje de teclear y lanza
    // la búsqueda en el servidor (reiniciando la paginación).
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

    // Cancela el debounce pendiente al destruir la Activity (evita un
    // callback sobre una pantalla muerta).
    @Override
    protected void onDestroy() {
        if (debounceRunnable != null) debounceHandler.removeCallbacks(debounceRunnable);
        super.onDestroy();
    }

    // Configura la barra de navegación inferior para moverse entre las
    // secciones principales de la app sin animación de transición.
    private void configurarNavegacion() {
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setSelectedItemId(R.id.nav_ejercicios);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                startActivity(new Intent(this, HomeActivity.class));
                es.pmdm.gymprofit.utils.AnimUtils.sinAnimacion(this);
                finish();
                es.pmdm.gymprofit.utils.AnimUtils.sinAnimacion(this);
                return true;
            } else if (itemId == R.id.nav_rutinas) {
                startActivity(new Intent(this, RutinasActivity.class));
                es.pmdm.gymprofit.utils.AnimUtils.sinAnimacion(this);
                finish();
                es.pmdm.gymprofit.utils.AnimUtils.sinAnimacion(this);
                return true;
            } else if (itemId == R.id.nav_ejercicios) {
                return true;
            } else if (itemId == R.id.nav_nutricion) {
                startActivity(new Intent(this, NutricionActivity.class));
                es.pmdm.gymprofit.utils.AnimUtils.sinAnimacion(this);
                finish();
                es.pmdm.gymprofit.utils.AnimUtils.sinAnimacion(this);
                return true;
            } else if (itemId == R.id.nav_perfil) {
                startActivity(new Intent(this, PerfilActivity.class));
                es.pmdm.gymprofit.utils.AnimUtils.sinAnimacion(this);
                finish();
                es.pmdm.gymprofit.utils.AnimUtils.sinAnimacion(this);
                return true;
            }
            return false;
        });
    }

}
