package es.pmdm.gymprofit.ui.activities;

import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

import java.util.HashMap;
import java.util.Locale;
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
import es.pmdm.gymprofit.utils.UIHelper;

// ============================================================
// RegistroActivity — Pantalla de registro de un nuevo usuario.
// Valida los campos del formulario, crea la cuenta en la API, hace login
// automático con las credenciales introducidas y encadena hasta el onboarding
// (o al login si algún paso intermedio falla).
// ============================================================
public class RegistroActivity extends AppCompatActivity {

    private TextInputEditText etRegUsername, etRegEmail, etRegPassword, etRegConfirmarPassword;
    private PreferencesManager prefsManager;
    // Interfaces Retrofit tipadas de auth y usuarios (etapa 2)
    private final AuthApi authApi = ApiClient.service(AuthApi.class);
    private final UsuarioApi usuarioApi = ApiClient.service(UsuarioApi.class);

    // Inicializa la pantalla: aplica tema/idioma, monta vistas y configura
    // los listeners de los botones.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefsManager = new PreferencesManager(this);
        prefsManager.applyTheme();
        aplicarIdioma();

        setContentView(R.layout.activity_registro);

        inicializarVistas();
        configurarEventos();
    }

    // Enlaza los campos del formulario de registro con sus vistas del layout.
    private void inicializarVistas() {
        etRegUsername = findViewById(R.id.etRegUsername);
        etRegEmail = findViewById(R.id.etRegEmail);
        etRegPassword = findViewById(R.id.etRegPassword);
        etRegConfirmarPassword = findViewById(R.id.etRegConfirmarPassword);
    }

    // Configura los botones: volver al login y crear cuenta (con validación
    // previa de los campos).
    private void configurarEventos() {
        findViewById(R.id.btnVolverLogin).setOnClickListener(v -> finish());
        findViewById(R.id.tvYaTengoCuenta).setOnClickListener(v -> finish());
        findViewById(R.id.btnCrearCuenta).setOnClickListener(v -> {
            if (!validarCampos()) return;
            registrar();
        });
    }

    // Valida que los campos no estén vacíos, que el email tenga formato
    // válido, que la contraseña tenga longitud mínima y que ambas coincidan.
    private boolean validarCampos() {
        String username = etRegUsername.getText().toString().trim();
        String email = etRegEmail.getText().toString().trim();
        String password = etRegPassword.getText().toString().trim();
        String confirmar = etRegConfirmarPassword.getText().toString().trim();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmar.isEmpty()) {
            UIHelper.mostrarToastError(this, getString(R.string.error_campos_vacios));
            return false;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            UIHelper.mostrarToastError(this, getString(R.string.registro_email_invalido));
            etRegEmail.requestFocus();
            return false;
        }
        if (password.length() < 6) {
            UIHelper.mostrarToastError(this, getString(R.string.registro_password_corta));
            etRegPassword.requestFocus();
            return false;
        }
        if (!password.equals(confirmar)) {
            UIHelper.mostrarToastError(this, getString(R.string.registro_passwords_no_coinciden));
            etRegConfirmarPassword.requestFocus();
            return false;
        }
        return true;
    }

    // Llama a la API para crear la cuenta; si tiene éxito, encadena el login
    // automático con las mismas credenciales.
    private void registrar() {
        String username = etRegUsername.getText().toString().trim();
        String email    = etRegEmail.getText().toString().trim();
        String password = etRegPassword.getText().toString().trim();

        Map<String, Object> body = new HashMap<>();
        body.put("username", username);
        body.put("password", password);
        body.put("email", email);

        authApi.register(body).enqueue(new ApiCallback<Void>() {
            @Override
            public void onOk(Void ignored) {
                UIHelper.mostrarToastExito(RegistroActivity.this, getString(R.string.registro_exito));
                hacerLoginAutomatico(username, password);
            }

            @Override
            public void onFail(int code, String message) {
                UIHelper.mostrarToastError(RegistroActivity.this, getString(R.string.registro_error));
            }
        });
    }

    // Hace login con las credenciales recién registradas, guarda el token y
    // el username en preferencias y continúa obteniendo los datos del usuario.
    private void hacerLoginAutomatico(String username, String password) {
        Map<String, Object> body = new HashMap<>();
        body.put("username", username);
        body.put("password", password);

        authApi.login(body).enqueue(new ApiCallback<TokenResponse>() {
            @Override
            public void onOk(TokenResponse resp) {
                if (resp == null) {
                    irAlLogin();
                    return;
                }
                String token   = resp.getToken();
                String refresh = resp.getRefreshToken();
                String user    = resp.getUsername();

                // Guardado de tokens idéntico al flujo original (UtilREST + prefs);
                // aquí NO se guarda el rol (se obtiene luego del perfil de usuario).
                UtilREST.setToken(token);
                UtilREST.setRefreshToken(refresh);
                prefsManager.saveSesion(token, refresh);
                prefsManager.saveUsername(user);

                obtenerUsuario(user);
            }

            @Override
            public void onFail(int code, String message) {
                irAlLogin();
            }
        });
    }

    // Recupera el usuario recién creado por username para guardar su id y rol
    // en preferencias, y a continuación navega al onboarding.
    private void obtenerUsuario(String username) {
        usuarioApi.getPorUsername(username).enqueue(new ApiCallback<Usuario>() {
            @Override
            public void onOk(Usuario u) {
                if (u != null) {
                    prefsManager.saveUsuarioId(u.getId());
                    prefsManager.saveRol(u.getRol());
                }
                irAlOnboarding();
            }

            @Override
            public void onFail(int code, String message) {
                irAlOnboarding();
            }
        });
    }

    // Navega al primer paso del onboarding, pasando username y email,
    // limpiando el back stack.
    private void irAlOnboarding() {
        Intent intent = new Intent(this, Onboarding1Activity.class);
        intent.putExtra("username", prefsManager.getUsername());
        intent.putExtra("email", etRegEmail.getText().toString().trim());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // Navega a la pantalla de login limpiando el back stack (usado como
    // fallback si el login automático o la obtención del usuario fallan).
    private void irAlLogin() {
        startActivity(new Intent(this, LoginActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }

    // Aplica el idioma guardado en preferencias a la configuración de recursos
    // de la Activity antes de inflar el layout.
    private void aplicarIdioma() {
        String lang = prefsManager.getLanguage();
        if (!lang.isEmpty()) {
            Locale locale = new Locale(lang);
            Locale.setDefault(locale);
            Resources r = getResources();
            Configuration c = r.getConfiguration();
            c.setLocale(locale);
            r.updateConfiguration(c, r.getDisplayMetrics());
        }
    }
}
