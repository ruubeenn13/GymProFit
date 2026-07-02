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
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.button.MaterialButton;

import org.json.JSONException;

import java.util.Locale;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.usuario.Usuario;
import es.pmdm.gymprofit.network.API;
import es.pmdm.gymprofit.network.UtilJSONParser;
import es.pmdm.gymprofit.network.UtilREST;
import es.pmdm.gymprofit.utils.PreferencesManager;
import es.pmdm.gymprofit.utils.UIHelper;

// ============================================================
// LoginActivity — pantalla de inicio de sesión de GymProFit.
// Permite autenticarse con usuario/contraseña, entrar como invitado,
// ir al registro y cambiar tema/idioma antes de acceder a la app.
// ============================================================
public class LoginActivity extends AppCompatActivity {

    private EditText etUsuario, etPassword;
    private ImageButton btnCambiarTema, btnCambiarIdioma;
    private PreferencesManager prefsManager;

    // Aplica tema e idioma guardados antes de inflar el layout y configura
    // vistas, eventos e icono de tema.
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

    // Referencia los campos de texto y botones de tema/idioma del layout.
    private void inicializarVistas() {
        etUsuario = findViewById(R.id.etUsuario);
        etPassword = findViewById(R.id.etPassword);
        btnCambiarTema = findViewById(R.id.btnCambiarTema);
        btnCambiarIdioma = findViewById(R.id.btnCambiarIdioma);
    }

    // Configura los listeners de login, login como invitado, ir a registro
    // y cambio de tema/idioma.
    private void configurarEventos() {
        findViewById(R.id.btnEntrar).setOnClickListener(v -> {
            String usuario  = etUsuario.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (usuario.isEmpty() || password.isEmpty()) {
                UIHelper.mostrarToastError(this, getString(R.string.error_campos_vacios));
                return;
            }

            hacerLogin(usuario, password);
        });

        findViewById(R.id.btnEntrarInvitado).setOnClickListener(v -> hacerLoginInvitado());

        findViewById(R.id.tvNoTienesCuenta).setOnClickListener(v ->
                startActivity(new Intent(this, RegistroActivity.class)));

        btnCambiarTema.setOnClickListener(v -> cambiarTema());
        btnCambiarIdioma.setOnClickListener(v -> mostrarDialogoIdioma());
    }

    // Realiza login como invitado (rol GUEST): guarda token/rol y marca
    // el onboarding como completado, navegando directo a HomeActivity.
    private void hacerLoginInvitado() {
        API.loginAsGuest(new UtilREST.OnResponseListener() {
            @Override
            public void onSuccess(String response, int statusCode) {
                try {
                    String token = UtilJSONParser.parseToken(response);
                    String user  = UtilJSONParser.parseTokenUsername(response);
                    String rol   = UtilJSONParser.parseTokenRol(response);

                    UtilREST.setToken(token);
                    prefsManager.saveToken(token);
                    prefsManager.saveUsername(user);
                    prefsManager.saveRol(rol);
                    prefsManager.setOnboardingCompletado(true);

                    startActivity(new Intent(LoginActivity.this, HomeActivity.class)
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                    finish();
                } catch (JSONException e) {
                    UIHelper.mostrarToastError(LoginActivity.this, getString(R.string.login_error_invitado));
                }
            }

            @Override
            public void onError(String message, int statusCode) {
                UIHelper.mostrarToastError(LoginActivity.this, getString(R.string.login_error_invitado));
            }
        });
    }

    // Realiza el login normal con credenciales, guarda token/usuario/rol
    // y continúa obteniendo los datos completos del usuario.
    private void hacerLogin(String username, String password) {
        API.login(username, password, new UtilREST.OnResponseListener() {
            @Override
            public void onSuccess(String response, int statusCode) {
                try {
                    String token = UtilJSONParser.parseToken(response);
                    String user  = UtilJSONParser.parseTokenUsername(response);
                    String rol   = UtilJSONParser.parseTokenRol(response);

                    UtilREST.setToken(token);
                    prefsManager.saveToken(token);
                    prefsManager.saveUsername(user);
                    prefsManager.saveRol(rol);

                    obtenerUsuario(user);
                } catch (JSONException e) {
                    UIHelper.mostrarToastError(LoginActivity.this, getString(R.string.login_error_credenciales));
                }
            }

            @Override
            public void onError(String message, int statusCode) {
                UIHelper.mostrarToastError(LoginActivity.this, getString(R.string.login_error_credenciales));
            }
        });
    }

    // Obtiene el usuario por username para guardar su id y determinar si ya
    // completó el onboarding (admin, nivel de experiencia definido o flag local).
    private void obtenerUsuario(String username) {
        API.getUsuarioPorUsername(username, new UtilREST.OnResponseListener() {
            @Override
            public void onSuccess(String response, int statusCode) {
                try {
                    Usuario u = UtilJSONParser.parseUsuario(response);
                    prefsManager.saveUsuarioId(u.getId());

                    boolean yaCompleto = prefsManager.isAdmin()
                            || (u.getNivelExperiencia() != null && !u.getNivelExperiencia().isEmpty())
                            || prefsManager.isOnboardingCompletadoParaUsuario(username);
                    if (yaCompleto) {
                        prefsManager.setOnboardingCompletado(true);
                        prefsManager.setOnboardingCompletadoParaUsuario(username);
                    }
                } catch (JSONException e) {
                    // continúa sin guardar id/rol
                }
                navegarTrasLogin();
            }

            @Override
            public void onError(String message, int statusCode) {
                if (prefsManager.isOnboardingCompletadoParaUsuario(username)) {
                    prefsManager.setOnboardingCompletado(true);
                }
                navegarTrasLogin();
            }
        });
    }

    // Navega a HomeActivity si el onboarding ya está completo, o al primer
    // paso del onboarding en caso contrario.
    private void navegarTrasLogin() {
        if (prefsManager.isOnboardingCompletado()) {
            startActivity(new Intent(this, HomeActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        } else {
            Intent intent = new Intent(this, Onboarding1Activity.class);
            intent.putExtra("username", prefsManager.getUsername());
            startActivity(intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        }
        finish();
    }

    // Alterna entre tema claro y oscuro, guarda la preferencia y recrea la Activity.
    private void cambiarTema() {
        int currentMode = prefsManager.getTheme();
        int newMode = (currentMode == AppCompatDelegate.MODE_NIGHT_YES)
                ? AppCompatDelegate.MODE_NIGHT_NO
                : AppCompatDelegate.MODE_NIGHT_YES;
        prefsManager.saveTheme(newMode);
        recreate();
    }

    // Actualiza el icono del botón de tema (sol/luna) según el modo actual.
    private void actualizarIconoTema() {
        int currentMode = prefsManager.getTheme();
        btnCambiarTema.setImageResource(
                currentMode == AppCompatDelegate.MODE_NIGHT_YES
                        ? R.drawable.ic_sun
                        : R.drawable.ic_moon);
    }

    // Muestra un diálogo personalizado para seleccionar el idioma (español/inglés),
    // marcando el check del idioma actualmente activo.
    private void mostrarDialogoIdioma() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_idioma);

        if (dialog.getWindow() != null) {
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
        ImageView ivCheckIngles  = dialog.findViewById(R.id.ivCheckIngles);

        if ("es".equals(idiomaActual) || idiomaActual.isEmpty()) {
            ivCheckEspanol.setVisibility(View.VISIBLE);
        } else if ("en".equals(idiomaActual)) {
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
        ((MaterialButton) dialog.findViewById(R.id.btnCerrarIdioma))
                .setOnClickListener(v -> dialog.dismiss());

        dialog.show();
        if (dialog.getWindow() != null) {
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.88);
            dialog.getWindow().setLayout(width, android.view.WindowManager.LayoutParams.WRAP_CONTENT);
        }
    }

    // Guarda el idioma seleccionado y recrea la Activity para aplicarlo.
    private void cambiarIdioma(String languageCode) {
        prefsManager.saveLanguage(languageCode);
        recreate();
    }

    // Aplica el idioma guardado en preferencias a la configuración de recursos,
    // si existe uno guardado previamente.
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
