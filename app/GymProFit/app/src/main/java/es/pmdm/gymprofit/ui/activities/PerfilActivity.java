package es.pmdm.gymprofit.ui.activities;

import android.app.Dialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;

import java.util.Locale;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.usuario.Usuario;
import es.pmdm.gymprofit.ui.activities.AdminActivity;
import es.pmdm.gymprofit.ui.activities.LogrosActivity;
import es.pmdm.gymprofit.ui.activities.MedicionesActivity;
import es.pmdm.gymprofit.ui.activities.SesionesActivity;
import es.pmdm.gymprofit.network.API;
import es.pmdm.gymprofit.network.UtilJSONParser;
import es.pmdm.gymprofit.network.UtilREST;
import es.pmdm.gymprofit.utils.PreferencesManager;
import es.pmdm.gymprofit.utils.UIHelper;

public class PerfilActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private PreferencesManager prefsManager;
    private ActivityResultLauncher<Intent> editarPerfilLauncher;
    private TextView tvTemaActual, tvIdiomaActual;
    private TextView tvNombreUsuario, tvEmailUsuario;
    private TextView tvInfoNombre, tvInfoEmail;
    private TextView tvInfoNivel, tvInfoPeso, tvInfoAltura, tvInfoEdad, tvInfoObjetivo;
    private ImageView ivIconoTema;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefsManager = new PreferencesManager(this);
        prefsManager.applyTheme();
        aplicarIdiomaGuardado();

        editarPerfilLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        configurarDatosUsuario();
                    }
                }
        );

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
        tvNombreUsuario = findViewById(R.id.tvNombreUsuario);
        tvEmailUsuario = findViewById(R.id.tvEmailUsuario);
        tvInfoNombre = findViewById(R.id.tvInfoNombre);
        tvInfoEmail = findViewById(R.id.tvInfoEmail);
        tvInfoNivel = findViewById(R.id.tvInfoNivel);
        tvInfoPeso = findViewById(R.id.tvInfoPeso);
        tvInfoAltura = findViewById(R.id.tvInfoAltura);
        tvInfoEdad = findViewById(R.id.tvInfoEdad);
        tvInfoObjetivo = findViewById(R.id.tvInfoObjetivo);
    }

    private void configurarDatosUsuario() {
        String sinDatos = getString(R.string.perfil_sin_datos);
        tvInfoNivel.setText(sinDatos);
        tvInfoPeso.setText(sinDatos);
        tvInfoAltura.setText(sinDatos);
        tvInfoEdad.setText(sinDatos);
        tvInfoObjetivo.setText(sinDatos);

        int usuarioId = prefsManager.getUsuarioId();
        if (usuarioId == -1) return;

        API.getUsuarioPorId(usuarioId, new UtilREST.OnResponseListener() {
            @Override
            public void onSuccess(String response, int statusCode) {
                try {
                    Usuario u = UtilJSONParser.parseUsuario(response);
                    if (u == null) return;

                    runOnUiThread(() -> {
                        tvNombreUsuario.setText(u.getUsername());
                        tvEmailUsuario.setText(val(u.getEmail(), sinDatos));
                        tvInfoNombre.setText(u.getUsername());
                        tvInfoEmail.setText(val(u.getEmail(), sinDatos));
                        tvInfoNivel.setText(val(u.getNivelExperiencia(), null) != null
                                ? mapearNivel(u.getNivelExperiencia()) : sinDatos);
                        tvInfoPeso.setText(val(u.getPeso(), null) != null
                                ? getString(R.string.perfil_kg, u.getPeso()) : sinDatos);
                        tvInfoAltura.setText(u.getAltura() > 0
                                ? getString(R.string.perfil_cm, (int) u.getAltura()) : sinDatos);
                        tvInfoEdad.setText(u.getEdad() > 0
                                ? getString(R.string.perfil_anos, u.getEdad()) : sinDatos);
                        tvInfoObjetivo.setText(val(u.getObjetivo(), sinDatos) != null
                                ? mapearObjetivo(u.getObjetivo()) : sinDatos);
                    });
                } catch (Exception ignored) {}
            }

            @Override
            public void onError(String message, int statusCode) {
                Log.e("GymProFit", "getUsuarioPorId error status=" + statusCode + " msg=" + message);
                runOnUiThread(() -> {
                    String username = prefsManager.getUsername();
                    if (username != null && !username.isEmpty()) {
                        tvNombreUsuario.setText(username);
                        tvInfoNombre.setText(username);
                    }
                });
            }
        });
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
                editarPerfilLauncher.launch(new Intent(this, EditarPerfilActivity.class))
        );

        findViewById(R.id.itemSesiones).setOnClickListener(v ->
                startActivity(new Intent(this, SesionesActivity.class)));

        findViewById(R.id.itemMediciones).setOnClickListener(v ->
                startActivity(new Intent(this, MedicionesActivity.class)));

        findViewById(R.id.itemLogros).setOnClickListener(v ->
                startActivity(new Intent(this, LogrosActivity.class)));

        View itemAdmin = findViewById(R.id.itemAdmin);
        if (prefsManager.isAdmin()) {
            itemAdmin.setVisibility(View.VISIBLE);
            itemAdmin.setOnClickListener(v ->
                    startActivity(new Intent(this, AdminActivity.class)));
        } else {
            itemAdmin.setVisibility(View.GONE);
        }

        findViewById(R.id.btnCerrarSesion).setOnClickListener(v ->
                UIHelper.mostrarDialogoConIcono(
                        this,
                        getString(R.string.perfil_cerrar_sesion),
                        getString(R.string.dialog_cerrar_sesion_mensaje),
                        R.drawable.ic_logout,
                        () -> {
                            UtilREST.clearToken();
                            prefsManager.cerrarSesion();
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

    private String val(String s, String fallback) {
        return (s != null && !s.isEmpty() && !"null".equals(s)) ? s : fallback;
    }

    private String mapearNivel(String nivel) {
        if (nivel == null) return getString(R.string.perfil_sin_datos);
        switch (nivel) {
            case "PRINCIPIANTE": return getString(R.string.nivel_principiante);
            case "INTERMEDIO":   return getString(R.string.nivel_intermedio);
            case "AVANZADO":     return getString(R.string.nivel_avanzado);
            default:             return nivel;
        }
    }

    private String mapearObjetivo(String objetivo) {
        if (objetivo == null) return getString(R.string.perfil_sin_datos);
        switch (objetivo) {
            case "PERDER_PESO":             return getString(R.string.objetivo_perder_peso);
            case "GANAR_MASA_MUSCULAR":     return getString(R.string.objetivo_ganar_musculo);
            case "MANTENER_PESO":           return getString(R.string.objetivo_mantener);
            case "MEJORAR_RESISTENCIA":     return getString(R.string.objetivo_resistencia);
            case "MEJORAR_FUERZA":          return getString(R.string.objetivo_fuerza);
            case "REDUCIR_GRASA_CORPORAL":  return getString(R.string.objetivo_reducir_grasa);
            case "MEJORAR_FLEXIBILIDAD":    return getString(R.string.objetivo_flexibilidad);
            case "MEJORAR_VELOCIDAD":       return getString(R.string.objetivo_velocidad);
            case "AUMENTAR_CALORIAS":       return getString(R.string.objetivo_aumentar_calorias);
            case "MEJORAR_MOVILIDAD":       return getString(R.string.objetivo_movilidad);
            default:                        return objetivo;
        }
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