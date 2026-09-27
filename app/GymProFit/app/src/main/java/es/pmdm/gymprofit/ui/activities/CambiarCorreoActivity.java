package es.pmdm.gymprofit.ui.activities;

import android.os.Bundle;
import android.util.Patterns;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.HashMap;
import java.util.Map;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.usuario.Usuario;
import es.pmdm.gymprofit.network.ApiCallback;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.UsuarioApi;
import es.pmdm.gymprofit.utils.Correo;
import es.pmdm.gymprofit.utils.LoadingDialog;
import es.pmdm.gymprofit.utils.PreferencesManager;
import es.pmdm.gymprofit.utils.UIHelper;
import es.pmdm.gymprofit.utils.UiFeedback;

// ============================================================
// CambiarCorreoActivity — cambiar el correo desde Ajustes (GP-105).
//
// Con la contraseña, como en GP-083: PUT /usuarios/me/email. Cada rechazo va al campo
// que lo causa: 403 contraseña, 409 en uso, 400 formato. Antes estaba dentro de
// editar perfil, que ahora solo lleva los datos y objetivos. La contraseña se recorta
// en los extremos, como en el resto de la app (DEC-034).
// ============================================================
public class CambiarCorreoActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(es.pmdm.gymprofit.utils.ScaleUtils.wrap(newBase));
    }

    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText etEmail, etPassword;
    @Nullable private String emailActual;
    private final UsuarioApi usuarioApi = ApiClient.service(UsuarioApi.class);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferencesManager prefs = new PreferencesManager(this);
        prefs.applyTheme();
        setContentView(R.layout.activity_cambiar_correo);

        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPasswordEmail);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPasswordEmail);
        ((MaterialToolbar) findViewById(R.id.toolbar)).setNavigationOnClickListener(v -> finish());
        findViewById(R.id.btnGuardar).setOnClickListener(v -> guardar());

        usuarioApi.getPorId(prefs.getUsuarioId()).enqueue(new ApiCallback<Usuario>() {
            @Override
            public void onOk(Usuario u) {
                if (u == null || isFinishing()) return;
                emailActual = u.getEmail();
                ((TextView) findViewById(R.id.tvCorreoActual))
                        .setText(getString(R.string.cambiar_correo_actual, Correo.enmascarar(emailActual)));
            }
            @Override
            public void onFail(int code, String message) {
                // Sin el correo actual no se puede comparar, pero sí cambiar: la API dice
                // si es el mismo. Se avisa del fallo.
                UiFeedback.toastError(CambiarCorreoActivity.this, code, message);
            }
        });
    }

    /**
     * Dice si lo escrito es otro correo que el de la cuenta.
     *
     * <p>Sin distinguir mayúsculas, como la restricción única de la base: cambiar solo
     * mayúsculas no es otro correo.
     *
     * @param original correo de la cuenta, o null si no ha cargado.
     * @param escrito lo que hay en el campo.
     * @return {@code true} si es otro correo; {@code false} si es el mismo o no se sabe.
     */
    static boolean cambiaEmail(@Nullable String original, @Nullable String escrito) {
        if (original == null || escrito == null) return false;
        return !escrito.trim().equalsIgnoreCase(original.trim());
    }

    private void guardar() {
        String email = etEmail.getText() == null ? "" : etEmail.getText().toString().trim();
        String password = etPassword.getText() == null ? "" : etPassword.getText().toString().trim();

        if (email.isEmpty()) {
            tilEmail.setError(getString(R.string.editar_perfil_email_requerido));
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError(getString(R.string.registro_email_invalido));
            return;
        }
        if (emailActual != null && !cambiaEmail(emailActual, email)) {
            tilEmail.setError(getString(R.string.cambiar_correo_igual));
            return;
        }
        tilEmail.setError(null);
        if (password.isEmpty()) {
            tilPassword.setError(getString(R.string.editar_perfil_password_requerida));
            return;
        }
        tilPassword.setError(null);

        Map<String, Object> body = new HashMap<>();
        body.put("email", email);
        body.put("password", password);
        LoadingDialog.show(this);
        usuarioApi.cambiarEmail(body).enqueue(new ApiCallback<Void>() {
            @Override
            public void onOk(Void ignorado) {
                LoadingDialog.hide(CambiarCorreoActivity.this);
                UIHelper.mostrarToastExito(CambiarCorreoActivity.this, getString(R.string.cambiar_correo_ok));
                setResult(RESULT_OK);
                finish();
            }
            @Override
            public void onFail(int code, String message) {
                LoadingDialog.hide(CambiarCorreoActivity.this);
                if (code == 403) {
                    tilPassword.setError(getString(R.string.editar_perfil_password_incorrecta));
                } else if (code == 409) {
                    tilEmail.setError(getString(R.string.editar_perfil_email_en_uso));
                } else if (code == 400) {
                    tilEmail.setError(getString(R.string.registro_email_invalido));
                } else {
                    UiFeedback.toastError(CambiarCorreoActivity.this, code, message);
                }
            }
        });
    }
}
