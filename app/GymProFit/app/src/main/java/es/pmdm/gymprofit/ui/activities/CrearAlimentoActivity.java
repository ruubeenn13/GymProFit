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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.network.AlimentoApi;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.model.alimento.Alimento;
import es.pmdm.gymprofit.utils.UIHelper;

// ============================================================
// CrearAlimentoActivity — formulario de creación de alimento personalizado
// Permite al usuario dar de alta un alimento propio con macros y categoría,
// usada desde AnadirAlimentoActivity para ampliar el catálogo disponible.
// ============================================================
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

    // Categorías disponibles para el spinner, obtenidas de la API (con fallback local)
    private final List<String> categorias = new ArrayList<>();
    private ArrayAdapter<String> categoriaAdapter;

    // Servicio Retrofit tipado del dominio alimentos (etapa 2).
    private final AlimentoApi alimentoApi = ApiClient.service(AlimentoApi.class);

    // Enlaza las vistas, configura el spinner de categorías y los listeners de guardado/volver
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

    // Obtiene la lista de categorías de la API; si falla, usa el fallback local (usarCategoriasLocales)
    private void cargarCategorias() {
        alimentoApi.getCategorias().enqueue(new ApiCallback<List<String>>() {
            @Override
            public void onOk(List<String> lista) {
                if (lista == null) {
                    usarCategoriasLocales();
                    return;
                }
                categorias.clear();
                categorias.addAll(lista);
                categoriaAdapter.notifyDataSetChanged();
            }
            @Override
            public void onFail(int code, String message) {
                usarCategoriasLocales();
            }
        });
    }

    // Fallback: carga las categorías desde el array de recursos local si la API no responde
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
     * Valida campos, construye el cuerpo y llama a alimentoApi.crear.
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

        // Cuerpo parcial: BigDecimal en los macros decimales, enteros/strings tal cual.
        Map<String, Object> body = new HashMap<>();
        body.put("nombre", nombre);
        body.put("categoria", spCategoria.getSelectedItem().toString());
        body.put("calorias", calorias);
        body.put("proteinas", BigDecimal.valueOf(proteinas));
        body.put("carbohidratos", BigDecimal.valueOf(carbohidratos));
        body.put("grasas", BigDecimal.valueOf(grasas));
        body.put("usuarioId", prefsManager.getUsuarioId());

        alimentoApi.crear(body).enqueue(new ApiCallback<Alimento>() {
            @Override
            public void onOk(Alimento creado) {
                setResult(RESULT_OK);
                finish();
            }
            @Override
            public void onFail(int code, String message) {
                UIHelper.mostrarToastError(CrearAlimentoActivity.this,
                        getString(R.string.error_conexion));
            }
        });
    }

    // Parsea el contenido de un campo a double, devolviendo 0.0 si está vacío o no es válido
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
