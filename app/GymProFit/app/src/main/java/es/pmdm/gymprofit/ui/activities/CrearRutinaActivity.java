package es.pmdm.gymprofit.ui.activities;

import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Locale;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.utils.PreferencesManager;
import es.pmdm.gymprofit.utils.UIHelper;

public class CrearRutinaActivity extends AppCompatActivity {

    private TextInputEditText etNombre, etDescripcion, etDuracion;
    private ChipGroup chipGroupNivel;
    private PreferencesManager prefsManager;

    private ActivityResultLauncher<Intent> anadirLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefsManager = new PreferencesManager(this);
        prefsManager.applyTheme();
        aplicarIdiomaGuardado();
        setContentView(R.layout.activity_crear_rutina);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        etNombre       = findViewById(R.id.etNombre);
        etDescripcion  = findViewById(R.id.etDescripcion);
        etDuracion     = findViewById(R.id.etDuracion);
        chipGroupNivel = findViewById(R.id.chipGroupNivel);

        anadirLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        setResult(RESULT_OK);
                        finish();
                    } else if (result.getResultCode() == android.app.Activity.RESULT_FIRST_USER) {
                        finish();
                    }
                });

        findViewById(R.id.btnSiguiente).setOnClickListener(v -> siguiente());
    }

    private void siguiente() {
        String nombre = etNombre.getText().toString().trim();
        String desc   = etDescripcion.getText().toString().trim();
        String dur    = etDuracion.getText().toString().trim();

        if (nombre.isEmpty()) { error(etNombre); return; }
        if (desc.isEmpty())   { error(etDescripcion); return; }
        if (dur.isEmpty())    { error(etDuracion); return; }

        Intent intent = new Intent(this, AnadirEjerciciosActivity.class);
        intent.putExtra("nombre", nombre);
        intent.putExtra("descripcion", desc);
        intent.putExtra("nivel", obtenerNivel());
        intent.putExtra("duracion", Integer.parseInt(dur));
        anadirLauncher.launch(intent);
    }

    private void error(TextInputEditText campo) {
        UIHelper.mostrarToastError(this, getString(R.string.error_campo_requerido));
        campo.requestFocus();
    }

    private String obtenerNivel() {
        int id = chipGroupNivel.getCheckedChipId();
        if (id == R.id.chipIntermedio) return "INTERMEDIO";
        if (id == R.id.chipAvanzado)   return "AVANZADO";
        return "PRINCIPIANTE";
    }

    private void aplicarIdiomaGuardado() {
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
