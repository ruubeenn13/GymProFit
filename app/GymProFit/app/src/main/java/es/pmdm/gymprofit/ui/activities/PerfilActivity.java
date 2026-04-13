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
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;

import java.util.Locale;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.utils.PreferencesManager;
import es.pmdm.gymprofit.utils.UIHelper;

public class PerfilActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private PreferencesManager prefsManager;
    private TextView tvTemaActual, tvIdiomaActual;
    private ImageView ivIconoTema;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefsManager = new PreferencesManager(this);
        prefsManager.applyTheme();
        aplicarIdiomaGuardado();

        setContentView(R.layout.activity_perfil);

        inicializarVistas();
        configurarDatosUsuario();
        configurarConfiguracion();
        configurarBotones();
        configurarNavegacion();
    }

    private void inicializarVistas() {
        tvTemaActual = findViewById(R.id.tvTemaActual);
        tvIdiomaActual = findViewById(R.id.tvIdiomaActual);
        ivIconoTema = findViewById(R.id.ivIconoTema);
    }

    private void configurarDatosUsuario() {
        String sinDatos = getString(R.string.perfil_sin_datos);
        ((TextView) findViewById(R.id.tvInfoNivel)).setText(sinDatos);
        ((TextView) findViewById(R.id.tvInfoPeso)).setText(sinDatos);
        ((TextView) findViewById(R.id.tvInfoAltura)).setText(sinDatos);
        ((TextView) findViewById(R.id.tvInfoEdad)).setText(sinDatos);
        ((TextView) findViewById(R.id.tvInfoObjetivo)).setText(sinDatos);
    }

    private void configurarConfiguracion() {
        int temaActual = prefsManager.getTheme();
        if (temaActual == AppCompatDelegate.MODE_NIGHT_YES) {
            tvTemaActual.setText(R.string.tema_oscuro);
            ivIconoTema.setImageResource(R.drawable.ic_moon);
        } else {
            tvTemaActual.setText(R.string.tema_claro);
            ivIconoTema.setImageResource(R.drawable.ic_sun);
        }

        String idioma = prefsManager.getLanguage();
        tvIdiomaActual.setText("es".equals(idioma) || idioma.isEmpty() ? getString(R.string.idioma_espanol) : getString(R.string.idioma_ingles));

        findViewById(R.id.itemTema).setOnClickListener(v -> {
            int currentMode = prefsManager.getTheme();
            int newMode = (currentMode == AppCompatDelegate.MODE_NIGHT_YES) ? AppCompatDelegate.MODE_NIGHT_NO : AppCompatDelegate.MODE_NIGHT_YES;
            prefsManager.saveTheme(newMode);
            recreate();
        });

        findViewById(R.id.itemIdioma).setOnClickListener(v -> mostrarDialogoIdioma());
    }

    private void configurarBotones() {
        findViewById(R.id.btnEditarPerfil).setOnClickListener(v ->
                UIHelper.mostrarToastInfo(this, getString(R.string.perfil_editar_proximamente))
        );

        findViewById(R.id.btnCerrarSesion).setOnClickListener(v ->
                UIHelper.mostrarDialogoConIcono(
                        this,
                        getString(R.string.perfil_cerrar_sesion),
                        getString(R.string.dialog_cerrar_sesion_mensaje),
                        R.drawable.ic_logout,
                        () -> {
                            Intent intent = new Intent(this, LoginActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        }
                )
        );
    }

    private void mostrarDialogoIdioma() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_idioma);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setDimAmount(0.5f);
        }

        android.widget.LinearLayout root = dialog.findViewById(R.id.dialogRoot);

        TypedValue typedValue = new TypedValue();

        getTheme().resolveAttribute(android.R.attr.colorBackground, typedValue, true);

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
        } else {
            ivCheckIngles.setVisibility(View.VISIBLE);
        }

        dialog.findViewById(R.id.optionEspanol).setOnClickListener(v -> {
            prefsManager.saveLanguage("es");
            dialog.dismiss();
            recreate();
        });

        dialog.findViewById(R.id.optionIngles).setOnClickListener(v -> {
            prefsManager.saveLanguage("en");
            dialog.dismiss();
            recreate();
        });

        ((MaterialButton) dialog.findViewById(R.id.btnCerrarIdioma))
                .setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void configurarNavegacion() {
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setSelectedItemId(R.id.nav_perfil);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                startActivity(new Intent(this, HomeActivity.class));
                overridePendingTransition(0, 0);
                finish();
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
                return true;
            }
            return false;
        });
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