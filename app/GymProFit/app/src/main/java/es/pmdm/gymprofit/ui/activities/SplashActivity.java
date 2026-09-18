package es.pmdm.gymprofit.ui.activities;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import androidx.appcompat.app.AppCompatActivity;
import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.network.UtilREST;
import es.pmdm.gymprofit.utils.PreferencesManager;

// ============================================================
// SplashActivity — pantalla de arranque de la aplicación.
// Muestra una animación de entrada mientras comprueba si hay sesión guardada,
// y tras un tiempo mínimo visible redirige a Home (si hay sesión) o a Login.
// ============================================================
public class SplashActivity extends AppCompatActivity {

    // Aplica la escala de fuente global de la app (agranda todo el texto uniformemente).
    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(es.pmdm.gymprofit.utils.ScaleUtils.wrap(newBase));
    }

    // Duración mínima (ms) que se muestra la splash aunque la comprobación de sesión sea instantánea
    private static final int SPLASH_MIN_DURATION = 1500;

    private PreferencesManager prefsManager;
    private long tiempoInicio;

    // Aplica el idioma guardado, infla el layout y lanza la animación y la comprobación de sesión.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefsManager = new PreferencesManager(this);

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
        // Handler ligado explícitamente al hilo principal (constructor sin Looper está deprecado)
        new Handler(Looper.getMainLooper()).post(() -> {
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

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                startActivity(new Intent(this, siguientePantalla()));
                finish();
            }, tiempoRestante);
        });
    }

    /**
     * Decide a qué pantalla se entra: login, onboarding o la app.
     *
     * <p>Antes solo se miraba si había sesión guardada, así que un usuario que
     * cerraba la app a mitad del onboarding —o que lo abandonaba tras registrarse—
     * entraba directo a la app sin nivel, objetivo ni calorías calculadas, y no
     * volvía a ver el asistente nunca: la única vía de entrada al onboarding son
     * el login y el registro, que ya había pasado. Con el flag comprobado aquí, el
     * asistente se retoma en el siguiente arranque.
     *
     * @return la Activity a la que hay que ir.
     */
    private Class<?> siguientePantalla() {
        if (!prefsManager.haySesion()) return LoginActivity.class;

        // Los invitados no tienen onboarding: entran a mirar la app sin configurar nada.
        if (prefsManager.isGuest()) return MainActivity.class;

        String usuario = prefsManager.getUsername();
        boolean completado = usuario != null && !usuario.isEmpty()
                && prefsManager.isOnboardingCompletadoParaUsuario(usuario);

        return completado ? MainActivity.class : Onboarding1Activity.class;
    }
}
