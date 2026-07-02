package es.pmdm.gymprofit.ui.activities;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONException;
import org.json.JSONObject;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.network.API;
import es.pmdm.gymprofit.network.UtilREST;
import es.pmdm.gymprofit.utils.UIHelper;

// ============================================================
// EditarEjercicioAdminActivity — pantalla de administración para
// editar un ejercicio existente del catálogo.
// Precarga los campos con los datos recibidos por Intent y envía el
// formulario editado a la API (solo accesible por rol ADMIN).
// ============================================================
public class EditarEjercicioAdminActivity extends BaseActivity {

    private int ejercicioId;
    private TextInputEditText etNombre, etDescripcion, etCalorias, etEquipo, etInstrucciones;
    private Spinner spGrupoMuscular, spDificultad;

    // Opciones fijas para los spinners de grupo muscular y dificultad.
    private static final String[] GRUPOS = {
            "PECHO", "ESPALDA", "PIERNAS", "HOMBROS", "BRAZOS", "ABDOMEN", "CARDIO", "FULLBODY"
    };
    private static final String[] DIFICULTADES = {"PRINCIPIANTE", "INTERMEDIO", "AVANZADO"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editar_ejercicio_admin);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnGuardar).setOnClickListener(v -> guardar());

        etNombre       = findViewById(R.id.etNombre);
        etDescripcion  = findViewById(R.id.etDescripcion);
        etCalorias     = findViewById(R.id.etCalorias);
        etEquipo       = findViewById(R.id.etEquipo);
        etInstrucciones= findViewById(R.id.etInstrucciones);
        spGrupoMuscular= findViewById(R.id.spGrupoMuscular);
        spDificultad   = findViewById(R.id.spDificultad);

        ArrayAdapter<String> grupoAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, GRUPOS);
        grupoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spGrupoMuscular.setAdapter(grupoAdapter);

        ArrayAdapter<String> difAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, DIFICULTADES);
        difAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spDificultad.setAdapter(difAdapter);

        ejercicioId = getIntent().getIntExtra("id", -1);
        etNombre.setText(getIntent().getStringExtra("nombre"));
        etDescripcion.setText(getIntent().getStringExtra("descripcion"));
        int calorias = getIntent().getIntExtra("calorias", 0);
        if (calorias > 0) etCalorias.setText(String.valueOf(calorias));
        etEquipo.setText(getIntent().getStringExtra("equipoNecesario"));
        etInstrucciones.setText(getIntent().getStringExtra("instrucciones"));

        String grupo = getIntent().getStringExtra("grupoMuscular");
        if (grupo != null) {
            for (int i = 0; i < GRUPOS.length; i++) {
                if (GRUPOS[i].equalsIgnoreCase(grupo)) { spGrupoMuscular.setSelection(i); break; }
            }
        }
        String dif = getIntent().getStringExtra("dificultad");
        if (dif != null) {
            for (int i = 0; i < DIFICULTADES.length; i++) {
                if (DIFICULTADES[i].equalsIgnoreCase(dif)) { spDificultad.setSelection(i); break; }
            }
        }
    }

    // Valida el nombre, construye el JSON con los campos editados
    // (los opcionales vacíos se envían como null) y llama a la API de admin.
    private void guardar() {
        String nombre = etNombre.getText() != null ? etNombre.getText().toString().trim() : "";
        if (nombre.isEmpty()) {
            UIHelper.mostrarToastError(this, getString(R.string.admin_nombre_requerido));
            return;
        }

        try {
            JSONObject body = new JSONObject();
            body.put("nombre", nombre);

            String desc = etDescripcion.getText() != null ? etDescripcion.getText().toString().trim() : "";
            body.put("descripcion", desc.isEmpty() ? JSONObject.NULL : desc);

            body.put("grupoMuscular", GRUPOS[spGrupoMuscular.getSelectedItemPosition()]);
            body.put("dificultad", DIFICULTADES[spDificultad.getSelectedItemPosition()]);

            String calStr = etCalorias.getText() != null ? etCalorias.getText().toString().trim() : "";
            if (!calStr.isEmpty()) body.put("caloriasQuemadas", Integer.parseInt(calStr));

            String equipo = etEquipo.getText() != null ? etEquipo.getText().toString().trim() : "";
            body.put("equipoNecesario", equipo.isEmpty() ? JSONObject.NULL : equipo);

            String instr = etInstrucciones.getText() != null ? etInstrucciones.getText().toString().trim() : "";
            body.put("instrucciones", instr.isEmpty() ? JSONObject.NULL : instr);

            API.adminEditarEjercicio(ejercicioId, body, new UtilREST.OnResponseListener() {
                @Override
                public void onSuccess(String response, int statusCode) {
                    runOnUiThread(() -> {
                        UIHelper.mostrarToastExito(EditarEjercicioAdminActivity.this,
                                getString(R.string.admin_exito_editar_ejercicio));
                        setResult(RESULT_OK);
                        finish();
                    });
                }
                @Override
                public void onError(String message, int statusCode) {
                    runOnUiThread(() -> UIHelper.mostrarToastError(EditarEjercicioAdminActivity.this,
                            getString(R.string.admin_error_generico)));
                }
            });
        } catch (JSONException | NumberFormatException e) {
            UIHelper.mostrarToastError(this, getString(R.string.admin_error_generico));
        }
    }
}
