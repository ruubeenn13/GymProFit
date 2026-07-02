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

// ============================================================
// SplashActivity — pantalla de arranque de la aplicación.
// Muestra una animación de entrada mientras comprueba si hay sesión guardada,
// y tras un tiempo mínimo visible redirige a Home (si hay sesión) o a Login.
// ============================================================
public class SplashActivity extends AppCompatActivity {

    // Duración mínima (ms) que se muestra la splash aunque la comprobación de sesión sea instantánea
    private static final int SPLASH_MIN_DURATION = 1500;

    private PreferencesManager prefsManager;
    private long tiempoInicio;

    // Aplica el idioma guardado, infla el layout y lanza la animación y la comprobación de sesión.
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

    // Aplica un fade-in al contenido de la splash.
    private void animarContenido() {
        View contenido = findViewById(R.id.layoutContenido);

        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(contenido, "alpha", 0f, 1f);
        fadeIn.setDuration(800);
        fadeIn.setInterpolator(new AccelerateDecelerateInterpolator());
        fadeIn.setStartDelay(200);
        fadeIn.start();
    }

    // Comprueba si hay sesión guardada, configura el token REST si corresponde
    // y navega a Home/Login tras esperar el tiempo mínimo restante de la splash.
    private void verificarSesion() {
        new Handler().post(() -> {
            // Registra el persistidor: cuando UtilREST renueve el token con el refresh,
            // guarda los nuevos tokens en preferencias (instancia propia → sin compartir editor
            // con la UI, ya que el refresh ocurre en un hilo de red).
            UtilREST.setTokenPersister((nuevoToken, nuevoRefresh) ->
                    new PreferencesManager(getApplicationContext()).saveSesion(nuevoToken, nuevoRefresh));

            boolean tieneSesion = prefsManager.haySesion();
            if (tieneSesion) {
                UtilREST.setToken(prefsManager.getToken());
                UtilREST.setRefreshToken(prefsManager.getRefreshToken());
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

    // Aplica el idioma guardado en preferencias a la configuración de recursos.
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