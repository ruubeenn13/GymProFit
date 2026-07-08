package es.pmdm.gymprofit.ui.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.utils.PreferencesManager;
import es.pmdm.gymprofit.utils.UIHelper;

// ============================================================
// Onboarding3Activity — tercer paso del asistente de onboarding.
// Recoge peso, altura y nivel de actividad física del usuario y
// avanza al siguiente paso arrastrando los datos ya introducidos.
// ============================================================
public class Onboarding3Activity extends AppCompatActivity {

    private TextInputEditText etPeso, etAltura;
    private ChipGroup chipGroupActividad;

    // Aplica tema/idioma, infla el layout y configura los botones de
    // siguiente/anterior/saltar, validando peso y altura antes de avanzar.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferencesManager prefs = new PreferencesManager(this);
        prefs.applyTheme();

        setContentView(R.layout.activity_onboarding3);

        etPeso = findViewById(R.id.etPesoOnboarding);
        etAltura = findViewById(R.id.etAlturaOnboarding);
        chipGroupActividad = findViewById(R.id.chipGroupActividad);

        Bundle extras = getIntent().getExtras();

        findViewById(R.id.btnSiguiente3).setOnClickListener(v -> {
            if (etPeso.getText().toString().trim().isEmpty() || etAltura.getText().toString().trim().isEmpty()) {
                UIHelper.mostrarToastError(this, getString(R.string.error_campo_requerido));
                return;
            }

            Intent intent = new Intent(this, Onboarding4Activity.class);

            if (extras != null) {
                intent.putExtras(extras);
            }

            intent.putExtra("peso", etPeso.getText().toString().trim());
            intent.putExtra("altura", Double.parseDouble(etAltura.getText().toString().trim()));

            int checkedId = chipGroupActividad.getCheckedChipId();
            String actividad;

            if (checkedId == R.id.chipSedentario) {
                actividad = "SEDENTARIO";
            } else if (checkedId == R.id.chipLigero) {
                actividad = "LIGERO";
            } else if (checkedId == R.id.chipActivo) {
                actividad = "ACTIVO";
            } else {
                actividad = "MODERADO";
            }

            intent.putExtra("actividad", actividad);
            startActivity(intent);
        });

        findViewById(R.id.btnAnterior3).setOnClickListener(v -> finish());
        findViewById(R.id.tvSaltar3).setOnClickListener(v -> saltarAlHome());
    }

    // Salta el onboarding y navega directo a HomeActivity, limpiando el back stack.
    private void saltarAlHome() {
        startActivity(new Intent(this, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));

        finish();
    }
}