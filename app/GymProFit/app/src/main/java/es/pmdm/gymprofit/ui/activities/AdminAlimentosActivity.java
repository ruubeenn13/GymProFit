package es.pmdm.gymprofit.ui.activities;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
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
import es.pmdm.gymprofit.model.alimento.Alimento;
import es.pmdm.gymprofit.network.API;
import es.pmdm.gymprofit.network.UtilJSONParser;
import es.pmdm.gymprofit.network.UtilREST;
import es.pmdm.gymprofit.ui.adapters.AdminAlimentoAdapter;

// ============================================================
// AdminAlimentosActivity — gestión CRUD de alimentos desde el panel de administración
// Permite buscar/filtrar alimentos por nombre, categoría y estado activo,
// activarlos/desactivarlos (borrado lógico) y editar sus valores
// nutricionales mediante un diálogo. Solo accesible para rol ADMIN.
// ============================================================
public class AdminAlimentosActivity extends BaseActivity {

    // Lista de alimentos mostrada en el RecyclerView y su adaptador
    private final List<Alimento> lista = new ArrayList<>();
    private AdminAlimentoAdapter adapter;

    // Filtros activos de la búsqueda (null = sin filtrar por ese campo)
    private String filtroNombre = null;
    private String filtroCategoria = null;
    private Boolean filtroActivo = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_alimentos);

        setupMenuButton();
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        RecyclerView rv = findViewById(R.id.rvAlimentos);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminAlimentoAdapter(lista, new AdminAlimentoAdapter.OnAccionListener() {
            @Override
            public void onToggleActivo(Alimento a, int pos) {
                toggleActivo(a, pos);
            }
            @Override
            public void onEditar(Alimento a, int pos) {
                mostrarDialogoEditar(a, pos);
            }
        });
        rv.setAdapter(adapter);

        configurarBusqueda();
        configurarSpinner();
        configurarChips();
        cargar();
    }

    // Recarga la lista al volver a esta pantalla (por si se editó desde otro sitio)
    @Override
    protected void onResume() {
        super.onResume();
        cargar();
    }

    // Configura el SearchView para filtrar por nombre en tiempo real
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

    // Configura el spinner de categorías: carga las categorías desde la API (con fallback local)
    private void configurarSpinner() {
        Spinner sp = findViewById(R.id.spCategoria);
        List<String> categorias = new ArrayList<>();
        categorias.add("Todas");
        ArrayAdapter<String> adapterSp = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, categorias);
        adapterSp.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp.setAdapter(adapterSp);
        sp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filtroCategoria = position == 0 ? null : categorias.get(position);
                cargar();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        API.getCategorias(new UtilREST.OnResponseListener() {
            @Override
            public void onSuccess(String response, int statusCode) {
                try {
                    org.json.JSONArray arr = new org.json.JSONArray(response);
                    runOnUiThread(() -> {
                        for (int i = 0; i < arr.length(); i++) {
                            try { categorias.add(arr.getString(i)); } catch (org.json.JSONException ignored) {}
                        }
                        adapterSp.notifyDataSetChanged();
                    });
                } catch (org.json.JSONException ignored) {
                    usarCategoriasLocales(categorias, adapterSp);
                }
            }
            @Override
            public void onError(String message, int statusCode) {
                usarCategoriasLocales(categorias, adapterSp);
            }
        });
    }

    // Fallback: rellena el spinner con las categorías definidas localmente en strings/arrays si falla la API
    private void usarCategoriasLocales(List<String> categorias, ArrayAdapter<String> adapter) {
        runOnUiThread(() -> {
            for (String c : getResources().getStringArray(R.array.categorias_alimento)) {
                categorias.add(c);
            }
            adapter.notifyDataSetChanged();
        });
    }

    // Configura los chips de filtro rápido (todos / activos / inactivos)
    private void configurarChips() {
        ChipGroup cg = findViewById(R.id.chipGroupFiltros);
        cg.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if (id == R.id.chipTodosA) {
                filtroActivo = null;
            } else if (id == R.id.chipActivosA) {
                filtroActivo = true;
            } else if (id == R.id.chipInactivosA) {
                filtroActivo = false;
            }
            cargar();
        });
    }

    // Consulta a la API los alimentos aplicando los filtros actuales y refresca el RecyclerView
    private void cargar() {
        API.adminBuscarAlimentos(filtroNombre, filtroCategoria, filtroActivo,
                new UtilREST.OnResponseListener() {
                    @Override
                    public void onSuccess(String response, int statusCode) {
                        try {
                            List<Alimento> nuevos = UtilJSONParser.parseListaAlimentos(response);
                            runOnUiThread(() -> {
                                lista.clear();
                                lista.addAll(nuevos);
                                adapter.notifyDataSetChanged();
                            });
                        } catch (JSONException ignored) {}
                    }
                    @Override
                    public void onError(String message, int statusCode) {}
                });
    }

    // Activa o desactiva un alimento y actualiza el item en la lista sin recargar todo
    private void toggleActivo(Alimento a, int pos) {
        API.adminToggleActivoAlimento(a.getId(), !a.isActivo(),
                new UtilREST.OnResponseListener() {
                    @Override
                    public void onSuccess(String response, int statusCode) {
                        a.setActivo(!a.isActivo());
                        adapter.actualizarItem(pos, a);
                        Toast.makeText(AdminAlimentosActivity.this,
                                getString(R.string.admin_exito_toggle_alimento), Toast.LENGTH_SHORT).show();
                    }
                    @Override
                    public void onError(String message, int statusCode) {
                        Toast.makeText(AdminAlimentosActivity.this,
                                getString(R.string.admin_error_generico), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Muestra un diálogo para editar los valores nutricionales del alimento y envía un PATCH parcial
    private void mostrarDialogoEditar(Alimento a, int pos) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_editar_alimento, null);
        ((android.widget.TextView) dialogView.findViewById(R.id.tvDialogTitulo))
                .setText(getString(R.string.admin_editar_alimento_titulo));

        EditText etNombre        = dialogView.findViewById(R.id.etNombreEditar);
        EditText etCalorias      = dialogView.findViewById(R.id.etCaloriasEditar);
        EditText etProteinas     = dialogView.findViewById(R.id.etProteinasEditar);
        EditText etCarbohidratos = dialogView.findViewById(R.id.etCarbohidratosEditar);
        EditText etGrasas        = dialogView.findViewById(R.id.etGrasasEditar);

        etNombre.setText(a.getNombre());
        etCalorias.setText(String.valueOf(a.getCalorias()));
        etProteinas.setText(String.valueOf(a.getProteinas()));
        etCarbohidratos.setText(String.valueOf(a.getCarbohidratos()));
        etGrasas.setText(String.valueOf(a.getGrasas()));

        android.app.Dialog dialog = es.pmdm.gymprofit.utils.UIHelper
                .prepararDialogoFormulario(this, dialogView);

        dialogView.findViewById(R.id.btnDialogConfirmar).setOnClickListener(v -> {
            try {
                JSONObject patch = new JSONObject();
                String nombre = etNombre.getText().toString().trim();
                if (!nombre.isEmpty()) patch.put("nombre", nombre);
                String calStr = etCalorias.getText().toString().trim();
                if (!calStr.isEmpty()) patch.put("calorias", Integer.parseInt(calStr));
                String protStr = etProteinas.getText().toString().trim();
                if (!protStr.isEmpty()) patch.put("proteinas", Double.parseDouble(protStr));
                String carbStr = etCarbohidratos.getText().toString().trim();
                if (!carbStr.isEmpty()) patch.put("carbohidratos", Double.parseDouble(carbStr));
                String grasStr = etGrasas.getText().toString().trim();
                if (!grasStr.isEmpty()) patch.put("grasas", Double.parseDouble(grasStr));

                dialog.dismiss();
                API.adminPatchAlimento(a.getId(), patch, new UtilREST.OnResponseListener() {
                    @Override
                    public void onSuccess(String response, int statusCode) {
                        cargar();
                    }
                    @Override
                    public void onError(String message, int statusCode) {
                        Toast.makeText(AdminAlimentosActivity.this,
                                getString(R.string.admin_error_generico), Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (JSONException ignored) {}
        });
        dialogView.findViewById(R.id.btnDialogCancelar).setOnClickListener(v -> dialog.dismiss());

        es.pmdm.gymprofit.utils.UIHelper.mostrarDialogoFormulario(this, dialog);
    }
}
