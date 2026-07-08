package es.pmdm.gymprofit.ui.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.rutina.Rutina;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.RutinaApi;
import es.pmdm.gymprofit.ui.activities.CrearRutinaActivity;
import es.pmdm.gymprofit.ui.activities.DetalleRutinaActivity;
import es.pmdm.gymprofit.ui.activities.EditarRutinaActivity;
import es.pmdm.gymprofit.ui.activities.EditarRutinaAdminActivity;
import es.pmdm.gymprofit.ui.adapters.RutinaAdapter;
import es.pmdm.gymprofit.utils.LoadingDialog;
import es.pmdm.gymprofit.utils.UIHelper;
import es.pmdm.gymprofit.utils.UiFeedback;

// ============================================================
// RutinasFragment — pestaña de listado de rutinas (predefinidas + del usuario)
// con filtros por nivel y CRUD. Muestra un menú contextual por rutina (editar,
// activar/desactivar o eliminar) y navega al detalle de cada una.
// ============================================================
public class RutinasFragment extends BaseFragment {
    private RecyclerView rvRutinas;
    private RutinaAdapter adapter;
    private ChipGroup chipGroupNivel;
    private FloatingActionButton fabCrearRutina;
    private TextView tvEmpty;

    private final RutinaApi rutinaApi = ApiClient.service(RutinaApi.class);

    private ActivityResultLauncher<Intent> crearRutinaLauncher;
    private ActivityResultLauncher<Intent> detalleLauncher;
    private ActivityResultLauncher<Intent> editarLauncher;

    // Registra los launchers antes de que el fragment llegue a STARTED.
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        crearRutinaLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> { if (result.getResultCode() == Activity.RESULT_OK) cargarRutinas(); });
        detalleLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> { if (result.getResultCode() == Activity.RESULT_OK) cargarRutinas(); });
        editarLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> { if (result.getResultCode() == Activity.RESULT_OK) cargarRutinas(); });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_rutinas, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupMenuButton();
        inicializarVistas();
        configurarRecyclerView();
        configurarChips();
        configurarFab();
        cargarRutinas();
    }

    private void inicializarVistas() {
        rvRutinas = findViewById(R.id.rvRutinas);
        chipGroupNivel = findViewById(R.id.chipGroupNivel);
        fabCrearRutina = findViewById(R.id.fabCrearRutina);
        tvEmpty = findViewById(R.id.tvEmpty);
    }

    private void configurarRecyclerView() {
        adapter = new RutinaAdapter(new ArrayList<>(), this::abrirDetalle);
        adapter.setOnLongClickListener(this::mostrarMenuContextual);
        adapter.setUserContext(prefsManager.isAdmin(), prefsManager.getUsuarioId());
        rvRutinas.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvRutinas.setAdapter(adapter);
    }

    // Abre la pantalla de detalle de una rutina pasando sus datos por extras.
    private void abrirDetalle(Rutina rutina) {
        Intent intent = new Intent(requireContext(), DetalleRutinaActivity.class);
        intent.putExtra("rutinaId",     rutina.getId());
        intent.putExtra("nombre",       rutina.getNombre());
        intent.putExtra("descripcion",  rutina.getDescripcion());
        intent.putExtra("nivel",        rutina.getNivel());
        intent.putExtra("duracion",     rutina.getDuracionMinutos());
        intent.putExtra("calorias",     rutina.getCaloriasAproximadas());
        intent.putExtra("numEjercicios", rutina.getNumEjercicios());
        intent.putExtra("predefinida",  rutina.isPredefinida());
        intent.putExtra("usuarioId",    rutina.getUsuarioId());
        detalleLauncher.launch(intent);
    }

    // Carga las rutinas predefinidas y, si hay usuario, añade las propias.
    private void cargarRutinas() {
        final FragmentActivity act = requireActivity();
        int usuarioId = prefsManager.getUsuarioId();

        LoadingDialog.show(act);

        rutinaApi.getPredefinidas().enqueue(new ApiCallback<List<Rutina>>() {
            @Override
            public void onOk(List<Rutina> predefinidas) {
                List<Rutina> todas = new ArrayList<>(predefinidas != null ? predefinidas : new ArrayList<>());

                if (usuarioId != -1) {
                    rutinaApi.getDeUsuarioActivas(usuarioId).enqueue(new ApiCallback<List<Rutina>>() {
                        @Override
                        public void onOk(List<Rutina> propias) {
                            if (propias != null) todas.addAll(propias);
                            LoadingDialog.hide(act);
                            if (!isAdded()) return;
                            adapter.setRutinas(todas);
                            actualizarEstadoVacio();
                        }
                        @Override
                        public void onFail(int code, String message) {
                            LoadingDialog.hide(act);
                            if (!isAdded()) return;
                            adapter.setRutinas(todas);
                            actualizarEstadoVacio();
                        }
                    });
                } else {
                    LoadingDialog.hide(act);
                    if (!isAdded()) return;
                    adapter.setRutinas(todas);
                    actualizarEstadoVacio();
                }
            }

            @Override
            public void onFail(int code, String message) {
                LoadingDialog.hide(act);
                if (!isAdded()) return;
                UiFeedback.toastError(act, code, message);
                actualizarEstadoVacio();
            }
        });
    }

    // Muestra u oculta el mensaje "no hay nada aún" según los ítems del adapter.
    private void actualizarEstadoVacio() {
        boolean vacio = adapter.getItemCount() == 0;
        tvEmpty.setVisibility(vacio ? View.VISIBLE : View.GONE);
        rvRutinas.setVisibility(vacio ? View.GONE : View.VISIBLE);
    }

    // Menú contextual por rutina (editar, activar/desactivar o eliminar).
    private void mostrarMenuContextual(Rutina rutina, View anchorView) {
        if (!verificarAccesoRegistrado()) return;
        final FragmentActivity act = requireActivity();

        List<UIHelper.MenuAction> actions = new ArrayList<>();

        actions.add(new UIHelper.MenuAction(R.drawable.ic_edit, getString(R.string.rutinas_editar), () -> {
            if (rutina.isPredefinida()) {
                Intent intent = new Intent(requireContext(), EditarRutinaAdminActivity.class);
                intent.putExtra("id",                  rutina.getId());
                intent.putExtra("nombre",              rutina.getNombre());
                intent.putExtra("descripcion",         rutina.getDescripcion());
                intent.putExtra("nivel",               rutina.getNivel());
                intent.putExtra("duracionMinutos",     rutina.getDuracionMinutos());
                intent.putExtra("caloriasAproximadas", rutina.getCaloriasAproximadas());
                intent.putExtra("categoria",           rutina.getCategoria());
                intent.putExtra("diasSemana",          rutina.getDiasSemana());
                editarLauncher.launch(intent);
            } else {
                Intent intent = new Intent(requireContext(), EditarRutinaActivity.class);
                intent.putExtra("rutinaId",    rutina.getId());
                intent.putExtra("nombre",      rutina.getNombre());
                intent.putExtra("descripcion", rutina.getDescripcion());
                intent.putExtra("nivel",       rutina.getNivel());
                intent.putExtra("duracion",    rutina.getDuracionMinutos());
                editarLauncher.launch(intent);
            }
        }));

        if (rutina.isPredefinida()) {
            int iconToggle = rutina.isActiva() ? R.drawable.ic_visibility_off : R.drawable.ic_check;
            String labelToggle = rutina.isActiva()
                    ? getString(R.string.rutinas_desactivar)
                    : getString(R.string.rutinas_activar);
            actions.add(new UIHelper.MenuAction(iconToggle, labelToggle,
                    () -> toggleActivaRutinaPredefinida(rutina)));
        } else {
            actions.add(new UIHelper.MenuAction(R.drawable.ic_delete, getString(R.string.rutinas_eliminar), true,
                    () -> UIHelper.mostrarDialogoConIcono(act,
                            getString(R.string.rutinas_eliminar),
                            getString(R.string.rutinas_confirmar_eliminar),
                            R.drawable.ic_delete,
                            () -> eliminarRutina(rutina))));
        }

        UIHelper.mostrarMenuAnclado(act, anchorView, rutina.getNombre(), actions);
    }

    // Activa o desactiva una rutina predefinida (solo admin) y recarga el listado.
    private void toggleActivaRutinaPredefinida(Rutina rutina) {
        final FragmentActivity act = requireActivity();
        ApiCallback<Void> cb = new ApiCallback<Void>() {
            @Override
            public void onOk(Void body) {
                UIHelper.mostrarToastExito(act, getString(R.string.admin_exito_toggle_rutina));
                cargarRutinas();
            }
            @Override
            public void onFail(int code, String message) {
                UIHelper.mostrarToastError(act, getString(R.string.error_conexion));
            }
        };
        if (rutina.isActiva()) {
            rutinaApi.eliminar(rutina.getId()).enqueue(cb);
        } else {
            rutinaApi.activar(rutina.getId()).enqueue(cb);
        }
    }

    // Elimina una rutina propia del usuario y recarga el listado.
    private void eliminarRutina(Rutina rutina) {
        final FragmentActivity act = requireActivity();
        rutinaApi.eliminar(rutina.getId()).enqueue(new ApiCallback<Void>() {
            @Override
            public void onOk(Void body) {
                UIHelper.mostrarToastExito(act, getString(R.string.rutinas_eliminada_exito));
                cargarRutinas();
            }
            @Override
            public void onFail(int code, String message) {
                UIHelper.mostrarToastError(act, getString(R.string.error_conexion));
            }
        });
    }

    // Filtra el listado de rutinas por nivel según el chip seleccionado.
    private void configurarChips() {
        chipGroupNivel.setOnCheckedStateChangeListener(((chipGroup, list) -> {
            if (list.isEmpty()) return;

            int id = list.get(0);
            String nivel;

            if (id == R.id.chipTodos)              nivel = "Todos";
            else if (id == R.id.chipPrincipiante)  nivel = "Principiante";
            else if (id == R.id.chipIntermedio)    nivel = "Intermedio";
            else if (id == R.id.chipAvanzado)      nivel = "Avanzado";
            else                                   nivel = "Todos";

            adapter.filtrarPorNivel(nivel);
            actualizarEstadoVacio();
        }));
    }

    // Configura el FAB para crear una nueva rutina (requiere usuario registrado).
    private void configurarFab() {
        fabCrearRutina.setOnClickListener(v -> {
            if (!verificarAccesoRegistrado()) return;
            crearRutinaLauncher.launch(new Intent(requireContext(), CrearRutinaActivity.class));
        });
    }
}
