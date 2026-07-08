package es.pmdm.gymprofit.ui.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
// EjerciciosFragment — pestaña del catálogo de ejercicios.
// Lista los ejercicios activos con buscador por texto y filtro por grupo
// muscular mediante chips. Búsqueda y filtros se resuelven en el SERVIDOR
// (/ejercicios/buscar paginado) con debounce y scroll infinito.
// ============================================================
public class EjerciciosFragment extends BaseFragment {

    private static final int TAM_PAGINA = 30;
    private static final long DEBOUNCE_MS = 400;
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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_ejercicios, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupMenuButton();
        inicializarVistas();
        configurarRecyclerView();
        configurarBuscador();
        configurarChips();
        cargarEjercicios();
    }

    private void inicializarVistas() {
        rvEjercicios = findViewById(R.id.rvEjercicios);
        etBuscar = findViewById(R.id.etBuscar);
        chipGroupFiltros = findViewById(R.id.chipGroupFiltros);
        tvEmpty = findViewById(R.id.tvEmpty);
    }

    // RecyclerView del catálogo con navegación al detalle y scroll infinito.
    private void configurarRecyclerView() {
        adapter = new EjercicioAdapter(new ArrayList<>(),
                e -> EjercicioNavHelper.abrir(requireContext(), e));
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
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
        final FragmentActivity act = requireActivity();
        cargando = true;
        if (pagina == 0) LoadingDialog.show(act);
        api.buscar(queryActual.isEmpty() ? null : queryActual, grupoActual, null, pagina, TAM_PAGINA)
                .enqueue(new ApiCallback<PageDTO<Ejercicio>>() {
                    @Override
                    public void onOk(PageDTO<Ejercicio> resultado) {
                        cargando = false;
                        if (pagina == 0) LoadingDialog.hide(act);
                        if (resultado == null || !isAdded()) return;
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
                        if (pagina == 0) LoadingDialog.hide(act);
                        if (!isAdded()) return;
                        UiFeedback.toastError(act, code, message);
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

    // Cancela el debounce pendiente al destruir la vista.
    @Override
    public void onDestroyView() {
        if (debounceRunnable != null) debounceHandler.removeCallbacks(debounceRunnable);
        super.onDestroyView();
    }
}
