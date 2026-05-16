package es.pmdm.gymprofit.ui.activities;

import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONException;

import java.util.Locale;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.usuario.Usuario;
import es.pmdm.gymprofit.network.API;
import es.pmdm.gymprofit.network.UtilJSONParser;
import es.pmdm.gymprofit.network.UtilREST;
import es.pmdm.gymprofit.utils.PreferencesManager;
import es.pmdm.gymprofit.utils.UIHelper;

public class RegistroActivity extends AppCompatActivity {

    private TextInputEditText etRegUsername, etRegEmail, etRegPassword, etRegConfirmarPassword;
    private PreferencesManager prefsManager;

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

    private void inicializarVistas() {
        etRegUsername = findViewById(R.id.etRegUsername);
        etRegEmail = findViewById(R.id.etRegEmail);
        etRegPassword = findViewById(R.id.etRegPassword);
        etRegConfirmarPassword = findViewById(R.id.etRegConfirmarPassword);
    }

    private void configurarEventos() {
        findViewById(R.id.btnVolverLogin).setOnClickListener(v -> finish());
        findViewById(R.id.tvYaTengoCuenta).setOnClickListener(v -> finish());
        findViewById(R.id.btnCrearCuenta).setOnClickListener(v -> {
            if (!validarCampos()) return;
            registrar();
        });
    }

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

    private void registrar() {
        String username = etRegUsername.getText().toString().trim();
        String email    = etRegEmail.getText().toString().trim();
        String password = etRegPassword.getText().toString().trim();

        API.register(username, password, email, new UtilREST.OnResponseListener() {
            @Override
            public void onSuccess(String response, int statusCode) {
                UIHelper.mostrarToastExito(RegistroActivity.this, getString(R.string.registro_exito));
                hacerLoginAutomatico(username, password);
            }

            @Override
            public void onError(String message, int statusCode) {
                UIHelper.mostrarToastError(RegistroActivity.this, getString(R.string.registro_error));
            }
        });
    }

    private void hacerLoginAutomatico(String username, String password) {
        API.login(username, password, new UtilREST.OnResponseListener() {
            @Override
            public void onSuccess(String response, int statusCode) {
                try {
                    String token = UtilJSONParser.parseToken(response);
                    String user  = UtilJSONParser.parseTokenUsername(response);

                    UtilREST.setToken(token);
                    prefsManager.saveToken(token);
                    prefsManager.saveUsername(user);

                    obtenerUsuario(user);
                } catch (JSONException e) {
                    irAlLogin();
                }
            }

            @Override
            public void onError(String message, int statusCode) {
                irAlLogin();
            }
        });
    }

    private void obtenerUsuario(String username) {
        API.getUsuarioPorUsername(username, new UtilREST.OnResponseListener() {
            @Override
            public void onSuccess(String response, int statusCode) {
                try {
                    Usuario u = UtilJSONParser.parseUsuario(response);
                    prefsManager.saveUsuarioId(u.getId());
                    prefsManager.saveRol(u.getRol());
                } catch (JSONException e) {
                    // continúa sin id/rol
                }
                irAlOnboarding();
            }

            @Override
            public void onError(String message, int statusCode) {
                irAlOnboarding();
            }
        });
    }

    private void irAlOnboarding() {
        Intent intent = new Intent(this, Onboarding1Activity.class);
        intent.putExtra("username", prefsManager.getUsername());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void irAlLogin() {
        startActivity(new Intent(this, LoginActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }

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
