package es.pmdm.gymprofit.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.rutina.Rutina;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.RutinaApi;
import es.pmdm.gymprofit.ui.adapters.RutinaAdapter;
import es.pmdm.gymprofit.utils.LoadingDialog;
import es.pmdm.gymprofit.utils.UIHelper;
import es.pmdm.gymprofit.utils.UiFeedback;

// ============================================================
// RutinasActivity — listado de rutinas (predefinidas + del usuario) con filtros y CRUD.
// Muestra las rutinas predefinidas del sistema junto a las creadas por el usuario,
// permite filtrarlas por nivel, crear nuevas, editarlas/eliminarlas mediante un
// menú contextual y navegar al detalle de cada una. Incluye la barra de navegación inferior.
// ============================================================
public class RutinasActivity extends BaseActivity {

    private BottomNavigationView bottomNavigationView;
    private RecyclerView rvRutinas;
    private RutinaAdapter adapter;
    private ChipGroup chipGroupNivel;
    private FloatingActionButton fabCrearRutina;
    private TextView tvEmpty;

    // Interfaz Retrofit tipada del dominio rutinas (etapa 2)
    private final RutinaApi rutinaApi = ApiClient.service(RutinaApi.class);

    private ActivityResultLauncher<Intent> crearRutinaLauncher;
    private ActivityResultLauncher<Intent> detalleLauncher;
    private ActivityResultLauncher<Intent> editarLauncher;

    // Configura vistas, adapters, filtros, FAB, navegación y lanza la carga inicial de rutinas.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rutinas);

        setupMenuButton();
        registrarLauncher();
        inicializarVistas();
        configurarRecyclerView();
        configurarChips();
        configurarFab();
        configurarNavegacion();
        cargarRutinas();
    }

    // Registra los ActivityResultLauncher para crear, ver detalle y editar rutinas;
    // en cualquier caso de éxito recarga el listado.
    private void registrarLauncher() {
        crearRutinaLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) cargarRutinas();
                });
        detalleLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) cargarRutinas();
                });
        editarLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) cargarRutinas();
                });
    }

    // Vincula las vistas principales del layout.
    private void inicializarVistas() {
        rvRutinas = findViewById(R.id.rvRutinas);
        chipGroupNivel = findViewById(R.id.chipGroupNivel);
        fabCrearRutina = findViewById(R.id.fabCrearRutina);
        tvEmpty = findViewById(R.id.tvEmpty);
    }

    // Configura el RecyclerView con su adapter, listeners de click/long-click
    // y el contexto de usuario (admin/id) para el menú contextual.
    private void configurarRecyclerView() {
        adapter = new RutinaAdapter(new ArrayList<>(), this::abrirDetalle);
        adapter.setOnLongClickListener(this::mostrarMenuContextual);
        adapter.setUserContext(prefsManager.isAdmin(), prefsManager.getUsuarioId());
        rvRutinas.setLayoutManager(new LinearLayoutManager(this));
        rvRutinas.setAdapter(adapter);
    }

    // Abre la pantalla de detalle de una rutina pasando sus datos por extras.
    private void abrirDetalle(Rutina rutina) {
        Intent intent = new Intent(this, DetalleRutinaActivity.class);
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

    // Carga las rutinas predefinidas y, si hay usuario logueado, añade también
    // las rutinas propias del usuario, combinando ambas listas en el adapter.
    private void cargarRutinas() {
        int usuarioId = prefsManager.getUsuarioId();

        // Spinner de carga durante el cold-start / la petición (la pantalla estaría en blanco).
        LoadingDialog.show(this);

        // Rutinas predefinidas (ya deserializadas por Gson).
        rutinaApi.getPredefinidas().enqueue(new ApiCallback<List<Rutina>>() {
            @Override
            public void onOk(List<Rutina> predefinidas) {
                List<Rutina> todas = new ArrayList<>(predefinidas != null ? predefinidas : new ArrayList<>());

                if (usuarioId != -1) {
                    // Añade las rutinas propias del usuario a las predefinidas.
                    rutinaApi.getDeUsuarioActivas(usuarioId).enqueue(new ApiCallback<List<Rutina>>() {
                        @Override
                        public void onOk(List<Rutina> propias) {
                            if (propias != null) todas.addAll(propias);
                            LoadingDialog.hide(RutinasActivity.this);
                            adapter.setRutinas(todas);
                            actualizarEstadoVacio();
                        }
                        @Override
                        public void onFail(int code, String message) {
                            // Fallo solo en las propias: se muestran igualmente las predefinidas.
                            LoadingDialog.hide(RutinasActivity.this);
                            adapter.setRutinas(todas);
                            actualizarEstadoVacio();
                        }
                    });
                } else {
                    LoadingDialog.hide(RutinasActivity.this);
                    adapter.setRutinas(todas);
                    actualizarEstadoVacio();
                }
            }

            @Override
            public void onFail(int code, String message) {
                // Fallo total (incluye cold-start): oculta spinner, avisa y muestra estado vacío.
                LoadingDialog.hide(RutinasActivity.this);
                UiFeedback.toastError(RutinasActivity.this, code, message);
                actualizarEstadoVacio();
            }
        });
    }

    // Muestra u oculta el mensaje "no hay nada aún" según los ítems visibles del adapter.
    private void actualizarEstadoVacio() {
        boolean vacio = adapter.getItemCount() == 0;
        tvEmpty.setVisibility(vacio ? View.VISIBLE : View.GONE);
        rvRutinas.setVisibility(vacio ? View.GONE : View.VISIBLE);
    }

    // Construye y muestra el menú contextual (editar, activar/desactivar o eliminar)
    // según si la rutina es predefinida (solo admin) o propia del usuario.
    private void mostrarMenuContextual(Rutina rutina, View anchorView) {
        if (!verificarAccesoRegistrado()) return;

        List<UIHelper.MenuAction> actions = new ArrayList<>();

        actions.add(new UIHelper.MenuAction(R.drawable.ic_edit, getString(R.string.rutinas_editar), () -> {
            if (rutina.isPredefinida()) {
                Intent intent = new Intent(this, EditarRutinaAdminActivity.class);
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
                Intent intent = new Intent(this, EditarRutinaActivity.class);
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
                    () -> UIHelper.mostrarDialogoConIcono(this,
                            getString(R.string.rutinas_eliminar),
                            getString(R.string.rutinas_confirmar_eliminar),
                            R.drawable.ic_delete,
                            () -> eliminarRutina(rutina))));
        }

        UIHelper.mostrarMenuAnclado(this, anchorView, rutina.getNombre(), actions);
    }

    // Activa o desactiva una rutina predefinida (solo admin) y recarga el listado.
    private void toggleActivaRutinaPredefinida(Rutina rutina) {
        ApiCallback<Void> cb = new ApiCallback<Void>() {
            @Override
            public void onOk(Void body) {
                UIHelper.mostrarToastExito(RutinasActivity.this,
                        getString(R.string.admin_exito_toggle_rutina));
                cargarRutinas();
            }
            @Override
            public void onFail(int code, String message) {
                UIHelper.mostrarToastError(
                        RutinasActivity.this, getString(R.string.error_conexion));
            }
        };
        // Desactivar = DELETE rutinas/{id}; activar = PUT rutinas/{id}/activar.
        if (rutina.isActiva()) {
            rutinaApi.eliminar(rutina.getId()).enqueue(cb);
        } else {
            rutinaApi.activar(rutina.getId()).enqueue(cb);
        }
    }

    // Elimina una rutina propia del usuario y recarga el listado.
    private void eliminarRutina(Rutina rutina) {
        rutinaApi.eliminar(rutina.getId()).enqueue(new ApiCallback<Void>() {
            @Override
            public void onOk(Void body) {
                UIHelper.mostrarToastExito(RutinasActivity.this,
                        getString(R.string.rutinas_eliminada_exito));
                cargarRutinas();
            }
            @Override
            public void onFail(int code, String message) {
                UIHelper.mostrarToastError(
                        RutinasActivity.this, getString(R.string.error_conexion));
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
            crearRutinaLauncher.launch(new Intent(this, CrearRutinaActivity.class));
        });
    }

    // Configura la barra de navegación inferior y la redirección entre pantallas principales.
    private void configurarNavegacion() {
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setSelectedItemId(R.id.nav_rutinas);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                startActivity(new Intent(this, HomeActivity.class));
                overridePendingTransition(0, 0);
                finish();
                overridePendingTransition(0, 0);
                return true;
            } else if (itemId == R.id.nav_rutinas) {
                return true;
            } else if (itemId == R.id.nav_ejercicios) {
                startActivity(new Intent(this, EjerciciosActivity.class));
                overridePendingTransition(0, 0);
                finish();
                overridePendingTransition(0, 0);
                return true;
            } else if (itemId == R.id.nav_nutricion) {
                startActivity(new Intent(this, NutricionActivity.class));
                overridePendingTransition(0, 0);
                finish();
                overridePendingTransition(0, 0);
                return true;
            } else if (itemId == R.id.nav_perfil) {
                startActivity(new Intent(this, PerfilActivity.class));
                overridePendingTransition(0, 0);
                finish();
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });
    }

}
