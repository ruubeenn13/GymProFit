package es.pmdm.gymprofit.ui.activities;

import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.Locale;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.network.API;
import es.pmdm.gymprofit.network.UtilREST;
import es.pmdm.gymprofit.utils.NotificationHelper;
import es.pmdm.gymprofit.utils.PreferencesManager;
import es.pmdm.gymprofit.utils.UIHelper;

// ============================================================
// RegistrarMedicionActivity — Formulario de alta/edición de una medición corporal.
// Permite introducir peso, altura, grasa corporal, masa muscular y perímetros
// (cintura, pecho, brazos, piernas), y los envía a la API creando una medición
// nueva (POST) o actualizando una existente (PATCH) si se recibe un "medicion_id".
// ============================================================
public class RegistrarMedicionActivity extends AppCompatActivity {

    private TextInputEditText etPeso, etAltura, etGrasa, etMusculo;
    private TextInputEditText etCintura, etPecho, etBrazos, etPiernas, etNotas;
    private PreferencesManager prefsManager;
    // Id de la medición a editar, o -1 si se está creando una nueva
    private int medicionId = -1;

    // Inicializa la pantalla; si se recibe un "medicion_id" en el intent,
    // cambia el título a modo edición y precarga los campos con sus valores.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefsManager = new PreferencesManager(this);
        prefsManager.applyTheme();
        aplicarIdioma();
        setContentView(R.layout.activity_registrar_medicion);

        etPeso    = findViewById(R.id.etPeso);
        etAltura  = findViewById(R.id.etAltura);
        etGrasa   = findViewById(R.id.etGrasa);
        etMusculo = findViewById(R.id.etMusculo);
        etCintura = findViewById(R.id.etCintura);
        etPecho   = findViewById(R.id.etPecho);
        etBrazos  = findViewById(R.id.etBrazos);
        etPiernas = findViewById(R.id.etPiernas);
        etNotas   = findViewById(R.id.etNotas);

        Intent intent = getIntent();
        medicionId = intent.getIntExtra("medicion_id", -1);

        if (medicionId != -1) {
            ((TextView) findViewById(R.id.tvTituloMedicion)).setText(R.string.mediciones_editar);
            ((MaterialButton) findViewById(R.id.btnGuardar)).setText(R.string.mediciones_editar_guardar);
            precargarCampos(intent);
        }

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnGuardar).setOnClickListener(v -> guardarMedicion());
    }

    // Rellena los campos del formulario con los valores recibidos por intent
    // (usado en modo edición de una medición existente).
    private void precargarCampos(Intent intent) {
        setDecimal(etPeso,    intent.getDoubleExtra("peso", 0));
        setDecimal(etAltura,  intent.getDoubleExtra("altura", 0));
        setDecimal(etGrasa,   intent.getDoubleExtra("grasa", 0));
        setDecimal(etMusculo, intent.getDoubleExtra("musculo", 0));
        setDecimal(etCintura, intent.getDoubleExtra("cintura", 0));
        setDecimal(etPecho,   intent.getDoubleExtra("pecho", 0));
        setDecimal(etBrazos,  intent.getDoubleExtra("brazos", 0));
        setDecimal(etPiernas, intent.getDoubleExtra("piernas", 0));
        String notas = intent.getStringExtra("notas");
        if (notas != null && !notas.isEmpty()) etNotas.setText(notas);
    }

    // Establece el texto de un campo decimal solo si el valor es positivo.
    private void setDecimal(TextInputEditText et, double value) {
        if (value > 0) et.setText(String.format(Locale.getDefault(), "%.2f", value));
    }

    // Valida el peso (obligatorio), construye el JSON con los campos rellenados
    // y llama a la API para crear o actualizar la medición según medicionId.
    private void guardarMedicion() {
        String pesoStr = etPeso.getText() != null ? etPeso.getText().toString().trim() : "";
        if (pesoStr.isEmpty()) {
            UIHelper.mostrarToastError(this, getString(R.string.error_campo_requerido));
            return;
        }

        try {
            JSONObject body = new JSONObject();
            body.put("peso", new BigDecimal(pesoStr));

            putDecimal(body, "altura",        etAltura);
            putDecimal(body, "grasaCorporal", etGrasa);
            putDecimal(body, "masaMuscular",  etMusculo);
            putDecimal(body, "cintura",       etCintura);
            putDecimal(body, "pecho",         etPecho);
            putDecimal(body, "brazos",        etBrazos);
            putDecimal(body, "piernas",       etPiernas);

            String notas = etNotas.getText() != null ? etNotas.getText().toString().trim() : "";
            if (!notas.isEmpty()) body.put("notas", notas);

            if (medicionId != -1) {
                API.patchMedicion(medicionId, body, new UtilREST.OnResponseListener() {
                    @Override
                    public void onSuccess(String response, int statusCode) {
                        runOnUiThread(() -> {
                            UIHelper.mostrarToastExito(RegistrarMedicionActivity.this,
                                    getString(R.string.mediciones_editar_exito));
                            setResult(RESULT_OK);
                            finish();
                        });
                    }
                    @Override
                    public void onError(String message, int statusCode) {
                        runOnUiThread(() -> UIHelper.mostrarToastError(
                                RegistrarMedicionActivity.this, getString(R.string.error_conexion)));
                    }
                });
            } else {
                body.put("usuarioId", prefsManager.getUsuarioId());
                API.crearMedicion(body, new UtilREST.OnResponseListener() {
                    @Override
                    public void onSuccess(String response, int statusCode) {
                        runOnUiThread(() -> {
                            NotificationHelper.notificarMedicionGuardada(RegistrarMedicionActivity.this);
                            UIHelper.mostrarToastExito(RegistrarMedicionActivity.this,
                                    getString(R.string.mediciones_exito));
                            setResult(RESULT_OK);
                            finish();
                        });
                    }
                    @Override
                    public void onError(String message, int statusCode) {
                        runOnUiThread(() -> UIHelper.mostrarToastError(
                                RegistrarMedicionActivity.this, getString(R.string.error_conexion)));
                    }
                });
            }

        } catch (JSONException | NumberFormatException e) {
            UIHelper.mostrarToastError(this, getString(R.string.error_conexion));
        }
    }

    // Añade al JSON el valor decimal del campo indicado, solo si no está vacío.
    private void putDecimal(JSONObject body, String key, TextInputEditText et) throws JSONException {
        if (et.getText() == null) return;
        String val = et.getText().toString().trim();
        if (!val.isEmpty()) body.put(key, new BigDecimal(val));
    }

    // Aplica el idioma guardado en preferencias a la configuración de recursos
    // de la Activity antes de inflar el layout.
    private void aplicarIdioma() {
        String lang = prefsManager.getLanguage();
        if (!lang.isEmpty()) {
            Locale locale = new Locale(lang);
            Locale.setDefault(locale);
            Resources res = getResources();
            Configuration cfg = res.getConfiguration();
            cfg.setLocale(locale);
            res.updateConfiguration(cfg, res.getDisplayMetrics());
        }
    }
}
