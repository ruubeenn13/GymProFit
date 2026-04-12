package es.pmdm.gymprofit.ui.activities;

import android.app.Dialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.button.MaterialButton;

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
        aplicarIdiomaGuardado();

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

            Intent intent = new Intent(this, HomeActivity.class);
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
        recreate();
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
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_idioma);

        if(dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setDimAmount(0.5f);
        }

        LinearLayout root = dialog.findViewById(R.id.dialogRoot);

        TypedValue typedValue = new TypedValue();

        getTheme().resolveAttribute(com.google.android.material.R.attr.colorSurface, typedValue, true);

        GradientDrawable fondo = new GradientDrawable();
        fondo.setShape(GradientDrawable.RECTANGLE);
        fondo.setCornerRadius(20 * getResources().getDisplayMetrics().density);
        fondo.setColor(typedValue.data);
        root.setBackground(fondo);

        String idiomaActual = prefsManager.getLanguage();

        ImageView ivCheckEspanol = dialog.findViewById(R.id.ivCheckEspanol);
        ImageView ivCheckIngles = dialog.findViewById(R.id.ivCheckIngles);

        if ("es".equals(idiomaActual) || idiomaActual.isEmpty()) {
            ivCheckEspanol.setVisibility(View.VISIBLE);
        } else if ("en".equals(idiomaActual) || idiomaActual.isEmpty()) {
            ivCheckIngles.setVisibility(View.VISIBLE);
        }

        dialog.findViewById(R.id.optionEspanol).setOnClickListener(v -> {
            cambiarIdioma("es");
            dialog.dismiss();
        });

        dialog.findViewById(R.id.optionIngles).setOnClickListener(v -> {
            cambiarIdioma("en");
            dialog.dismiss();
        });

        MaterialButton btnCerrar = dialog.findViewById(R.id.btnCerrarIdioma);
        btnCerrar.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void cambiarIdioma(String languageCode) {
        prefsManager.saveLanguage(languageCode);
        recreate();
    }

    private void aplicarIdiomaGuardado() {
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