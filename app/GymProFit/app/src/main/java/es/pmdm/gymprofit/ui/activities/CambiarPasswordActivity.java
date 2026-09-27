package es.pmdm.gymprofit.ui.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.HashMap;
import java.util.Map;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.auth.TokenResponse;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.AuthApi;
import es.pmdm.gymprofit.network.UtilREST;
import es.pmdm.gymprofit.utils.AvatarUtils;
import es.pmdm.gymprofit.utils.LoadingDialog;
import es.pmdm.gymprofit.utils.PoliticaCuenta;
import es.pmdm.gymprofit.utils.PreferencesManager;
import es.pmdm.gymprofit.utils.UIHelper;
import es.pmdm.gymprofit.utils.UiFeedback;

// ============================================================
// CambiarPasswordActivity — cambiar la contraseña desde Ajustes (GP-105).
//
// Sobre POST /auth/change-password. La contraseña nueva sigue la regla de GP-101
// (DEC-034), comprobada aquí antes de salir a la red para que el error salga en su
// campo. Las dos se recortan en los extremos, como al crearla y al entrar (DEC-034).
//
// Cambiarla revoca todas las sesiones en el servidor, así que al terminar la app
// vuelve a entrar sola con la contraseña nueva; si no puede, lleva a Login con un
// aviso. Con la actual equivocada la API responde 403 (A.3), no 401: un 401 aquí lo
// tomaría la app por sesión caducada y echaría al usuario.
// ============================================================
public class CambiarPasswordActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(es.pmdm.gymprofit.utils.ScaleUtils.wrap(newBase));
    }

    private PreferencesManager prefs;
    private TextInputLayout tilActual, tilNueva;
    private TextInputEditText etActual, etNueva;
    private final AuthApi authApi = ApiClient.service(AuthApi.class);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new PreferencesManager(this);
        prefs.applyTheme();
        setContentView(R.layout.activity_cambiar_password);

        tilActual = findViewById(R.id.tilActual);
        tilNueva = findViewById(R.id.tilNueva);
        etActual = findViewById(R.id.etActual);
        etNueva = findViewById(R.id.etNueva);
        ((MaterialToolbar) findViewById(R.id.toolbar)).setNavigationOnClickListener(v -> finish());
        findViewById(R.id.btnGuardar).setOnClickListener(v -> cambiar());
    }

    private void cambiar() {
        String actual = texto(etActual);
        String nueva = texto(etNueva);
        boolean valido = true;

        if (actual.isEmpty()) {
            tilActual.setError(getString(R.string.cambiar_password_actual_requerida));
            valido = false;
        } else {
            tilActual.setError(null);
        }
        String problema = UIHelper.mensajePassword(this, PoliticaCuenta.problemaPassword(nueva));
        tilNueva.setError(problema);
        if (problema != null) valido = false;
        if (!valido) return;

        Map<String, Object> body = new HashMap<>();
        body.put("currentPassword", actual);
        body.put("newPassword", nueva);

        LoadingDialog.show(this);
        authApi.cambiarPassword(body).enqueue(new ApiCallback<Void>() {
            @Override
            public void onOk(Void cuerpo) {
                volverAEntrar(nueva);
            }
            @Override
            public void onFail(int code, String message) {
                LoadingDialog.hide(CambiarPasswordActivity.this);
                if (code == 403) {
                    tilActual.setError(getString(R.string.cambiar_password_actual_mal));
                    etActual.requestFocus();
                    return;
                }
                if (code == 400) {
                    String rechazo = UIHelper.mensajeRechazoPassword(CambiarPasswordActivity.this,
                            PoliticaCuenta.rechazoPassword(message));
                    tilNueva.setError(rechazo != null ? rechazo : getString(R.string.cambiar_password_igual));
                    etNueva.requestFocus();
                    return;
                }
                UiFeedback.toastError(CambiarPasswordActivity.this, code, message);
            }
        });
    }

    // Las sesiones se han revocado: se entra de nuevo con la contraseña recién puesta.
    private void volverAEntrar(String nueva) {
        Map<String, Object> body = new HashMap<>();
        body.put("username", prefs.getUsername());
        body.put("password", nueva);
        authApi.login(body).enqueue(new ApiCallback<TokenResponse>() {
            @Override
            public void onOk(TokenResponse resp) {
                LoadingDialog.hide(CambiarPasswordActivity.this);
                if (resp == null) { aLogin(); return; }
                UtilREST.setToken(resp.getToken());
                UtilREST.setRefreshToken(resp.getRefreshToken());
                prefs.saveSesion(resp.getToken(), resp.getRefreshToken());
                UIHelper.mostrarToastExito(CambiarPasswordActivity.this, getString(R.string.cambiar_password_ok));
                finish();
            }
            @Override
            public void onFail(int code, String message) {
                LoadingDialog.hide(CambiarPasswordActivity.this);
                aLogin();
            }
        });
    }

    // Sin poder entrar solo, a Login con el aviso de por qué.
    private void aLogin() {
        UtilREST.clearToken();
        prefs.cerrarSesion();
        AvatarUtils.olvidar();
        Intent i = new Intent(this, LoginActivity.class);
        i.putExtra(LoginActivity.EXTRA_PASSWORD_CAMBIADA, true);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    // Recortada en los extremos, como al crearla y al entrar (DEC-034).
    private static String texto(TextInputEditText campo) {
        return campo.getText() == null ? "" : campo.getText().toString().trim();
    }
}
