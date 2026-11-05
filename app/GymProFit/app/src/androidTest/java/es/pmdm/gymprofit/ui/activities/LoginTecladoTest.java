package es.pmdm.gymprofit.ui.activities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.utils.PreferencesManager;

// ============================================================
// LoginTecladoTest — GP-223: Entrar con la sesión cerrada sola enseña el teclado.
//
// Con el usuario de la última vez ya escrito, el foco va a la contraseña y sale el teclado.
// Fallaba cuando Entrar se recreaba nada más abrirse: al aplicar el tema guardado (oscuro,
// el de serie) siendo la primera pantalla del proceso, o al volver después de que Android
// matara el proceso. En la recreación, el campo del usuario recupera su texto después de
// onCreate, y la decisión se tomaba antes, con el campo aún vacío.
//
// Toca las preferencias de la app instalada (tema y última entrada): se guardan antes y se
// reponen después.
// ============================================================
@RunWith(AndroidJUnit4.class)
public class LoginTecladoTest {

    private static final long ESPERA_MS = 5000;

    private PreferencesManager prefs;
    private int temaAntes;
    private String ultimaAntes;
    private int modoAntes;

    @Before
    public void guardar() {
        Context ctx = ApplicationProvider.getApplicationContext();
        prefs = new PreferencesManager(ctx);
        temaAntes = prefs.getTheme();
        ultimaAntes = prefs.getUltimaEntrada();
        modoAntes = AppCompatDelegate.getDefaultNightMode();
        prefs.saveUltimaEntrada("prueba");
    }

    @After
    public void reponer() {
        prefs.saveTheme(temaAntes);
        if (ultimaAntes.isEmpty()) prefs.olvidarUltimaEntrada();
        else prefs.saveUltimaEntrada(ultimaAntes);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(
                () -> AppCompatDelegate.setDefaultNightMode(modoAntes));
    }

    // Como al volver tras morir el proceso: la pantalla se recrea con el usuario ya escrito.
    @Test
    public void al_recrearse_el_foco_y_el_teclado_van_a_la_contrasena() {
        prefs.saveTheme(AppCompatDelegate.MODE_NIGHT_NO);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(
                () -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO));
        try (ActivityScenario<LoginActivity> esc = ActivityScenario.launch(intentSesionCerrada())) {
            esperarTeclado(esc);
            // Tras morir el proceso no hay teclado que conservar: se esconde antes de recrear.
            esc.onActivity(act -> androidx.core.view.WindowCompat
                    .getInsetsController(act.getWindow(), act.findViewById(R.id.etPassword))
                    .hide(WindowInsetsCompat.Type.ime()));
            SystemClock.sleep(1000);
            esc.recreate();
            esperarTeclado(esc);
        }
    }

    // Entrar como primera pantalla del proceso, en oscuro: aplicar el tema la recrea.
    @Test
    public void primera_pantalla_en_oscuro_el_foco_y_el_teclado_van_a_la_contrasena() {
        prefs.saveTheme(AppCompatDelegate.MODE_NIGHT_YES);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(
                () -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_UNSPECIFIED));
        try (ActivityScenario<LoginActivity> esc = ActivityScenario.launch(intentSesionCerrada())) {
            esperarTeclado(esc);
        }
    }

    private static Intent intentSesionCerrada() {
        return new Intent(ApplicationProvider.getApplicationContext(), LoginActivity.class)
                .putExtra(LoginActivity.EXTRA_SESION_CERRADA, true);
    }

    // El usuario sigue escrito, la contraseña tiene el foco y el teclado se ve.
    private static void esperarTeclado(ActivityScenario<LoginActivity> esc) {
        AtomicReference<String> usuario = new AtomicReference<>();
        AtomicBoolean foco = new AtomicBoolean();
        AtomicBoolean teclado = new AtomicBoolean();
        long fin = SystemClock.uptimeMillis() + ESPERA_MS;
        do {
            SystemClock.sleep(100);
            esc.onActivity(act -> {
                EditText etUsuario = act.findViewById(R.id.etUsuario);
                EditText etPassword = act.findViewById(R.id.etPassword);
                usuario.set(etUsuario.getText().toString());
                foco.set(etPassword.hasFocus());
                WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(etPassword);
                teclado.set(insets != null && insets.isVisible(WindowInsetsCompat.Type.ime()));
            });
        } while (!(foco.get() && teclado.get()) && SystemClock.uptimeMillis() < fin);
        assertEquals("el usuario de la última vez sigue escrito", "prueba", usuario.get());
        assertTrue("el foco está en la contraseña", foco.get());
        assertTrue("el teclado se ve", teclado.get());
    }
}
