package es.pmdm.gymprofit.ui.activities;

import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

import java.util.Locale;
import java.util.Map;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.dto.LoginDTO;
import es.pmdm.gymprofit.network.dto.RegisterDTO;
import es.pmdm.gymprofit.network.dto.TokenDTO;
import es.pmdm.gymprofit.network.dto.UsuarioDTO;
import es.pmdm.gymprofit.utils.PreferencesManager;
import es.pmdm.gymprofit.utils.UIHelper;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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
        String email = etRegEmail.getText().toString().trim();
        String password = etRegPassword.getText().toString().trim();

        RegisterDTO registerDTO = new RegisterDTO(username, password, email);

        ApiClient.getApiService().register(registerDTO).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    UIHelper.mostrarToastExito(RegistroActivity.this,
                            getString(R.string.registro_exito));

                    hacerLoginAutomatico(username, password);
                } else {
                    UIHelper.mostrarToastError(RegistroActivity.this,
                            getString(R.string.registro_error));
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                UIHelper.mostrarToastError(RegistroActivity.this,
                        getString(R.string.error_conexion));
            }
        });
    }

    private void hacerLoginAutomatico(String username, String password) {
        LoginDTO loginDTO = new LoginDTO(username, password);


        ApiClient.getApiService().login(loginDTO).enqueue(new Callback<TokenDTO>() {
            @Override
            public void onResponse(Call<TokenDTO> call, Response<TokenDTO> response) {
                if (response.isSuccessful() && response.body() != null) {
                    TokenDTO token = response.body();

                    ApiClient.setToken(token.getToken());

                    prefsManager.saveToken(token.getToken());
                    prefsManager.saveUsername(token.getUsername());

                    obtenerUsuarioId(token.getUsername());
                } else {
                    startActivity(new Intent(RegistroActivity.this, LoginActivity.class)
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));

                    finish();
                }
            }

            @Override
            public void onFailure(Call<TokenDTO> call, Throwable t) {
                startActivity(new Intent(RegistroActivity.this, LoginActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));

                finish();
            }
        });
    }

    private void obtenerUsuarioId(String username) {
        ApiClient.getApiService().getUsuarioPorUsername(username)
                .enqueue(new Callback<UsuarioDTO>() {
                    @Override
                    public void onResponse(Call<UsuarioDTO> call, Response<UsuarioDTO> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            prefsManager.saveUsuarioId(response.body().getId());
                        }

                        Intent intent = new Intent(RegistroActivity.this, Onboarding1Activity.class);
                        intent.putExtra("username", prefsManager.getUsername());
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                        startActivity(intent);

                        finish();
                    }

                    @Override
                    public void onFailure(Call<UsuarioDTO> call, Throwable t) {
                        Intent intent = new Intent(RegistroActivity.this, Onboarding1Activity.class);
                        intent.putExtra("username", prefsManager.getUsername());
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                        startActivity(intent);

                        finish();
                    }
                });
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