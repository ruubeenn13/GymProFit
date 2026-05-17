package es.pmdm.gymprofit.ui.activities;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.Locale;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.network.API;
import es.pmdm.gymprofit.network.UtilREST;
import es.pmdm.gymprofit.utils.PreferencesManager;
import es.pmdm.gymprofit.utils.UIHelper;

public class RegistrarMedicionActivity extends AppCompatActivity {

    private TextInputEditText etPeso, etAltura, etGrasa, etMusculo;
    private TextInputEditText etCintura, etPecho, etBrazos, etPiernas, etNotas;
    private PreferencesManager prefsManager;

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

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnGuardar).setOnClickListener(v -> guardarMedicion());
    }

    private void guardarMedicion() {
        String pesoStr = etPeso.getText() != null ? etPeso.getText().toString().trim() : "";
        if (pesoStr.isEmpty()) {
            UIHelper.mostrarToastError(this, getString(R.string.error_campo_requerido));
            return;
        }

        try {
            JSONObject body = new JSONObject();
            body.put("usuarioId", prefsManager.getUsuarioId());
            body.put("peso", new BigDecimal(pesoStr));

            putDecimal(body, "altura",       etAltura);
            putDecimal(body, "grasaCorporal", etGrasa);
            putDecimal(body, "masaMuscular",  etMusculo);
            putDecimal(body, "cintura",       etCintura);
            putDecimal(body, "pecho",         etPecho);
            putDecimal(body, "brazos",        etBrazos);
            putDecimal(body, "piernas",       etPiernas);

            String notas = etNotas.getText() != null ? etNotas.getText().toString().trim() : "";
            if (!notas.isEmpty()) body.put("notas", notas);

            API.crearMedicion(body, new UtilREST.OnResponseListener() {
                @Override
                public void onSuccess(String response, int statusCode) {
                    runOnUiThread(() -> {
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

        } catch (JSONException | NumberFormatException e) {
            UIHelper.mostrarToastError(this, getString(R.string.error_conexion));
        }
    }

    private void putDecimal(JSONObject body, String key, TextInputEditText et) throws JSONException {
        if (et.getText() == null) return;
        String val = et.getText().toString().trim();
        if (!val.isEmpty()) body.put(key, new BigDecimal(val));
    }

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
