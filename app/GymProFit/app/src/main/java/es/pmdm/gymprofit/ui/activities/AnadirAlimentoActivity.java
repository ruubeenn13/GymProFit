package es.pmdm.gymprofit.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import android.app.AlertDialog;
import android.app.Dialog;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.alimento.Alimento;
import es.pmdm.gymprofit.network.API;
import es.pmdm.gymprofit.network.UtilJSONParser;
import es.pmdm.gymprofit.network.UtilREST;
import es.pmdm.gymprofit.ui.adapters.AlimentoAdapter;
import es.pmdm.gymprofit.utils.UIHelper;


/**
 * Permite al usuario buscar un alimento y añadirlo a una comida del día.
 * Long-press sobre alimento propio muestra menú para editar o eliminar.
 */
public class AnadirAlimentoActivity extends BaseActivity {

    private String tipoComida;
    private int comidaId;
    private String fecha;

    private final List<Alimento> listaAlimentosFull = new ArrayList<>();
    private final List<Alimento> listaAlimentosFiltrada = new ArrayList<>();
    private AlimentoAdapter adapter;

    private ActivityResultLauncher<Intent> crearAlimentoLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_anadir_alimento);

        tipoComida = getIntent().getStringExtra("tipoComida");
        comidaId   = getIntent().getIntExtra("comidaId", -1);
        fecha      = getIntent().getStringExtra("fecha");

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        RecyclerView rvAlimentos = findViewById(R.id.rvAlimentos);
        rvAlimentos.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AlimentoAdapter(listaAlimentosFiltrada, this::mostrarDialogoGramos);
        adapter.setOnItemLongClickListener((alimento, anchor) -> {
            boolean esAdmin = "ROLE_ADMIN".equals(prefsManager.getRol());
            boolean esPropio = alimento.getUsuarioId() != null
                    && alimento.getUsuarioId() == prefsManager.getUsuarioId();
            if (esPropio || esAdmin) {
                mostrarMenuContextualAlimento(alimento, anchor);
            }
        });
        rvAlimentos.setAdapter(adapter);

        EditText etBuscador = findViewById(R.id.etBuscador);
        etBuscador.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim().toLowerCase(Locale.getDefault());
                listaAlimentosFiltrada.clear();
                if (query.isEmpty()) {
                    listaAlimentosFiltrada.addAll(listaAlimentosFull);
                } else {
                    for (Alimento a : listaAlimentosFull) {
                        if (a.getNombre().toLowerCase(Locale.getDefault()).contains(query)) {
                            listaAlimentosFiltrada.add(a);
                        }
                    }
                }
                adapter.notifyDataSetChanged();
            }
        });

        crearAlimentoLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        cargarAlimentos();
                    }
                });

        MaterialButton btnCrearAlimento = findViewById(R.id.btnCrearAlimento);
        btnCrearAlimento.setOnClickListener(v -> {
            Intent intent = new Intent(this, CrearAlimentoActivity.class);
            crearAlimentoLauncher.launch(intent);
        });

        cargarAlimentos();
    }

    private void mostrarMenuContextualAlimento(Alimento alimento, View anchorView) {
        boolean esAdmin = "ROLE_ADMIN".equals(prefsManager.getRol());
        boolean esPredefinido = alimento.getUsuarioId() == null;

        List<UIHelper.MenuAction> actions = new ArrayList<>();
        actions.add(new UIHelper.MenuAction(R.drawable.ic_edit, getString(R.string.alimento_editar),
                () -> mostrarDialogoEditarAlimento(alimento)));
        if (esAdmin && esPredefinido) {
            actions.add(new UIHelper.MenuAction(R.drawable.ic_visibility_off, getString(R.string.comida_desactivar_alimento),
                    () -> UIHelper.mostrarDialogoConIcono(this,
                            getString(R.string.comida_desactivar_alimento),
                            getString(R.string.alimento_desactivar_confirmar),
                            R.drawable.ic_visibility_off,
                            () -> desactivarAlimento(alimento))));
        }
        actions.add(new UIHelper.MenuAction(R.drawable.ic_delete, getString(R.string.alimento_eliminar), true,
                () -> UIHelper.mostrarDialogoConIcono(this,
                        getString(R.string.alimento_eliminar),
                        getString(R.string.alimento_eliminar_confirmar),
                        R.drawable.ic_delete,
                        () -> eliminarAlimento(alimento))));
        UIHelper.mostrarBottomMenu(this, alimento.getNombre(), actions);
    }

    private void mostrarDialogoEditarAlimento(Alimento alimento) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_editar_alimento, null);
        ((TextView) dialogView.findViewById(R.id.tvDialogTitulo)).setText(getString(R.string.alimento_editar));

        TextInputEditText etNombre    = dialogView.findViewById(R.id.etNombreEditar);
        TextInputEditText etCalorias  = dialogView.findViewById(R.id.etCaloriasEditar);
        TextInputEditText etProteinas = dialogView.findViewById(R.id.etProteinasEditar);
        TextInputEditText etCarbos    = dialogView.findViewById(R.id.etCarbohidratosEditar);
        TextInputEditText etGrasas    = dialogView.findViewById(R.id.etGrasasEditar);

        etNombre.setText(alimento.getNombre());
        etCalorias.setText(String.valueOf(alimento.getCalorias()));
        etProteinas.setText(String.format(Locale.getDefault(), "%.1f", alimento.getProteinas()));
        etCarbos.setText(String.format(Locale.getDefault(), "%.1f", alimento.getCarbohidratos()));
        etGrasas.setText(String.format(Locale.getDefault(), "%.1f", alimento.getGrasas()));

        Dialog dialog = UIHelper.prepararDialogoFormulario(this, dialogView);

        dialogView.findViewById(R.id.btnDialogConfirmar).setOnClickListener(v -> {
            try {
                JSONObject body = new JSONObject();
                String nombre = etNombre.getText() != null ? etNombre.getText().toString().trim() : "";
                if (!nombre.isEmpty()) body.put("nombre", nombre);
                String calStr = etCalorias.getText() != null ? etCalorias.getText().toString().trim() : "";
                if (!calStr.isEmpty()) body.put("calorias", Integer.parseInt(calStr));
                body.put("proteinas",     parseDoubleOrZero(etProteinas));
                body.put("carbohidratos", parseDoubleOrZero(etCarbos));
                body.put("grasas",        parseDoubleOrZero(etGrasas));
                dialog.dismiss();
                API.adminPatchAlimento(alimento.getId(), body, new UtilREST.OnResponseListener() {
                    @Override
                    public void onSuccess(String response, int statusCode) {
                        runOnUiThread(() -> cargarAlimentos());
                    }
                    @Override
                    public void onError(String message, int statusCode) {
                        runOnUiThread(() -> UIHelper.mostrarToastError(
                                AnadirAlimentoActivity.this, getString(R.string.error_conexion)));
                    }
                });
            } catch (JSONException | NumberFormatException e) {
                UIHelper.mostrarToastError(this, getString(R.string.error_conexion));
            }
        });
        dialogView.findViewById(R.id.btnDialogCancelar).setOnClickListener(v -> dialog.dismiss());

        UIHelper.mostrarDialogoFormulario(this, dialog);
    }

    private void desactivarAlimento(Alimento alimento) {
        API.adminToggleActivoAlimento(alimento.getId(), false, new UtilREST.OnResponseListener() {
            @Override
            public void onSuccess(String response, int statusCode) {
                runOnUiThread(() -> cargarAlimentos());
            }
            @Override
            public void onError(String message, int statusCode) {
                runOnUiThread(() -> UIHelper.mostrarToastError(
                        AnadirAlimentoActivity.this, getString(R.string.error_conexion)));
            }
        });
    }

    private void eliminarAlimento(Alimento alimento) {
        API.adminToggleActivoAlimento(alimento.getId(), false, new UtilREST.OnResponseListener() {
            @Override
            public void onSuccess(String response, int statusCode) {
                runOnUiThread(() -> cargarAlimentos());
            }
            @Override
            public void onError(String message, int statusCode) {
                runOnUiThread(() -> UIHelper.mostrarToastError(
                        AnadirAlimentoActivity.this, getString(R.string.error_conexion)));
            }
        });
    }

    private void cargarAlimentos() {
        API.getAlimentosActivos(new UtilREST.OnResponseListener() {
            @Override
            public void onSuccess(String response, int statusCode) {
                try {
                    List<Alimento> alimentos = UtilJSONParser.parseListaAlimentos(response);
                    runOnUiThread(() -> {
                        listaAlimentosFull.clear();
                        listaAlimentosFull.addAll(alimentos);
                        listaAlimentosFiltrada.clear();
                        listaAlimentosFiltrada.addAll(listaAlimentosFull);
                        adapter.notifyDataSetChanged();
                    });
                } catch (JSONException e) {
                    runOnUiThread(() ->
                            Toast.makeText(AnadirAlimentoActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onError(String message, int statusCode) {
                runOnUiThread(() ->
                        Toast.makeText(AnadirAlimentoActivity.this, message, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void mostrarDialogoGramos(Alimento alimento) {
        android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_gramos, null);
        EditText etGramos = dialogView.findViewById(R.id.etGramos);
        TextView tvPreviewMacros = dialogView.findViewById(R.id.tvPreviewMacros);

        etGramos.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String raw = s.toString().trim();
                if (raw.isEmpty()) {
                    tvPreviewMacros.setText(getString(R.string.anadir_alimento_preview, 0, 0.0, 0.0, 0.0));
                    return;
                }
                try {
                    double gramos = Double.parseDouble(raw);
                    int kcal  = (int) (alimento.getCalorias()      * gramos / 100);
                    double prot  = alimento.getProteinas()     * gramos / 100;
                    double carbs = alimento.getCarbohidratos() * gramos / 100;
                    double gras  = alimento.getGrasas()        * gramos / 100;
                    tvPreviewMacros.setText(getString(R.string.anadir_alimento_preview, kcal, prot, carbs, gras));
                } catch (NumberFormatException ignored) {
                    tvPreviewMacros.setText(getString(R.string.anadir_alimento_preview, 0, 0.0, 0.0, 0.0));
                }
            }
        });

        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton(getString(R.string.btn_anadir), (dialog, which) -> {
                    String raw = etGramos.getText().toString().trim();
                    if (raw.isEmpty()) {
                        Toast.makeText(this, getString(R.string.anadir_alimento_gramos_hint), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    double gramos;
                    try {
                        gramos = Double.parseDouble(raw);
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, getString(R.string.anadir_alimento_gramos_hint), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (gramos <= 0) {
                        Toast.makeText(this, getString(R.string.anadir_alimento_gramos_hint), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    anadirAlimento(alimento, gramos);
                })
                .setNegativeButton(getString(R.string.dialog_cancelar), null)
                .show();
    }

    private void anadirAlimento(Alimento alimento, double gramos) {
        if (comidaId == -1) {
            try {
                JSONObject body = new JSONObject();
                body.put("usuarioId", prefsManager.getUsuarioId());
                body.put("tipoComida", tipoComida);
                body.put("fecha", fecha + "T00:00:00");
                API.crearComida(body, new UtilREST.OnResponseListener() {
                    @Override
                    public void onSuccess(String response, int statusCode) {
                        try {
                            comidaId = new JSONObject(response).getInt("id");
                            postAlimentoComida(alimento, gramos);
                        } catch (JSONException e) {
                            runOnUiThread(() ->
                                    Toast.makeText(AnadirAlimentoActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show());
                        }
                    }
                    @Override
                    public void onError(String message, int statusCode) {
                        runOnUiThread(() ->
                                Toast.makeText(AnadirAlimentoActivity.this, message, Toast.LENGTH_SHORT).show());
                    }
                });
            } catch (JSONException e) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            postAlimentoComida(alimento, gramos);
        }
    }

    private void postAlimentoComida(Alimento alimento, double gramos) {
        try {
            JSONObject body = new JSONObject();
            body.put("comidaId", comidaId);
            body.put("alimentoId", alimento.getId());
            body.put("cantidadGramos", gramos);
            API.anadirAlimentoAComida(body, new UtilREST.OnResponseListener() {
                @Override
                public void onSuccess(String response, int statusCode) {
                    runOnUiThread(() -> {
                        Intent result = new Intent();
                        result.putExtra("comidaId", comidaId);
                        setResult(RESULT_OK, result);
                        finish();
                    });
                }
                @Override
                public void onError(String message, int statusCode) {
                    runOnUiThread(() ->
                            Toast.makeText(AnadirAlimentoActivity.this, message, Toast.LENGTH_SHORT).show());
                }
            });
        } catch (JSONException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private double parseDoubleOrZero(TextInputEditText field) {
        if (field.getText() == null) return 0.0;
        String raw = field.getText().toString().trim();
        if (raw.isEmpty()) return 0.0;
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
