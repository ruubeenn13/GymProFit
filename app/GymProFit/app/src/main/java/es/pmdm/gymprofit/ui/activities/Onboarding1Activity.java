package es.pmdm.gymprofit.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

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

        // Login y registro traen el nombre, y el registro tambien el email. Es la
        // UNICA entrada de datos por extras que queda: se vuelcan al borrador aqui
        // y de la pantalla siguiente en adelante el asistente ya no usa extras.
        sembrarBorradorConLosExtras();

        String username = prefs.getBorradorNombre();
        if (username.isEmpty()) username = prefs.getUsername();

        TextView tvNombre = findViewById(R.id.tvBienvenidaNombre);

        if (!username.isEmpty()) {
            tvNombre.setText(username + "!");
        }

        findViewById(R.id.btnEmpezar).setOnClickListener(v ->
                startActivity(new Intent(this, Onboarding2Activity.class)));

        findViewById(R.id.tvSaltar1).setOnClickListener(v -> saltarAlHome());
    }

    /**
     * Vuelca al borrador el nombre y el email que traen login y registro.
     *
     * <p>Solo rellena lo que este vacio: si el usuario ya habia empezado el
     * asistente y vuelve a entrar, manda lo que el escribio, no lo que venia de
     * la pantalla de acceso.
     */
    private void sembrarBorradorConLosExtras() {
        String username = getIntent().getStringExtra("username");
        String email = getIntent().getStringExtra("email");

        String nombre = prefs.getBorradorNombre();
        if (nombre.isEmpty()) {
            nombre = (username != null && !username.isEmpty()) ? username : prefs.getUsername();
        }

        String correo = prefs.getBorradorEmail();
        if (correo.isEmpty() && email != null) correo = email;

        prefs.guardarBorradorDatos(nombre, correo, prefs.getBorradorEdad(), prefs.getBorradorSexo());
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