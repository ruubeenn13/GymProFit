package es.pmdm.gymprofit.ui.activities;

import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import java.util.Locale;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.utils.PreferencesManager;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsuario, etPassword;
    private Button btnEntrar;
    private TextView tvNoTienesCuenta;
    private ImageButton btnCambiarTema, btnCambiarIdioma;
    private PreferencesManager prefsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefsManager = new PreferencesManager(this);
        prefsManager.applyTheme();

        setContentView(R.layout.activity_login);

        inicializarVistas();
        configurarEventos();
        actualizarIconoTema();
    }

    private void inicializarVistas() {
        etUsuario = findViewById(R.id.etUsuario);
        etPassword = findViewById(R.id.etPassword);
        btnEntrar = findViewById(R.id.btnEntrar);
        tvNoTienesCuenta = findViewById(R.id.tvNoTienesCuenta);
        btnCambiarTema = findViewById(R.id.btnCambiarTema);
        btnCambiarIdioma = findViewById(R.id.btnCambiarIdioma);
    }

    private void configurarEventos() {
        btnEntrar.setOnClickListener(v -> {
            String usuario = etUsuario.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (usuario.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, R.string.error_campos_vacios, Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        tvNoTienesCuenta.setOnClickListener(v -> {
            Toast.makeText(this, R.string.registro_proximamente, Toast.LENGTH_SHORT).show();
        });

        btnCambiarTema.setOnClickListener(v -> {
            cambiarTema();
        });

        btnCambiarIdioma.setOnClickListener(v -> {
            mostrarDialogoIdioma();
        });
    }

    private void cambiarTema() {
        int currentMode = prefsManager.getTheme();
        int newMode;

        if (currentMode == AppCompatDelegate.MODE_NIGHT_YES) {
            newMode = AppCompatDelegate.MODE_NIGHT_NO;
        } else {
            newMode = AppCompatDelegate.MODE_NIGHT_YES;
        }

        prefsManager.saveTheme(newMode);
        AppCompatDelegate.setDefaultNightMode(newMode);

        actualizarIconoTema();
    }

    private void actualizarIconoTema() {
        int currentMode = prefsManager.getTheme();

        if (currentMode == AppCompatDelegate.MODE_NIGHT_YES) {
            btnCambiarTema.setImageResource(R.drawable.ic_sun);
        } else {
            btnCambiarTema.setImageResource(R.drawable.ic_moon);
        }
    }

    private void mostrarDialogoIdioma() {
        String[] idiomas = {
                getString(R.string.idioma_espanol),
                getString(R.string.idioma_ingles)
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.cambiar_idioma);
        builder.setItems(idiomas, (dialog, which) -> {
            String languageCode = (which == 0) ? "es" : "en";
            cambiarIdioma(languageCode);
        });
        builder.show();
    }

    private void cambiarIdioma(String languageCode) {
        prefsManager.saveLanguage(languageCode);

        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Resources resources = getResources();
        Configuration config = resources.getConfiguration();
        config.setLocale(locale);
        resources.updateConfiguration(config, resources.getDisplayMetrics());

        Intent intent = getIntent();
        finish();
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();

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