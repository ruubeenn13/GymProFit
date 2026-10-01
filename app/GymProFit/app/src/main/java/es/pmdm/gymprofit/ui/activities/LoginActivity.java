package es.pmdm.gymprofit.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;


import java.util.HashMap;
import java.util.Map;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.auth.TokenResponse;
import es.pmdm.gymprofit.model.usuario.Usuario;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.PerfilRemoto;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.AuthApi;
import es.pmdm.gymprofit.network.UsuarioApi;
import es.pmdm.gymprofit.network.UtilREST;
import es.pmdm.gymprofit.utils.PreferencesManager;
import es.pmdm.gymprofit.utils.PushTokenManager;
import es.pmdm.gymprofit.utils.UIHelper;
import es.pmdm.gymprofit.utils.UiFeedback;

// ============================================================
// LoginActivity — pantalla de inicio de sesión de GymProFit.
// Se entra con el correo o el usuario (DEC-036) y la contraseña; sin cuenta, al alta
// nueva (GP-103). Es «Ya tengo cuenta» del lienzo del alta.
// ============================================================
public class LoginActivity extends AppCompatActivity {

    // Aplica la escala de fuente global de la app (agranda todo el texto uniformemente).
    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(es.pmdm.gymprofit.utils.ScaleUtils.wrap(newBase));
    }

    /**
     * Extra booleano: se llega al login porque la API rechazó la sesión por cuenta
     * desactivada (GP-083). Sin él, el usuario solo veía volver el login sin saber por qué.
     */
    public static final String EXTRA_CUENTA_DESACTIVADA = "cuenta_desactivada";
    // Tras cambiar la contraseña, si la app no pudo volver a entrar sola (GP-105).
    public static final String EXTRA_PASSWORD_CAMBIADA = "password_cambiada";
    // El correo con que entrar, desde «Entrar con él» de «Guarda tu plan» (GP-103).
    public static final String EXTRA_USUARIO = "usuario";

    private EditText etUsuario, etPassword;
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

        // En Entrada no hay cuenta: la sesión en curso de la anterior (si la había) deja de
        // verse y su notificación se quita. Sigue en su fichero para cuando vuelva (GP-012).
        es.pmdm.gymprofit.envivo.SesionEnCursoRepositorio.get(this).usarCuenta(prefsManager.getUsuarioId());

        inicializarVistas();
        ((com.google.android.material.appbar.MaterialToolbar) findViewById(R.id.toolbar))
                .setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        // Tras cerrar sesión, Entrar es la única pantalla: atrás lleva a la bienvenida, que
        // tiene «Empezar» y el idioma, en vez de cerrar la app (GP-103).
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isTaskRoot()) startActivity(new Intent(LoginActivity.this, BienvenidaActivity.class));
                finish();
            }
        });
        String usuario = getIntent().getStringExtra(EXTRA_USUARIO);
        if (usuario != null && savedInstanceState == null) etUsuario.setText(usuario);
        configurarEventos();

        // Una sola vez: al recrear la pantalla (cambio de tema) el extra seguiría ahí.
        if (getIntent().getBooleanExtra(EXTRA_CUENTA_DESACTIVADA, false)) {
            getIntent().removeExtra(EXTRA_CUENTA_DESACTIVADA);
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.login_cuenta_desactivada_titulo)
                    .setMessage(R.string.login_cuenta_desactivada_mensaje)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        } else if (getIntent().getBooleanExtra(EXTRA_PASSWORD_CAMBIADA, false)) {
            getIntent().removeExtra(EXTRA_PASSWORD_CAMBIADA);
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.cambiar_password_vuelve_titulo)
                    .setMessage(R.string.cambiar_password_vuelve_mensaje)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        }
    }

    // Referencia los campos de texto del layout.
    private void inicializarVistas() {
        etUsuario = findViewById(R.id.etUsuario);
        etPassword = findViewById(R.id.etPassword);
    }

    // Configura los listeners de login, login como invitado, ir a registro
    // y cambio de tema/idioma.
    private void configurarEventos() {
        // Salida para quien no recuerda la contraseña. Sin esto, olvidarla significaba
        // perder la cuenta: no había ninguna otra forma de volver a entrar.
        findViewById(R.id.tvOlvidastePassword).setOnClickListener(v ->
                startActivity(new Intent(this, RecuperarPasswordActivity.class)));

        findViewById(R.id.btnEntrar).setOnClickListener(v -> {
            String usuario  = etUsuario.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (usuario.isEmpty() || password.isEmpty()) {
                UIHelper.mostrarToastError(this, getString(R.string.error_campos_vacios));
                return;
            }

            hacerLogin(usuario, password);
        });

        // Sin cuenta, el alta nueva: el cuestionario y la cuenta al final (GP-103).
        findViewById(R.id.tvNoTienesCuenta).setOnClickListener(v ->
                startActivity(new Intent(this, AltaActivity.class)));
    }

    // Realiza login como invitado (rol GUEST): guarda token/rol y marca
    // el onboarding como completado, navegando directo a HomeActivity.
    // Sin botón desde la 1.5.1 (GP-103): el plan ya se ve sin cuenta y el invitado era una
    // cuenta compartida. El código se queda hasta que GP-150 lo retire con la API.
    @SuppressWarnings("unused")
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

                startActivity(new Intent(LoginActivity.this, MainActivity.class)
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
                // Red, límite de intentos y servidor no son culpa de las credenciales: decir
                // «credenciales incorrectas» ante un 429 invita a reintentar enseguida (GP-096).
                if (code == -1 || code == 429 || code >= 500) {
                    UiFeedback.toastError(LoginActivity.this, code, message);
                } else {
                    UIHelper.mostrarToastError(LoginActivity.this, getString(R.string.login_error_credenciales));
                }
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

                    // Sexo y actividad de la API al móvil, o del móvil a la API si solo
                    // están aquí (GP-111). Antes de marcar el onboarding: esa marca cuenta
                    // para decidir de quién es el perfil de una instalación antigua.
                    PerfilRemoto.alEntrar(LoginActivity.this, u);

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
        // Se mira el flag POR USUARIO, no el global: en un movil compartido, que
        // una persona hubiera terminado el asistente le saltaba el onboarding a la
        // siguiente que iniciara sesion, y entraba sin calorias ni objetivo.
        if (prefsManager.isOnboardingCompletadoParaUsuario(prefsManager.getUsername())) {
            startActivity(new Intent(this, MainActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        } else {
            // Una cuenta sin onboarding responde lo mismo que el alta y acaba en «Empezar».
            startActivity(AltaActivity.paraCuentaExistente(this)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        }
        finish();
    }
}
