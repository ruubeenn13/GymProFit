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

import java.util.Locale;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.network.ApiClient;
import es.pmdm.gymprofit.network.dto.LoginDTO;
import es.pmdm.gymprofit.network.dto.TokenDTO;
import es.pmdm.gymprofit.network.dto.UsuarioDTO;
import es.pmdm.gymprofit.utils.PreferencesManager;
import es.pmdm.gymprofit.utils.UIHelper;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsuario, etPassword;
    private ImageButton btnCambiarTema, btnCambiarIdioma;
    private PreferencesManager prefsManager;

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

    private void inicializarVistas() {
        etUsuario = findViewById(R.id.etUsuario);
        etPassword = findViewById(R.id.etPassword);
        btnCambiarTema = findViewById(R.id.btnCambiarTema);
        btnCambiarIdioma = findViewById(R.id.btnCambiarIdioma);
    }

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

        findViewById(R.id.tvNoTienesCuenta).setOnClickListener(v ->
                startActivity(new Intent(this, RegistroActivity.class)));

        btnCambiarTema.setOnClickListener(v -> cambiarTema());
        btnCambiarIdioma.setOnClickListener(v -> mostrarDialogoIdioma());
    }

    private void hacerLogin(String username, String password) {
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
                    UIHelper.mostrarToastError(LoginActivity.this,
                            getString(R.string.login_error_credenciales));
                }
            }

            @Override
            public void onFailure(Call<TokenDTO> call, Throwable t) {
                UIHelper.mostrarToastError(LoginActivity.this,
                        getString(R.string.error_conexion));
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

                        navegarTrasLogin();
                    }

                    @Override
                    public void onFailure(Call<UsuarioDTO> call, Throwable t) {
                        navegarTrasLogin();
                    }
                });
    }

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

    private void cambiarTema() {
        int currentMode = prefsManager.getTheme();
        int newMode = (currentMode == AppCompatDelegate.MODE_NIGHT_YES)
                ? AppCompatDelegate.MODE_NIGHT_NO
                : AppCompatDelegate.MODE_NIGHT_YES;

        prefsManager.saveTheme(newMode);

        recreate();
    }

    private void actualizarIconoTema() {
        int currentMode = prefsManager.getTheme();
        btnCambiarTema.setImageResource(
                currentMode == AppCompatDelegate.MODE_NIGHT_YES
                        ? R.drawable.ic_sun
                        : R.drawable.ic_moon);
    }

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
    }

    private void cambiarIdioma(String languageCode) {
        prefsManager.saveLanguage(languageCode);
        recreate();
    }

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