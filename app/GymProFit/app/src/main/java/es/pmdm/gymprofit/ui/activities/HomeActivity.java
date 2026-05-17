package es.pmdm.gymprofit.ui.activities;

import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.utils.PreferencesManager;

public class HomeActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferencesManager prefsManager = new PreferencesManager(this);
        prefsManager.applyTheme();
        aplicarIdiomaGuardado(prefsManager);

        setContentView(R.layout.activity_home);

        configurarCabecera(prefsManager);
        configurarAccionesRapidas();
        configurarNavegacion();
    }

    private void configurarCabecera(PreferencesManager prefsManager) {
        TextView tvSaludo  = findViewById(R.id.tvSaludo);
        TextView tvUsuario = findViewById(R.id.tvUsuario);
        TextView tvFecha   = findViewById(R.id.tvFecha);

        int hora = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hora < 12) {
            tvSaludo.setText(R.string.home_buenos_dias);
        } else if (hora < 20) {
            tvSaludo.setText(R.string.home_buenas_tardes);
        } else {
            tvSaludo.setText(R.string.home_buenas_noches);
        }

        String username = prefsManager.getUsername();
        tvUsuario.setText(username.isEmpty() ? getString(R.string.home_usuario_defecto) : username);

        Locale locale = Locale.getDefault();
        String pattern = "en".equals(locale.getLanguage())
                ? "EEEE, MMMM d"
                : "EEEE, d 'de' MMMM";
        SimpleDateFormat sdf = new SimpleDateFormat(pattern, locale);
        tvFecha.setText(sdf.format(new Date()));
    }

    private void configurarAccionesRapidas() {
        MaterialCardView cardIniciarEntrenamiento = findViewById(R.id.cardIniciarEntrenamiento);
        MaterialCardView cardVerRutinas = findViewById(R.id.cardVerRutinas);
        MaterialCardView cardRegistrarComida = findViewById(R.id.cardRegistrarComida);

        cardIniciarEntrenamiento.setOnClickListener(v -> {
            startActivity(new Intent(this, SesionesActivity.class));
            overridePendingTransition(0, 0);
        });

        cardVerRutinas.setOnClickListener(v -> {
            startActivity(new Intent(this, RutinasActivity.class));
            overridePendingTransition(0, 0);
        });

        cardRegistrarComida.setOnClickListener(v -> {
            startActivity(new Intent(this, NutricionActivity.class));
            overridePendingTransition(0, 0);
        });
    }

    private void configurarNavegacion() {
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setSelectedItemId(R.id.nav_home);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                return true;
            } else if (itemId == R.id.nav_rutinas) {
                startActivity(new Intent(this, RutinasActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_ejercicios) {
                startActivity(new Intent(this, EjerciciosActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_nutricion) {
                startActivity(new Intent(this, NutricionActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_perfil) {
                startActivity(new Intent(this, PerfilActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }

            return false;
        });
    }

    private void aplicarIdiomaGuardado(PreferencesManager prefsManager) {
        String savedLanguage = prefsManager.getLanguage();
        if (!savedLanguage.isEmpty()) {
            Locale locale = new Locale(savedLanguage);
            Locale.setDefault(locale);
            Resources resources = getResources();
            Configuration config = resources.getConfiguration();
            config.setLocale(locale);
            resources.updateConfiguration(config, resources.getDisplayMetrics());
        }
    }
}