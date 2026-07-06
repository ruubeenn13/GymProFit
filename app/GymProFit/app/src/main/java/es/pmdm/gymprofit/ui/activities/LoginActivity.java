package es.pmdm.gymprofit.ui.activities;

import android.app.Dialog;
import android.content.Intent;
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

import java.util.HashMap;
import java.util.Map;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.auth.TokenResponse;
import es.pmdm.gymprofit.model.usuario.Usuario;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.AuthApi;
import es.pmdm.gymprofit.network.UsuarioApi;
import es.pmdm.gymprofit.network.UtilREST;
import es.pmdm.gymprofit.utils.PreferencesManager;
import es.pmdm.gymprofit.utils.PushTokenManager;
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
    // Interfaces Retrofit tipadas de auth y usuarios (etapa 2)
    private final AuthApi authApi = ApiClient.service(AuthApi.class);
    private final UsuarioApi usuarioApi = ApiClient.service(UsuarioApi.class);

    // Aplica tema e idioma guardados antes de inflar el layout y configura
    // vistas, eventos e icono de tema.
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
        authApi.guest().enqueue(new ApiCallback<TokenResponse>() {
            @Override
            public void onOk(TokenResponse body) {
                if (body == null) {
                    UIHelper.mostrarToastError(LoginActivity.this, getString(R.string.login_error_invitado));
                    return;
                }
                String token   = body.getToken();
                String refresh = body.getRefreshToken();
                String user    = body.getUsername();
                String rol     = body.rolPrincipal();

                // Guarda los tokens en UtilREST (memoria, para interceptor/authenticator)
                // y en preferencias (cifrado, para sobrevivir a reinicios), igual que antes.
                UtilREST.setToken(token);
                UtilREST.setRefreshToken(refresh);
                prefsManager.saveSesion(token, refresh);
                prefsManager.saveUsername(user);
                prefsManager.saveRol(rol);
                prefsManager.setOnboardingCompletado(true);

                startActivity(new Intent(LoginActivity.this, HomeActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                finish();
            }

            @Override
            public void onFail(int code, String message) {
                UIHelper.mostrarToastError(LoginActivity.this, getString(R.string.login_error_invitado));
            }
        });
    }

    // Realiza el login normal con credenciales, guarda token/usuario/rol
    // y continúa obteniendo los datos completos del usuario.
    private void hacerLogin(String username, String password) {
        Map<String, Object> body = new HashMap<>();
        body.put("username", username);
        body.put("password", password);

        authApi.login(body).enqueue(new ApiCallback<TokenResponse>() {
            @Override
            public void onOk(TokenResponse resp) {
                if (resp == null) {
                    UIHelper.mostrarToastError(LoginActivity.this, getString(R.string.login_error_credenciales));
                    return;
                }
                String token   = resp.getToken();
                String refresh = resp.getRefreshToken();
                String user    = resp.getUsername();
                String rol     = resp.rolPrincipal();

                // Guardado de tokens idéntico al flujo original (UtilREST + prefs).
                UtilREST.setToken(token);
                UtilREST.setRefreshToken(refresh);
                prefsManager.saveSesion(token, refresh);
                prefsManager.saveUsername(user);
                prefsManager.saveRol(rol);

                obtenerUsuario(user);
            }

            @Override
            public void onFail(int code, String message) {
                UIHelper.mostrarToastError(LoginActivity.this, getString(R.string.login_error_credenciales));
            }
        });
    }

    // Obtiene el usuario por username para guardar su id y determinar si ya
    // completó el onboarding (admin, nivel de experiencia definido o flag local).
    private void obtenerUsuario(String username) {
        usuarioApi.getPorUsername(username).enqueue(new ApiCallback<Usuario>() {
            @Override
            public void onOk(Usuario u) {
                if (u != null) {
                    prefsManager.saveUsuarioId(u.getId());

                    // Registra el token FCM del dispositivo para recibir push (best-effort).
                    PushTokenManager.registrar(LoginActivity.this);

                    boolean yaCompleto = prefsManager.isAdmin()
                            || (u.getNivelExperiencia() != null && !u.getNivelExperiencia().isEmpty())
                            || prefsManager.isOnboardingCompletadoParaUsuario(username);
                    if (yaCompleto) {
                        prefsManager.setOnboardingCompletado(true);
                        prefsManager.setOnboardingCompletadoParaUsuario(username);
                    }
                }
                navegarTrasLogin();
            }

            @Override
            public void onFail(int code, String message) {
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

    // Guarda el idioma y lo aplica vía AndroidX per-app locales (recrea la Activity solo).
    private void cambiarIdioma(String languageCode) {
        prefsManager.saveLanguage(languageCode);
        androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(
                androidx.core.os.LocaleListCompat.forLanguageTags(languageCode));
    }
}
