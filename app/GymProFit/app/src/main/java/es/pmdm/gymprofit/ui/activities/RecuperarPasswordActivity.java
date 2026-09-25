package es.pmdm.gymprofit.ui.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.AuthApi;
import es.pmdm.gymprofit.utils.LoadingDialog;
import es.pmdm.gymprofit.utils.PreferencesManager;
import es.pmdm.gymprofit.utils.UIHelper;
import es.pmdm.gymprofit.utils.UiFeedback;
import com.google.android.material.appbar.MaterialToolbar;

// ============================================================
// RecuperarPasswordActivity — recuperar una contraseña olvidada.
//
// Hasta ahora no había forma de volver a entrar: quien olvidaba la contraseña
// perdía la cuenta y sus entrenamientos con ella.
//
// Son dos pasos en una sola pantalla. Primero se pide el código con el usuario o
// el correo; después se teclea el código que ha llegado por email junto con la
// contraseña nueva. El segundo paso no aparece hasta que el primero ha salido,
// para que no haya cuatro campos vacíos esperando desde el principio.
//
// La API responde lo mismo exista la cuenta o no, así que esta pantalla tampoco
// puede decir "ese usuario no existe": diría lo que el servidor calla a propósito.
// ============================================================
public class RecuperarPasswordActivity extends AppCompatActivity {

    // Misma política que el registro y que la API. Si aquí fuera más laxa, el 400
    // del servidor llegaría sin que el usuario sepa qué ha hecho mal.
    private static final Pattern POLITICA_PASSWORD =
            Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$");

    // Aplica la escala de fuente global de la app, igual que el resto de pantallas de acceso.
    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(es.pmdm.gymprofit.utils.ScaleUtils.wrap(newBase));
    }

    private LinearLayout grupoPaso1, grupoPaso2;
    private TextInputLayout tilIdentificador, tilCodigo, tilPassword;
    private TextInputEditText etIdentificador, etCodigo, etPasswordNueva;
    private android.widget.TextView tvPaso2Explicacion;

    // Identificador con el que se pidió el código; se reutiliza al canjearlo y al reenviar.
    private String identificadorEnviado;

    private final AuthApi authApi = ApiClient.service(AuthApi.class);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        new PreferencesManager(this).applyTheme();
        setContentView(R.layout.activity_recuperar_password);

        inicializarVistas();
        configurarEventos();
    }

    // Referencia los grupos de cada paso y los campos de texto del layout.
    private void inicializarVistas() {
        grupoPaso1          = findViewById(R.id.grupoPaso1);
        grupoPaso2          = findViewById(R.id.grupoPaso2);
        tilIdentificador    = findViewById(R.id.tilIdentificador);
        tilCodigo           = findViewById(R.id.tilCodigo);
        tilPassword         = findViewById(R.id.tilPassword);
        etIdentificador     = findViewById(R.id.etIdentificador);
        etCodigo            = findViewById(R.id.etCodigo);
        etPasswordNueva     = findViewById(R.id.etPasswordNueva);
        tvPaso2Explicacion  = findViewById(R.id.tvPaso2Explicacion);
    }

    // Cablea volver, pedir el código, reenviarlo y restablecer la contraseña.
    private void configurarEventos() {
        ((MaterialToolbar) findViewById(R.id.toolbar)).setNavigationOnClickListener(v -> finish());
        findViewById(R.id.btnEnviarCodigo).setOnClickListener(v -> pedirCodigo());
        findViewById(R.id.btnReenviar).setOnClickListener(v -> reenviarCodigo());
        findViewById(R.id.btnRestablecer).setOnClickListener(v -> restablecer());
    }

    /**
     * Paso 1: pide a la API un código para el usuario o correo tecleado.
     * <p>
     * Al volver se pasa al paso 2 pase lo que pase en el servidor, porque el servidor
     * responde igual exista la cuenta o no. Quedarse en el paso 1 cuando la cuenta no
     * existe sería revelarlo.
     */
    private void pedirCodigo() {
        String identificador = texto(etIdentificador);

        if (identificador.isEmpty()) {
            tilIdentificador.setError(getString(R.string.recuperar_error_identificador));
            return;
        }
        tilIdentificador.setError(null);

        LoadingDialog.show(this);
        enviarSolicitud(identificador, () -> {
            identificadorEnviado = identificador;
            mostrarPaso2(identificador);
        });
    }

    // Reenvía el código para el mismo identificador, sin volver al paso 1.
    private void reenviarCodigo() {
        if (identificadorEnviado == null) return;

        LoadingDialog.show(this);
        enviarSolicitud(identificadorEnviado, () ->
                UIHelper.mostrarToastExito(this, getString(R.string.recuperar_enviar_codigo)));
    }

    // Llamada compartida por pedir y reenviar: misma petición, distinto remate.
    private void enviarSolicitud(String identificador, Runnable alTerminar) {
        Map<String, Object> body = new HashMap<>();
        body.put("identificador", identificador);

        authApi.forgotPassword(body).enqueue(new ApiCallback<Void>() {
            @Override
            public void onOk(Void cuerpo) {
                LoadingDialog.hide(RecuperarPasswordActivity.this);
                alTerminar.run();
            }
            @Override
            public void onFail(int code, String message) {
                // Aquí solo caben errores de red o el límite de peticiones: el servidor
                // devuelve 200 aunque la cuenta no exista.
                LoadingDialog.hide(RecuperarPasswordActivity.this);
                UiFeedback.toastError(RecuperarPasswordActivity.this, code, message);
            }
        });
    }

    // Descubre el paso 2 y esconde el 1, dejando el foco en el campo del código.
    private void mostrarPaso2(String identificador) {
        tvPaso2Explicacion.setText(getString(R.string.recuperar_paso2_explicacion, identificador));
        grupoPaso1.setVisibility(View.GONE);
        grupoPaso2.setVisibility(View.VISIBLE);
        etCodigo.requestFocus();
    }

    /**
     * Paso 2: canjea el código por la contraseña nueva.
     * <p>
     * Las dos validaciones de formato se hacen aquí antes de salir a la red, para que el
     * error salga en el campo que lo causa y no como un toast genérico del 400.
     */
    private void restablecer() {
        String codigo   = texto(etCodigo);
        String password = texto(etPasswordNueva);
        boolean valido  = true;

        if (codigo.length() != 6) {
            tilCodigo.setError(getString(R.string.recuperar_error_codigo));
            valido = false;
        } else {
            tilCodigo.setError(null);
        }

        if (!POLITICA_PASSWORD.matcher(password).matches()) {
            tilPassword.setError(getString(R.string.recuperar_error_password));
            valido = false;
        } else {
            tilPassword.setError(null);
        }

        if (!valido) return;

        Map<String, Object> body = new HashMap<>();
        body.put("identificador", identificadorEnviado);
        body.put("codigo", codigo);
        body.put("newPassword", password);

        LoadingDialog.show(this);
        authApi.resetPassword(body).enqueue(new ApiCallback<Void>() {
            @Override
            public void onOk(Void cuerpo) {
                LoadingDialog.hide(RecuperarPasswordActivity.this);
                UIHelper.mostrarToastExito(RecuperarPasswordActivity.this,
                        getString(R.string.recuperar_ok));
                // Se vuelve al login, que es donde hay que usar la contraseña recién puesta.
                finish();
            }
            @Override
            public void onFail(int code, String message) {
                LoadingDialog.hide(RecuperarPasswordActivity.this);
                // Un código equivocado o caducado llega como 400 y se marca en su campo;
                // el resto (red, límite de peticiones) va al aviso general.
                if (code == 400) {
                    tilCodigo.setError(getString(R.string.recuperar_error_codigo));
                } else {
                    UiFeedback.toastError(RecuperarPasswordActivity.this, code, message);
                }
            }
        });
    }

    private String texto(TextInputEditText campo) {
        return campo.getText() == null ? "" : campo.getText().toString().trim();
    }
}
