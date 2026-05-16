package es.pmdm.gymprofit.ui.activities;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import androidx.appcompat.app.AppCompatActivity;
import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.network.UtilREST;
import es.pmdm.gymprofit.utils.PreferencesManager;
import java.util.Locale;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_MIN_DURATION = 1500;

    private PreferencesManager prefsManager;
    private long tiempoInicio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefsManager = new PreferencesManager(this);
        aplicarIdiomaGuardado();

        setContentView(R.layout.activity_splash);

        tiempoInicio = System.currentTimeMillis();

        animarContenido();
        verificarSesion();
    }

    private void animarContenido() {
        View contenido = findViewById(R.id.layoutContenido);

        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(contenido, "alpha", 0f, 1f);
        fadeIn.setDuration(800);
        fadeIn.setInterpolator(new AccelerateDecelerateInterpolator());
        fadeIn.setStartDelay(200);
        fadeIn.start();
    }

    private void verificarSesion() {
        new Handler().post(() -> {
            boolean tieneSesion = prefsManager.haySesion();
            if (tieneSesion) {
                UtilREST.setToken(prefsManager.getToken());
            }

            long tiempoTranscurrido = System.currentTimeMillis() - tiempoInicio;
            long tiempoRestante = Math.max(0, SPLASH_MIN_DURATION - tiempoTranscurrido);

            new Handler().postDelayed(() -> {
                Intent intent;

                if (tieneSesion) {
                    intent = new Intent(this, HomeActivity.class);
                } else {
                    intent = new Intent(this, LoginActivity.class);
                }

                startActivity(intent);

                finish();
            }, tiempoRestante);
        });
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