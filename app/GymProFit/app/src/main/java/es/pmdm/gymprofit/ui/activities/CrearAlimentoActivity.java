package es.pmdm.gymprofit.ui.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.network.API;
import es.pmdm.gymprofit.network.UtilREST;
import es.pmdm.gymprofit.utils.UIHelper;

/**
 * Formulario para crear un nuevo alimento personalizado.
 * Carga las categorías desde la API. Devuelve RESULT_OK al caller si se guarda.
 */
public class CrearAlimentoActivity extends BaseActivity {

    private TextInputEditText etNombre;
    private Spinner spCategoria;
    private TextInputLayout tilCalorias;
    private TextInputEditText etCalorias;
    private TextInputEditText etProteinas;
    private TextInputEditText etCarbohidratos;
    private TextInputEditText etGrasas;

    private final List<String> categorias = new ArrayList<>();
    private ArrayAdapter<String> categoriaAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crear_alimento);

        etNombre        = findViewById(R.id.etNombre);
        spCategoria     = findViewById(R.id.spCategoria);
        tilCalorias     = findViewById(R.id.tilCalorias);
        etCalorias      = findViewById(R.id.etCalorias);
        etProteinas     = findViewById(R.id.etProteinas);
        etCarbohidratos = findViewById(R.id.etCarbohidratos);
        etGrasas        = findViewById(R.id.etGrasas);

        categoriaAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categorias);
        categoriaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCategoria.setAdapter(categoriaAdapter);
        spCategoria.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                boolean esBebida = "Bebidas".equals(categorias.get(position));
                tilCalorias.setHint(getString(esBebida
                        ? R.string.crear_alimento_calorias_hint_ml
                        : R.string.crear_alimento_calorias_hint));
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        MaterialButton btnGuardar = findViewById(R.id.btnGuardarAlimento);
        btnGuardar.setOnClickListener(v -> guardarAlimento());

        cargarCategorias();
    }

    private void cargarCategorias() {
        API.getCategorias(new UtilREST.OnResponseListener() {
            @Override
            public void onSuccess(String response, int statusCode) {
                try {
                    JSONArray arr = new JSONArray(response);
                    List<String> lista = new ArrayList<>();
                    for (int i = 0; i < arr.length(); i++) {
                        lista.add(arr.getString(i));
                    }
                    runOnUiThread(() -> {
                        categorias.clear();
                        categorias.addAll(lista);
                        categoriaAdapter.notifyDataSetChanged();
                    });
                } catch (JSONException e) {
                    usarCategoriasLocales();
                }
            }
            @Override
            public void onError(String message, int statusCode) {
                usarCategoriasLocales();
            }
        });
    }

    private void usarCategoriasLocales() {
        runOnUiThread(() -> {
            categorias.clear();
            for (String c : getResources().getStringArray(R.array.categorias_alimento)) {
                categorias.add(c);
            }
            categoriaAdapter.notifyDataSetChanged();
        });
    }

    /**
     * Valida campos, construye JSON y llama a API.crearAlimento.
     */
    private void guardarAlimento() {
        String nombre = etNombre.getText() != null
                ? etNombre.getText().toString().trim() : "";
        if (nombre.isEmpty()) {
            Toast.makeText(this, getString(R.string.crear_alimento_nombre_requerido), Toast.LENGTH_SHORT).show();
            return;
        }

        String caloriasStr = etCalorias.getText() != null
                ? etCalorias.getText().toString().trim() : "";
        int calorias;
        try {
            calorias = Integer.parseInt(caloriasStr);
        } catch (NumberFormatException e) {
            calorias = 0;
        }
        if (caloriasStr.isEmpty() || calorias <= 0) {
            Toast.makeText(this, getString(R.string.crear_alimento_calorias_requeridas), Toast.LENGTH_SHORT).show();
            return;
        }

        if (categorias.isEmpty()) {
            UIHelper.mostrarToastError(this, getString(R.string.error_conexion));
            return;
        }

        double proteinas     = parseDoubleOrZero(etProteinas);
        double carbohidratos = parseDoubleOrZero(etCarbohidratos);
        double grasas        = parseDoubleOrZero(etGrasas);

        try {
            JSONObject body = new JSONObject();
            body.put("nombre", nombre);
            body.put("categoria", spCategoria.getSelectedItem().toString());
            body.put("calorias", calorias);
            body.put("proteinas", proteinas);
            body.put("carbohidratos", carbohidratos);
            body.put("grasas", grasas);
            body.put("usuarioId", prefsManager.getUsuarioId());

            API.crearAlimento(body, new UtilREST.OnResponseListener() {
                @Override
                public void onSuccess(String response, int statusCode) {
                    runOnUiThread(() -> {
                        setResult(RESULT_OK);
                        finish();
                    });
                }
                @Override
                public void onError(String message, int statusCode) {
                    runOnUiThread(() ->
                            UIHelper.mostrarToastError(CrearAlimentoActivity.this,
                                    getString(R.string.error_conexion)));
                }
            });
        } catch (JSONException e) {
            UIHelper.mostrarToastError(this, getString(R.string.error_conexion));
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
