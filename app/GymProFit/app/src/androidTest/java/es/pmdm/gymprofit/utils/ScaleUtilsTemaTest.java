package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

// ============================================================
// ScaleUtilsTemaTest — el claro u oscuro de la app viaja en el override del contexto de
// cada pantalla (GP-216, lote 4). Antes lo ponía AppCompat cambiando a mano los Resources,
// y al encender TalkBack el proceso recibía la configuración del sistema y lo nuevo se
// pintaba en claro dentro de la pantalla oscura. Se prueba el tema contrario al del
// sistema, que es el caso que fallaba: en un emulador en claro, el oscuro de la app.
// ============================================================
@RunWith(AndroidJUnit4.class)
public class ScaleUtilsTemaTest {

    private static final String PREFS = "GymProFitPrefs";
    private static final String CLAVE = "theme_mode";

    private Context ctx;
    private SharedPreferences prefs;
    private boolean habia;
    private int antes;

    @Before
    public void guardar() {
        ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();
        prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        habia = prefs.contains(CLAVE);
        antes = prefs.getInt(CLAVE, 0);
    }

    @After
    public void restaurar() {
        if (habia) prefs.edit().putInt(CLAVE, antes).commit();
        else prefs.edit().remove(CLAVE).commit();
    }

    @Test
    public void elTemaGuardadoMandaSobreElDelSistema() {
        int sistema = ctx.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        int contrario = sistema == Configuration.UI_MODE_NIGHT_YES
                ? AppCompatDelegate.MODE_NIGHT_NO : AppCompatDelegate.MODE_NIGHT_YES;
        int esperado = sistema == Configuration.UI_MODE_NIGHT_YES
                ? Configuration.UI_MODE_NIGHT_NO : Configuration.UI_MODE_NIGHT_YES;

        prefs.edit().putInt(CLAVE, contrario).commit();
        assertEquals(esperado, modo(ScaleUtils.wrap(ctx)));
    }

    @Test
    public void claroYOscuroLlegan() {
        prefs.edit().putInt(CLAVE, AppCompatDelegate.MODE_NIGHT_NO).commit();
        assertEquals(Configuration.UI_MODE_NIGHT_NO, modo(ScaleUtils.wrap(ctx)));
        prefs.edit().putInt(CLAVE, AppCompatDelegate.MODE_NIGHT_YES).commit();
        assertEquals(Configuration.UI_MODE_NIGHT_YES, modo(ScaleUtils.wrap(ctx)));
    }

    @Test
    public void sinTemaGuardadoEsOscuro() {
        prefs.edit().remove(CLAVE).commit();
        assertEquals(Configuration.UI_MODE_NIGHT_YES, modo(ScaleUtils.wrap(ctx)));
    }

    @Test
    public void laLetraSigueSiendoLaDelSistema() {
        float sistema = ctx.getResources().getConfiguration().fontScale;
        assertEquals(sistema, ScaleUtils.wrap(ctx).getResources().getConfiguration().fontScale, 0.001f);
    }

    private static int modo(Context c) {
        return c.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
    }
}
