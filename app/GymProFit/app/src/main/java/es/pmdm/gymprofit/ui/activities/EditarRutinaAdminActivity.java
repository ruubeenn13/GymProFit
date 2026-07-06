package es.pmdm.gymprofit.ui.activities;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.google.android.material.textfield.TextInputEditText;

import java.util.HashMap;
import java.util.Map;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.RutinaApi;
import es.pmdm.gymprofit.utils.LoadingDialog;
import es.pmdm.gymprofit.utils.UiFeedback;
import es.pmdm.gymprofit.utils.UIHelper;

// ============================================================
// EditarRutinaAdminActivity — pantalla de administración para editar
// una rutina predefinida del catálogo.
// Precarga los campos con los datos recibidos por Intent y envía el
// formulario editado a la API (solo accesible por rol ADMIN).
// ============================================================
public class EditarRutinaAdminActivity extends BaseActivity {

    private int rutinaId;
    private TextInputEditText etNombre, etDescripcion, etDuracion, etCalorias, etCategoria, etDiasSemana;
    private Spinner spNivel;

    private static final String[] NIVELES = {"PRINCIPIANTE", "INTERMEDIO", "AVANZADO"};

    // Interfaz Retrofit tipada del dominio rutinas (CRUD admin, etapa 2)
    private final RutinaApi rutinaApi = ApiClient.service(RutinaApi.class);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editar_rutina_admin);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnGuardar).setOnClickListener(v -> guardar());

        etNombre     = findViewById(R.id.etNombre);
        etDescripcion= findViewById(R.id.etDescripcion);
        etDuracion   = findViewById(R.id.etDuracion);
        etCalorias   = findViewById(R.id.etCaloriasRutina);
        etCategoria  = findViewById(R.id.etCategoria);
        etDiasSemana = findViewById(R.id.etDiasSemana);
        spNivel      = findViewById(R.id.spNivel);

        ArrayAdapter<String> nivelAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, NIVELES);
        nivelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spNivel.setAdapter(nivelAdapter);

        rutinaId = getIntent().getIntExtra("id", -1);
        etNombre.setText(getIntent().getStringExtra("nombre"));
        etDescripcion.setText(getIntent().getStringExtra("descripcion"));
        int duracion = getIntent().getIntExtra("duracionMinutos", 0);
        if (duracion > 0) etDuracion.setText(String.valueOf(duracion));
        int calorias = getIntent().getIntExtra("caloriasAproximadas", 0);
        if (calorias > 0) etCalorias.setText(String.valueOf(calorias));
        etCategoria.setText(getIntent().getStringExtra("categoria"));
        etDiasSemana.setText(getIntent().getStringExtra("diasSemana"));

        String nivel = getIntent().getStringExtra("nivel");
        if (nivel != null) {
            for (int i = 0; i < NIVELES.length; i++) {
                if (NIVELES[i].equalsIgnoreCase(nivel)) {
                    spNivel.setSelection(i);
                    break;
                }
            }
        }
    }

    // Valida el nombre, construye el JSON con los campos editados
    // (los opcionales vacíos se omiten o se envían como null) y llama a la API de admin.
    private void guardar() {
        String nombre = etNombre.getText() != null ? etNombre.getText().toString().trim() : "";
        if (nombre.isEmpty()) {
            UIHelper.mostrarToastError(this, getString(R.string.admin_nombre_requerido));
            return;
        }

        try {
            // Cuerpo de edición parcial; la descripción vacía se envía como null
            // (Gson con serializeNulls), el resto de opcionales vacíos se omiten.
            Map<String, Object> body = new HashMap<>();
            body.put("nombre", nombre);

            String desc = etDescripcion.getText() != null ? etDescripcion.getText().toString().trim() : "";
            body.put("descripcion", desc.isEmpty() ? null : desc);

            body.put("nivel", NIVELES[spNivel.getSelectedItemPosition()]);

            String durStr = etDuracion.getText() != null ? etDuracion.getText().toString().trim() : "";
            if (!durStr.isEmpty()) body.put("duracionMinutos", Integer.parseInt(durStr));

            String calStr = etCalorias.getText() != null ? etCalorias.getText().toString().trim() : "";
            if (!calStr.isEmpty()) body.put("caloriasAproximadas", Integer.parseInt(calStr));

            String cat = etCategoria.getText() != null ? etCategoria.getText().toString().trim() : "";
            if (!cat.isEmpty()) body.put("categoria", cat);

            String dias = etDiasSemana.getText() != null ? etDiasSemana.getText().toString().trim() : "";
            if (!dias.isEmpty()) body.put("diasSemana", dias);

            // Muestra el overlay de carga tras el parseo numérico para no
            // dejarlo colgado si Integer.parseInt lanza NumberFormatException.
            LoadingDialog.show(this);
            rutinaApi.patch(rutinaId, body).enqueue(new ApiCallback<Void>() {
                @Override
                public void onOk(Void b) {
                    // Oculta el overlay antes de mostrar el éxito y cerrar
                    LoadingDialog.hide(EditarRutinaAdminActivity.this);
                    UIHelper.mostrarToastExito(EditarRutinaAdminActivity.this,
                            getString(R.string.admin_exito_editar_rutina));
                    setResult(RESULT_OK);
                    finish();
                }
                @Override
                public void onFail(int code, String message) {
                    // Oculta el overlay y mapea el error según el código HTTP
                    LoadingDialog.hide(EditarRutinaAdminActivity.this);
                    UiFeedback.toastError(EditarRutinaAdminActivity.this, code, message);
                }
            });
        } catch (NumberFormatException e) {
            UIHelper.mostrarToastError(this, getString(R.string.admin_error_generico));
        }
    }
}
