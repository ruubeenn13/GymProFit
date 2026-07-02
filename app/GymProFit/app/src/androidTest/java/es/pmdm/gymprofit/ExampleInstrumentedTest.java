package es.pmdm.gymprofit;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
// ============================================================
// ExampleInstrumentedTest — test instrumentado de ejemplo generado por Android Studio
// Se ejecuta en un dispositivo/emulador real y comprueba que el
// contexto de la app se resuelve con el package name esperado.
// ============================================================
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    // Verifica que el package name de la app bajo test es el correcto
    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("es.pmdm.gymprofit", appContext.getPackageName());
    }
}