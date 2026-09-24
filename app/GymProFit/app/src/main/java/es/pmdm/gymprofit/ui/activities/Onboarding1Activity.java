package es.pmdm.gymprofit.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.utils.PreferencesManager;

// ============================================================
// Onboarding1Activity — primer paso del asistente de onboarding.
// Muestra la pantalla de bienvenida personalizada con el nombre de
// usuario y permite avanzar al siguiente paso o saltar directo a Home.
// ============================================================
public class Onboarding1Activity extends AppCompatActivity {

    private PreferencesManager prefs;

    // Aplica la escala de fuente global de la app (agranda todo el texto uniformemente).
    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(es.pmdm.gymprofit.utils.ScaleUtils.wrap(newBase));
    }

    // Aplica tema/idioma, infla el layout, muestra el saludo personalizado
    // y configura los botones de avanzar y saltar el onboarding.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = new PreferencesManager(this);
        prefs.applyTheme();

        setContentView(R.layout.activity_onboarding1);

        // Login y registro traen el nombre. Es la
        // UNICA entrada de datos por extras que queda: se vuelcan al borrador aqui
        // y de la pantalla siguiente en adelante el asistente ya no usa extras.
        sembrarBorradorConLosExtras();

        String username = prefs.getBorradorNombre();
        if (username.isEmpty()) username = prefs.getUsername();

        TextView tvSaludo = findViewById(R.id.tvBienvenidaNombre);

        // Una sola frase, no dos TextView sueltos: el lector de pantalla lo
        // anuncia seguido en vez de leer "bienvenido a" y el nombre por separado.
        // Y sin género: antes decía "¡Bienvenido a" y debajo el nombre, que se
        // leía "¡Bienvenido a Rubén!", como si la persona fuese un sitio.
        tvSaludo.setText(getString(R.string.onboarding_bienvenida_saludo, username));
        ViewCompat.setAccessibilityHeading(tvSaludo, true);

        animarEntrada();

        findViewById(R.id.btnEmpezar).setOnClickListener(v ->
                startActivity(new Intent(this, Onboarding2Activity.class)));

        findViewById(R.id.tvSaltar1).setOnClickListener(v -> saltarAlHome());
    }

    /**
     * Entrada escalonada de los bloques de la pantalla.
     *
     * <p>El splash ya entra con un fundido de 800 ms y esta pantalla aparecía
     * seca justo después, que es la primera impresión del producto. Cada bloque
     * sube 16 dp y aparece con 60 ms de retraso sobre el anterior, lo justo para
     * que el ojo siga el orden de lectura sin que nadie tenga que esperar.
     *
     * <p>Si el sistema tiene las animaciones desactivadas (ajuste de
     * accesibilidad o modo de ahorro), no se anima nada: se deja todo visible.
     */
    private void animarEntrada() {
        View[] bloques = {
                findViewById(R.id.ivLogoBienvenida),
                findViewById(R.id.tvAntetitulo),
                findViewById(R.id.tvBienvenidaNombre),
                findViewById(R.id.tvBienvenidaDesc),
                findViewById(R.id.layoutPasos),
                findViewById(R.id.layoutPieBienvenida)
        };

        if (animacionesDesactivadas()) return;

        float desplazamiento = 16 * getResources().getDisplayMetrics().density;

        for (int i = 0; i < bloques.length; i++) {
            View bloque = bloques[i];
            if (bloque == null) continue;

            bloque.setAlpha(0f);
            bloque.setTranslationY(desplazamiento);
            bloque.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(80L + i * 60L)
                    .setDuration(260L)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }
    }

    /**
     * Comprueba si el sistema tiene las animaciones apagadas.
     *
     * @return {@code true} si no hay que animar nada.
     */
    private boolean animacionesDesactivadas() {
        float escala = Settings.Global.getFloat(
                getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE, 1f);
        return escala == 0f;
    }

    /**
     * Vuelca al borrador el nombre que traen login y registro.
     *
     * <p>El correo NO (GP-083): la cuenta ya lo tiene desde el registro, y tras
     * iniciar sesión llegaba en blanco, de modo que el asistente lo pedía otra vez y
     * mandaba lo que se escribiera. Una errata dejaba la cuenta sin recuperación.
     *
     * <p>Solo rellena lo que este vacio: si el usuario ya habia empezado el
     * asistente y vuelve a entrar, manda lo que el escribio, no lo que venia de
     * la pantalla de acceso.
     */
    private void sembrarBorradorConLosExtras() {
        String username = getIntent().getStringExtra("username");

        String nombre = prefs.getBorradorNombre();
        if (nombre.isEmpty()) {
            nombre = (username != null && !username.isEmpty()) ? username : prefs.getUsername();
        }

        prefs.guardarBorradorDatos(nombre, prefs.getBorradorEdad(), prefs.getBorradorSexo());
    }

    // Saltar el onboarding es una DECISION del usuario, no un abandono: se marca
    // como visto para no volver a pedirselo en cada arranque (el splash lo
    // reabriria si no) y se tira el borrador, que ya no hay nada que reanudar.
    private void saltarAlHome() {
        prefs.setOnboardingCompletado(true);
        prefs.setOnboardingCompletadoParaUsuario(prefs.getUsername());
        prefs.limpiarBorradorOnboarding();
        startActivity(new Intent(this, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }
}