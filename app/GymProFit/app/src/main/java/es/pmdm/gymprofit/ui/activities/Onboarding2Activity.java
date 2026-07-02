package es.pmdm.gymprofit.ui.activities;

import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Locale;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.utils.PreferencesManager;
import es.pmdm.gymprofit.utils.UIHelper;

// ============================================================
// Onboarding2Activity — segundo paso del asistente de onboarding.
// Recoge nombre, email, edad y sexo del usuario, precargando los
// valores recibidos del paso anterior, y avanza al siguiente paso.
// ============================================================
public class Onboarding2Activity extends AppCompatActivity {

    private TextInputEditText etNombre, etEmail, etEdad;
    private ChipGroup chipGroupSexo;

    // Aplica tema/idioma, infla el layout, precarga los datos recibidos
    // y configura los botones de siguiente/anterior/saltar.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferencesManager prefs = new PreferencesManager(this);
        prefs.applyTheme();
        aplicarIdioma(prefs);

        setContentView(R.layout.activity_onboarding2);

        etNombre = findViewById(R.id.etNombreOnboarding);
        etEmail = findViewById(R.id.etEmailOnboarding);
        etEdad = findViewById(R.id.etEdadOnboarding);
        chipGroupSexo = findViewById(R.id.chipGroupSexo);

        Bundle extras = getIntent().getExtras();

        if (extras != null) {
            if (extras.getString("username") != null) {
                etNombre.setText(extras.getString("username"));
            }
            if (extras.getString("email") != null) {
                etEmail.setText(extras.getString("email"));
            }
        }

        findViewById(R.id.btnSiguiente2).setOnClickListener(v -> {
            if (etNombre.getText().toString().trim().isEmpty()) {
                UIHelper.mostrarToastError(this, getString(R.string.error_campo_requerido));
                etNombre.requestFocus();
                return;
            }

            Intent intent = new Intent(this, Onboarding3Activity.class);

            if (extras != null) {
                intent.putExtras(extras);
            }

            intent.putExtra("nombre", etNombre.getText().toString().trim());
            intent.putExtra("email", etEmail.getText().toString().trim());

            String edadStr = etEdad.getText().toString().trim();
            if (!edadStr.isEmpty()) {
                intent.putExtra("edad", Integer.parseInt(edadStr));
            }

            String sexo = (chipGroupSexo.getCheckedChipId() == R.id.chipMujer) ? "MUJER" : "HOMBRE";
            intent.putExtra("sexo", sexo);

            startActivity(intent);
        });

        findViewById(R.id.btnAnterior2).setOnClickListener(v -> finish());
        findViewById(R.id.tvSaltar2).setOnClickListener(v -> saltarAlHome());
    }

    // Salta el onboarding y navega directo a HomeActivity, limpiando el back stack.
    private void saltarAlHome() {
        startActivity(new Intent(this, HomeActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }

    // Aplica el idioma guardado en preferencias a la configuración de recursos.
    private void aplicarIdioma(PreferencesManager prefs) {
        String lang = prefs.getLanguage();

        if (!lang.isEmpty()) {
            Locale locale = new Locale(lang);
            Locale.setDefault(locale);

            Resources resources = getResources();
            Configuration configuration = resources.getConfiguration();
            configuration.setLocale(locale);
            resources.updateConfiguration(configuration, resources.getDisplayMetrics());
        }
    }
}