package es.pmdm.gymprofit.ui.activities;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.rutina.Rutina;
import es.pmdm.gymprofit.network.API;
import es.pmdm.gymprofit.network.UtilJSONParser;
import es.pmdm.gymprofit.network.UtilREST;
import es.pmdm.gymprofit.ui.adapters.AdminRutinaAdapter;

public class AdminRutinasActivity extends BaseActivity {

    private RecyclerView rv;
    private AdminRutinaAdapter adapter;
    private final List<Rutina> lista = new ArrayList<>();

    private Boolean filtroActiva = null;
    private String filtroNivel = null;
    private String filtroNombre = null;

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

    private void mostrarDialogoEditar(Rutina r, int pos) {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_editar_rutina, null);
        EditText etNombre = v.findViewById(R.id.etNombreRutina);
        EditText etDescripcion = v.findViewById(R.id.etDescripcionRutina);

        etNombre.setText(r.getNombre());
        etDescripcion.setText(r.getDescripcion());

        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.admin_editar_rutina_titulo))
                .setView(v)
                .setPositiveButton(getString(R.string.admin_guardar), (d, w) -> {
                    String nombre = etNombre.getText().toString().trim();
                    String desc = etDescripcion.getText().toString().trim();
                    if (nombre.isEmpty()) return;
                    try {
                        JSONObject body = new JSONObject();
                        body.put("nombre", nombre);
                        body.put("descripcion", desc);
                        API.adminEditarRutina(r.getId(), body, new UtilREST.OnResponseListener() {
                            @Override
                            public void onSuccess(String response, int statusCode) {
                                r.setNombre(nombre);
                                r.setDescripcion(desc);
                                adapter.actualizarItem(pos, r);
                                Toast.makeText(AdminRutinasActivity.this,
                                        getString(R.string.admin_exito_editar_rutina), Toast.LENGTH_SHORT).show();
                            }
                            @Override
                            public void onError(String message, int statusCode) {
                                Toast.makeText(AdminRutinasActivity.this,
                                        getString(R.string.admin_error_generico), Toast.LENGTH_SHORT).show();
                            }
                        });
                    } catch (JSONException ignored) {}
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
