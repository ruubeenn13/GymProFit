package es.pmdm.gymprofit.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.rutina.Rutina;
import es.pmdm.gymprofit.network.API;
import es.pmdm.gymprofit.network.UtilJSONParser;
import es.pmdm.gymprofit.network.UtilREST;
import es.pmdm.gymprofit.ui.adapters.AdminRutinaAdapter;

// ============================================================
// AdminRutinasActivity — pantalla de administración de rutinas predefinidas
// Permite al rol ADMIN listar, filtrar por estado/nivel/nombre, activar/
// desactivar y editar rutinas predefinidas del catálogo de GymProFit.
// ============================================================
public class AdminRutinasActivity extends BaseActivity {

    private RecyclerView rv;
    private AdminRutinaAdapter adapter;
    // Lista de rutinas actualmente mostrada en el RecyclerView
    private final List<Rutina> lista = new ArrayList<>();

    // Filtro por estado activa/inactiva (null = sin filtrar)
    private Boolean filtroActiva = null;
    // Filtro por nivel de dificultad (null = sin filtrar)
    private String filtroNivel = null;
    // Filtro por nombre introducido en el buscador (null = sin filtrar)
    private String filtroNombre = null;

    // Configura RecyclerView, chips de filtro, buscador y carga inicial de datos
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_rutinas);

        setupMenuButton();
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        rv = findViewById(R.id.rvRutinas);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminRutinaAdapter(lista, new AdminRutinaAdapter.OnAccionListener() {
            @Override
            public void onToggleActiva(Rutina r, int pos) {
                toggleActiva(r, pos);
            }
            @Override
            public void onEditar(Rutina r, int pos) {
                mostrarDialogoEditar(r, pos);
            }
        });
        rv.setAdapter(adapter);

        configurarChips();
        configurarBusqueda();
        cargar();
    }

    // Recarga la lista al volver a la pantalla (por si hubo cambios en la edición)
    @Override
    protected void onResume() {
        super.onResume();
        cargar();
    }

    // Configura los chips de filtro por estado y por nivel; ambos son excluyentes entre sí
    private void configurarChips() {
        ChipGroup cg = findViewById(R.id.chipGroupFiltros);
        cg.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if (id == R.id.chipTodos) {
                filtroActiva = null;
                filtroNivel = null;
            } else if (id == R.id.chipActivas) {
                filtroActiva = true;
                filtroNivel = null;
            } else if (id == R.id.chipInactivas) {
                filtroActiva = false;
                filtroNivel = null;
            } else if (id == R.id.chipPrincipiante) {
                filtroNivel = "PRINCIPIANTE";
                filtroActiva = null;
            } else if (id == R.id.chipIntermedio) {
                filtroNivel = "INTERMEDIO";
                filtroActiva = null;
            } else if (id == R.id.chipAvanzado) {
                filtroNivel = "AVANZADO";
                filtroActiva = null;
            }
            cargar();
        });
    }

    // Configura el buscador por nombre; filtra en cada cambio de texto
    private void configurarBusqueda() {
        SearchView sv = findViewById(R.id.searchView);
        sv.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String q) {
                filtroNombre = q.trim().isEmpty() ? null : q.trim();
                cargar();
                return true;
            }
            @Override
            public boolean onQueryTextChange(String q) {
                filtroNombre = q.trim().isEmpty() ? null : q.trim();
                cargar();
                return true;
            }
        });
    }

    // Llama al endpoint admin de búsqueda de rutinas predefinidas con los filtros actuales
    private void cargar() {
        API.adminBuscarRutinasPredefinidas(filtroNombre, filtroNivel, null, filtroActiva,
                new UtilREST.OnResponseListener() {
                    @Override
                    public void onSuccess(String response, int statusCode) {
                        try {
                            List<Rutina> nuevas = UtilJSONParser.parseRutinaList(response);
                            lista.clear();
                            lista.addAll(nuevas);
                            adapter.notifyDataSetChanged();
                        } catch (JSONException ignored) {}
                    }
                    @Override
                    public void onError(String message, int statusCode) {}
                });
    }

    // Activa o desactiva una rutina según su estado actual y actualiza el item en la lista
    private void toggleActiva(Rutina r, int pos) {
        UtilREST.OnResponseListener cb = new UtilREST.OnResponseListener() {
            @Override
            public void onSuccess(String response, int statusCode) {
                r.setActiva(!r.isActiva());
                adapter.actualizarItem(pos, r);
                Toast.makeText(AdminRutinasActivity.this,
                        getString(R.string.admin_exito_toggle_rutina), Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onError(String message, int statusCode) {
                Toast.makeText(AdminRutinasActivity.this,
                        getString(R.string.admin_error_generico), Toast.LENGTH_SHORT).show();
            }
        };
        if (r.isActiva()) {
            API.adminDesactivarRutina(r.getId(), cb);
        } else {
            API.adminActivarRutina(r.getId(), cb);
        }
    }

    // Abre la actividad de edición de rutina pasando sus datos actuales como extras
    private void mostrarDialogoEditar(Rutina r, int pos) {
        Intent intent = new Intent(this, EditarRutinaAdminActivity.class);
        intent.putExtra("id", r.getId());
        intent.putExtra("nombre", r.getNombre());
        intent.putExtra("descripcion", r.getDescripcion());
        intent.putExtra("nivel", r.getNivel());
        intent.putExtra("duracionMinutos", r.getDuracionMinutos());
        intent.putExtra("caloriasAproximadas", r.getCaloriasAproximadas());
        intent.putExtra("categoria", r.getCategoria());
        intent.putExtra("diasSemana", r.getDiasSemana());
        startActivity(intent);
    }
}
