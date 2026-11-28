package es.pmdm.gymprofit.ui.cristal;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.ContextThemeWrapper;

import androidx.core.content.ContextCompat;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import es.pmdm.gymprofit.R;

// ============================================================
// CristalPintadoTest — GP-285, lote 2: el cristal estático deja ver lo que tiene detrás, pintado
// de verdad en esta versión de Android (se lanza en los tres emuladores).
//
// Al añadir el surco oscuro de la fila dorada de la Sesión en vivo (ORO_FILA), su pincel nacía
// negro opaco y se pintaba debajo de TODOS los cristales: las tarjetas de Inicio y de la sesión
// dejaban de ser translúcidas (en oscuro apenas se notaba; en claro salían grises). Aquí cada
// cristal se pinta sobre un rojo puro y se mira el píxel del centro: una capa blanca o dorada
// translúcida no le quita rojo; una capa negra debajo, sí.
//
// El surco solo existe en la fila dorada y solo en oscuro (en claro es transparente): ahí el
// rojo sí baja, y se comprueba que baja lo que dice su token y no más.
// ============================================================
@RunWith(AndroidJUnit4.class)
public class CristalPintadoTest {

    private static final int LADO = 120;

    private static Context contexto(boolean oscuro) {
        Context base = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Configuration conf = new Configuration(base.getResources().getConfiguration());
        conf.uiMode = (conf.uiMode & ~Configuration.UI_MODE_NIGHT_MASK)
                | (oscuro ? Configuration.UI_MODE_NIGHT_YES : Configuration.UI_MODE_NIGHT_NO);
        Context tema = new ContextThemeWrapper(base.createConfigurationContext(conf), R.style.Theme_GymProFit);
        return new ContextThemeWrapper(tema, R.style.ThemeOverlay_GymProFit_Cristal);
    }

    // El rojo (0–255) del centro de ese dibujo pintado sobre rojo puro.
    private static int rojoDelCentro(Context c, int dibujo) {
        Drawable d = ContextCompat.getDrawable(c, dibujo);
        assertNotNull(d);
        Bitmap b = Bitmap.createBitmap(LADO, LADO, Bitmap.Config.ARGB_8888);
        Canvas lienzo = new Canvas(b);
        lienzo.drawColor(Color.RED);
        d.setBounds(0, 0, LADO, LADO);
        d.draw(lienzo);
        return Color.red(b.getPixel(LADO / 2, LADO / 2));
    }

    @Test
    public void el_cristal_estatico_no_tapa_lo_de_detras() {
        for (boolean oscuro : new boolean[]{ true, false }) {
            Context c = contexto(oscuro);
            String tema = oscuro ? "oscuro" : "claro";
            int[] dibujos = { R.drawable.bg_cristal, R.drawable.bg_cristal_heroe, R.drawable.bg_cristal_tile,
                    R.drawable.bg_cristal_pildora, R.drawable.bg_cristal_fila_lente, R.drawable.bg_cristal_oro,
                    R.drawable.bg_cristal_fila_destello };
            for (int dibujo : dibujos) {
                int rojo = rojoDelCentro(c, dibujo);
                // Blanco o dorado translúcidos encima de rojo puro: el rojo se queda (el dorado
                // #E8B84B al 35 % lo baja, como mucho, a 247).
                assertTrue(tema + ", " + c.getResources().getResourceEntryName(dibujo) + ": rojo " + rojo
                        + ", hay algo oscuro debajo del cristal", rojo >= 240);
            }
        }
    }

    @Test
    public void el_surco_solo_esta_en_la_fila_dorada_y_solo_en_oscuro() {
        // En claro el surco es transparente: la fila dorada es el tinte y nada más.
        int claro = rojoDelCentro(contexto(false), R.drawable.bg_cristal_fila_oro);
        assertTrue("claro: rojo " + claro, claro >= 240);
        // En oscuro, negro al 25 % bajo el tinte: el rojo baja a tres cuartos (unos 190–200),
        // ni se queda como estaba ni se tapa del todo.
        int oscuro = rojoDelCentro(contexto(true), R.drawable.bg_cristal_fila_oro);
        assertTrue("oscuro: rojo " + oscuro, oscuro >= 180 && oscuro <= 215);
    }
}
