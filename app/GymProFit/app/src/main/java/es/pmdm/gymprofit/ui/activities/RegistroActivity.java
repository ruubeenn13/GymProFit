package es.pmdm.gymprofit.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

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
import es.pmdm.gymprofit.utils.LoadingDialog;
import es.pmdm.gymprofit.utils.PoliticaCuenta;
import es.pmdm.gymprofit.utils.PreferencesManager;
import es.pmdm.gymprofit.utils.UIHelper;
import es.pmdm.gymprofit.utils.UiFeedback;

// ============================================================
// RegistroActivity — Pantalla de registro de un nuevo usuario.
// Valida los campos del formulario, crea la cuenta en la API, hace login
// automático con las credenciales introducidas y encadena hasta el onboarding
// (o al login si algún paso intermedio falla).
// ============================================================
public class RegistroActivity extends AppCompatActivity {

    // Aplica la escala de fuente global de la app (agranda todo el texto uniformemente).
    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(es.pmdm.gymprofit.utils.ScaleUtils.wrap(newBase));
    }

    private TextInputEditText etRegUsername, etRegEmail, etRegPassword, etRegConfirmarPassword;
    private TextInputLayout tilRegUsername, tilRegEmail, tilRegPassword, tilRegConfirmarPassword;
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
        tilRegUsername = findViewById(R.id.tilRegUsername);
        tilRegEmail = findViewById(R.id.tilRegEmail);
        tilRegPassword = findViewById(R.id.tilRegPassword);
        tilRegConfirmarPassword = findViewById(R.id.tilRegConfirmarPassword);
        montarAvisoPrivacidad();
    }

    /**
     * Pinta el aviso de privacidad bajo el botón de crear cuenta, con solo el
     * nombre de la política pulsable (GP-008).
     *
     * <p>Es un aviso y no una casilla de aceptación a propósito: una política de
     * privacidad se <b>informa</b> (art. 13 RGPD), no se acepta. Una casilla de
     * "acepto la política" daría a entender que la base legal es el consentimiento,
     * y la del núcleo del servicio es la ejecución del propio servicio; el
     * consentimiento solo cubre foto, mediciones y notificaciones. Términos de
     * servicio no hay, así que no queda nada que aceptar.
     *
     * <p>El nombre de la política se busca dentro de la frase ya traducida en vez de
     * darlo por hecho al final: en inglés no cae en la misma posición.
     */
    private void montarAvisoPrivacidad() {
        TextView tvAviso = findViewById(R.id.tvRegistroPrivacidad);

        String enlace = getString(R.string.registro_aviso_privacidad_enlace);
        String frase  = getString(R.string.registro_aviso_privacidad, enlace);
        int inicio    = frase.indexOf(enlace);

        SpannableString texto = new SpannableString(frase);
        if (inicio >= 0) {
            // ClickableSpan y no un OnClickListener en toda la vista: TalkBack lo
            // anuncia como enlace dentro de la frase, y el resto del texto no queda
            // convertido en un botón gigante que no lleva a ninguna parte.
            texto.setSpan(new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    UIHelper.abrirUrl(RegistroActivity.this, getString(R.string.url_privacidad));
                }
            }, inicio, inicio + enlace.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            tvAviso.setMovementMethod(LinkMovementMethod.getInstance());
        }

        tvAviso.setText(texto);
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

    /**
     * Valida el formulario con las mismas reglas que la API ({@link PoliticaCuenta})
     * y pone cada error en el campo que lo causa (GP-095).
     *
     * <p>Antes el registro solo pedía 6 caracteres de contraseña: una como
     * «gymprofit1» pasaba aquí, la API la rechazaba con 400 y el usuario veía
     * «Error al crear la cuenta» sin saber qué cambiar. Ahora lo que no aceptaría la
     * API no sale del móvil. Se revisan todos los campos, no solo el primero que
     * falla, para que se vean todos los errores de una vez.
     *
     * @return true si se puede enviar.
     */
    private boolean validarCampos() {
        String username = texto(etRegUsername);
        String email = texto(etRegEmail);
        String password = texto(etRegPassword);
        String confirmar = texto(etRegConfirmarPassword);

        String errUsuario = null;
        if (username.isEmpty()) {
            errUsuario = getString(R.string.error_campo_requerido);
        } else if (!PoliticaCuenta.usuarioValido(username)) {
            errUsuario = getString(R.string.registro_error_usuario_longitud,
                    PoliticaCuenta.USUARIO_MIN, PoliticaCuenta.USUARIO_MAX);
        }

        String errEmail = null;
        if (email.isEmpty()) {
            errEmail = getString(R.string.error_campo_requerido);
        } else if (!PoliticaCuenta.correoLongitudValida(email)) {
            errEmail = getString(R.string.registro_error_email_largo, PoliticaCuenta.CORREO_MAX);
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            errEmail = getString(R.string.registro_email_invalido);
        }

        String errPassword = null;
        if (password.isEmpty()) {
            errPassword = getString(R.string.error_campo_requerido);
        } else if (!PoliticaCuenta.passwordValida(password)) {
            errPassword = getString(R.string.password_politica);
        }

        String errConfirmar = null;
        if (confirmar.isEmpty()) {
            errConfirmar = getString(R.string.error_campo_requerido);
        } else if (!confirmar.equals(password)) {
            errConfirmar = getString(R.string.registro_passwords_no_coinciden);
        }

        tilRegUsername.setError(errUsuario);
        tilRegEmail.setError(errEmail);
        tilRegPassword.setError(errPassword);
        tilRegConfirmarPassword.setError(errConfirmar);

        // El foco va al primer campo con error, que es el que TalkBack lee primero.
        if (errUsuario != null) etRegUsername.requestFocus();
        else if (errEmail != null) etRegEmail.requestFocus();
        else if (errPassword != null) etRegPassword.requestFocus();
        else if (errConfirmar != null) etRegConfirmarPassword.requestFocus();

        return errUsuario == null && errEmail == null && errPassword == null && errConfirmar == null;
    }

    // Texto recortado de un campo; nunca null.
    private static String texto(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    // Llama a la API para crear la cuenta; si tiene éxito, encadena el login
    // automático con las mismas credenciales.
    private void registrar() {
        String username = texto(etRegUsername);
        String email    = texto(etRegEmail);
        String password = texto(etRegPassword);

        Map<String, Object> body = new HashMap<>();
        body.put("username", username);
        body.put("password", password);
        body.put("email", email);

        LoadingDialog.show(this);
        authApi.register(body).enqueue(new ApiCallback<Void>() {
            @Override
            public void onOk(Void ignored) {
                LoadingDialog.hide(RegistroActivity.this);
                UIHelper.mostrarToastExito(RegistroActivity.this, getString(R.string.registro_exito));
                hacerLoginAutomatico(username, password);
            }

            @Override
            public void onFail(int code, String message) {
                LoadingDialog.hide(RegistroActivity.this);
                mostrarErrorRegistro(code, message);
            }
        });
    }

    /**
     * Enseña por qué la API no ha creado la cuenta (GP-095). Antes todo acababa en
     * el mismo «Error al crear la cuenta».
     *
     * <p>Un 400 es que el usuario o el correo ya tienen cuenta: la validación local
     * es la de la API, así que no puede ser otra cosa. El código de "cause" dice cuál
     * de los dos y el error va a ese campo; sin código, un aviso que dice que es uno
     * de los dos. Red, 429 y 5xx no dependen de lo escrito y van por el aviso común.
     *
     * @param code   código HTTP, o -1 si no hubo respuesta.
     * @param cuerpo cuerpo de error tal cual.
     */
    private void mostrarErrorRegistro(int code, String cuerpo) {
        if (code != 400) {
            UiFeedback.toastError(this, code, cuerpo);
            return;
        }
        switch (PoliticaCuenta.campoEnUso(cuerpo)) {
            case USUARIO:
                tilRegUsername.setError(getString(R.string.registro_error_usuario_en_uso));
                etRegUsername.requestFocus();
                break;
            case CORREO:
                tilRegEmail.setError(getString(R.string.registro_error_email_en_uso));
                etRegEmail.requestFocus();
                break;
            default:
                UIHelper.mostrarToastError(this, getString(R.string.registro_error_ya_registrado));
                break;
        }
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
}
