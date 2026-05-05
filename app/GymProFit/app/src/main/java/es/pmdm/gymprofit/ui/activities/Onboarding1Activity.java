package es.pmdm.gymprofit.ui.activities;

import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.utils.PreferencesManager;

public class Onboarding1Activity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferencesManager prefs = new PreferencesManager(this);
        prefs.applyTheme();
        aplicarIdioma(prefs);

        setContentView(R.layout.activity_onboarding1);

        String username = getIntent().getStringExtra("username");

        TextView tvNombre = findViewById(R.id.tvBienvenidaNombre);

        if (username != null && !username.isEmpty()) {
            tvNombre.setText(username + "!");
        }

        findViewById(R.id.btnEmpezar).setOnClickListener(v -> {
            Intent intent = new Intent(this, Onboarding2Activity.class);
            if (getIntent().getExtras() != null) {
                intent.putExtras(getIntent().getExtras());
            }
            startActivity(intent);
        });

        findViewById(R.id.tvSaltar1).setOnClickListener(v -> saltarAlHome());
    }

    private void saltarAlHome() {
        startActivity(new Intent(this, HomeActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }

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