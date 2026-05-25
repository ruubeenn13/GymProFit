package es.pmdm.gymprofit.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONException;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.rutina.Rutina;
import es.pmdm.gymprofit.network.API;
import es.pmdm.gymprofit.network.UtilJSONParser;
import es.pmdm.gymprofit.network.UtilREST;
import es.pmdm.gymprofit.ui.adapters.RutinaAdapter;
import es.pmdm.gymprofit.utils.UIHelper;

public class RutinasActivity extends BaseActivity {

    private BottomNavigationView bottomNavigationView;
    private RecyclerView rvRutinas;
    private RutinaAdapter adapter;
    private ChipGroup chipGroupNivel;
    private FloatingActionButton fabCrearRutina;

    private ActivityResultLauncher<Intent> crearRutinaLauncher;
    private ActivityResultLauncher<Intent> detalleLauncher;
    private ActivityResultLauncher<Intent> editarLauncher;

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

    private void inicializarVistas() {
        rvRutinas = findViewById(R.id.rvRutinas);
        chipGroupNivel = findViewById(R.id.chipGroupNivel);
        fabCrearRutina = findViewById(R.id.fabCrearRutina);
    }

    private void configurarRecyclerView() {
        adapter = new RutinaAdapter(new ArrayList<>(), this::abrirDetalle);
        adapter.setOnLongClickListener(this::mostrarMenuContextual);
        adapter.setUserContext(prefsManager.isAdmin(), prefsManager.getUsuarioId());
        rvRutinas.setLayoutManager(new LinearLayoutManager(this));
        rvRutinas.setAdapter(adapter);
    }

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

    private void cargarRutinas() {
        int usuarioId = prefsManager.getUsuarioId();

        API.getRutinasPredefinidas(new UtilREST.OnResponseListener() {
            @Override
            public void onSuccess(String response, int statusCode) {
                try {
                    List<Rutina> predefinidas = UtilJSONParser.parseRutinaList(response);
                    List<Rutina> todas = new ArrayList<>(predefinidas);

                    if (usuarioId != -1) {
                        API.getRutinasDeUsuario(usuarioId, new UtilREST.OnResponseListener() {
                            @Override
                            public void onSuccess(String response2, int statusCode2) {
                                try {
                                    todas.addAll(UtilJSONParser.parseRutinaList(response2));
                                } catch (JSONException e) {
                                    android.util.Log.e("RutinasActivity", "Error parseando rutinas usuario", e);
                                }
                                adapter.setRutinas(todas);
                            }

                            @Override
                            public void onError(String message, int statusCode2) {
                                adapter.setRutinas(todas);
                            }
                        });
                    } else {
                        adapter.setRutinas(todas);
                    }
                } catch (JSONException e) {
                    android.util.Log.e("RutinasActivity", "Error parseando rutinas predefinidas", e);
                }
            }

            @Override
            public void onError(String message, int statusCode) {
                android.util.Log.e("RutinasActivity", "Error cargando rutinas: " + message);
            }
        });
    }

    private void mostrarMenuContextual(Rutina rutina, View anchorView) {
        PopupMenu popup = new PopupMenu(this, anchorView);
        popup.inflate(R.menu.menu_rutina_context);

        MenuItem itemDesactivar = popup.getMenu().findItem(R.id.menuDesactivar);
        MenuItem itemEliminar   = popup.getMenu().findItem(R.id.menuEliminar);

        if (rutina.isPredefinida()) {
            itemEliminar.setVisible(false);
            if (rutina.isActiva()) {
                itemDesactivar.setTitle(getString(R.string.rutinas_desactivar));
                itemDesactivar.setIcon(R.drawable.ic_visibility_off);
            } else {
                itemDesactivar.setTitle(getString(R.string.rutinas_activar));
                itemDesactivar.setIcon(R.drawable.ic_check);
            }
        } else {
            itemDesactivar.setVisible(false);
        }

        try {
            Field field = popup.getClass().getDeclaredField("mPopup");
            field.setAccessible(true);
            Object helper = field.get(popup);
            Method method = helper.getClass().getDeclaredMethod("setForceShowIcon", boolean.class);
            method.invoke(helper, true);
        } catch (Exception ignored) {}

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menuEditar) {
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
                return true;
            } else if (id == R.id.menuDesactivar) {
                toggleActivaRutinaPredefinida(rutina);
                return true;
            } else if (id == R.id.menuEliminar) {
                UIHelper.mostrarDialogoConIcono(this,
                        getString(R.string.rutinas_eliminar),
                        getString(R.string.rutinas_confirmar_eliminar),
                        R.drawable.ic_delete,
                        () -> eliminarRutina(rutina));
                return true;
            }
            return false;
        });

        popup.show();
    }

    private void toggleActivaRutinaPredefinida(Rutina rutina) {
        UtilREST.OnResponseListener cb = new UtilREST.OnResponseListener() {
            @Override
            public void onSuccess(String response, int statusCode) {
                runOnUiThread(() -> {
                    UIHelper.mostrarToastExito(RutinasActivity.this,
                            getString(R.string.admin_exito_toggle_rutina));
                    cargarRutinas();
                });
            }
            @Override
            public void onError(String message, int statusCode) {
                runOnUiThread(() -> UIHelper.mostrarToastError(
                        RutinasActivity.this, getString(R.string.error_conexion)));
            }
        };
        if (rutina.isActiva()) {
            API.adminDesactivarRutina(rutina.getId(), cb);
        } else {
            API.adminActivarRutina(rutina.getId(), cb);
        }
    }

    private void eliminarRutina(Rutina rutina) {
        API.eliminarRutina(rutina.getId(), new UtilREST.OnResponseListener() {
            @Override
            public void onSuccess(String response, int statusCode) {
                runOnUiThread(() -> {
                    UIHelper.mostrarToastExito(RutinasActivity.this,
                            getString(R.string.rutinas_eliminada_exito));
                    cargarRutinas();
                });
            }

            @Override
            public void onError(String message, int statusCode) {
                runOnUiThread(() -> UIHelper.mostrarToastError(
                        RutinasActivity.this, getString(R.string.error_conexion)));
            }
        });
    }

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
        }));
    }

    private void configurarFab() {
        fabCrearRutina.setOnClickListener(v ->
                crearRutinaLauncher.launch(new Intent(this, CrearRutinaActivity.class)));
    }

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
