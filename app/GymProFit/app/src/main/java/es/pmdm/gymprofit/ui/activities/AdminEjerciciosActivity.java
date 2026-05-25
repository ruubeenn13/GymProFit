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
import es.pmdm.gymprofit.model.ejercicio.Ejercicio;
import es.pmdm.gymprofit.network.API;
import es.pmdm.gymprofit.network.UtilJSONParser;
import es.pmdm.gymprofit.network.UtilREST;
import es.pmdm.gymprofit.ui.adapters.AdminEjercicioAdapter;

public class AdminEjerciciosActivity extends BaseActivity {

    private RecyclerView rv;
    private AdminEjercicioAdapter adapter;
    private final List<Ejercicio> lista = new ArrayList<>();

    private Boolean filtroActivo = null;
    private String filtroNombre = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_ejercicios);

        setupMenuButton();
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        rv = findViewById(R.id.rvEjercicios);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminEjercicioAdapter(lista, new AdminEjercicioAdapter.OnAccionListener() {
            @Override
            public void onToggleActivo(Ejercicio e, int pos) {
                toggleActivo(e, pos);
            }
            @Override
            public void onEditar(Ejercicio e, int pos) {
                mostrarDialogoEditar(e, pos);
            }
        });
        rv.setAdapter(adapter);

        configurarChips();
        configurarBusqueda();
        cargar();
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargar();
    }

    private void configurarChips() {
        ChipGroup cg = findViewById(R.id.chipGroupFiltros);
        cg.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if (id == R.id.chipTodos) {
                filtroActivo = null;
            } else if (id == R.id.chipActivos) {
                filtroActivo = true;
            } else if (id == R.id.chipInactivos) {
                filtroActivo = false;
            }
            cargar();
        });
    }

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

    private void cargar() {
        API.adminBuscarEjercicios(filtroNombre, null, null, filtroActivo,
                new UtilREST.OnResponseListener() {
                    @Override
                    public void onSuccess(String response, int statusCode) {
                        try {
                            List<Ejercicio> nuevos = UtilJSONParser.parseEjercicioList(response);
                            lista.clear();
                            lista.addAll(nuevos);
                            adapter.notifyDataSetChanged();
                        } catch (JSONException ignored) {}
                    }
                    @Override
                    public void onError(String message, int statusCode) {}
                });
    }

    private void toggleActivo(Ejercicio e, int pos) {
        UtilREST.OnResponseListener cb = new UtilREST.OnResponseListener() {
            @Override
            public void onSuccess(String response, int statusCode) {
                e.setActivo(!e.isActivo());
                adapter.actualizarItem(pos, e);
                Toast.makeText(AdminEjerciciosActivity.this,
                        getString(R.string.admin_exito_toggle_ejercicio), Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onError(String message, int statusCode) {
                Toast.makeText(AdminEjerciciosActivity.this,
                        getString(R.string.admin_error_generico), Toast.LENGTH_SHORT).show();
            }
        };
        if (e.isActivo()) {
            API.adminDesactivarEjercicio(e.getId(), cb);
        } else {
            API.adminActivarEjercicio(e.getId(), cb);
        }
    }

    private void mostrarDialogoEditar(Ejercicio e, int pos) {
        Intent intent = new Intent(this, EditarEjercicioAdminActivity.class);
        intent.putExtra("id", e.getId());
        intent.putExtra("nombre", e.getNombre());
        intent.putExtra("descripcion", e.getDescripcion());
        intent.putExtra("grupoMuscular", e.getGrupoMuscular());
        intent.putExtra("dificultad", e.getDificultad());
        intent.putExtra("calorias", e.getCalorias());
        intent.putExtra("equipoNecesario", e.getEquipoNecesario());
        intent.putExtra("instrucciones", e.getInstrucciones());
        startActivity(intent);
    }
}
