package es.pmdm.gymprofit.ui.cristal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

// ============================================================
// BordeSistemaPintadoTest — GP-285 (remate): el borde del sistema, pintado de verdad en esta
// versión de Android (se lanza en los tres emuladores, uno por nivel del cristal).
//
// Las cuentas están en BordesSistemaTest; aquí se pinta el dibujo en un mapa de bits y se mira
// lo que sale: macizo, con el fondo base, en la franja de la barra del sistema; transparente
// en la otra punta; y entre medias, cada vez más opaco hacia la barra.
// ============================================================
@RunWith(AndroidJUnit4.class)
public class BordeSistemaPintadoTest {

    private static final int BASE = 0xFF08090B;
    // El de abajo con botones en un móvil de 420 ppp: 278 px de alto y 126 de franja.
    private static final int ALTO = 278, MACIZO = 126;

    private static Bitmap pintar(boolean arriba, int macizo) {
        BordeSistema borde = new BordeSistema(arriba);
        borde.poner(BASE, macizo);
        borde.setBounds(0, 0, 8, ALTO);
        Bitmap mapa = Bitmap.createBitmap(8, ALTO, Bitmap.Config.ARGB_8888);
        borde.draw(new Canvas(mapa));
        return mapa;
    }

    @Test
    public void el_de_abajo_es_macizo_bajo_los_botones_y_se_va_hacia_arriba() {
        Bitmap mapa = pintar(false, MACIZO);
        assertEquals("la punta de arriba, transparente", 0, Color.alpha(mapa.getPixel(4, 0)));
        // La franja de los botones entera: el fondo base, opaco.
        for (int y = ALTO - MACIZO + 1; y < ALTO; y++) {
            assertEquals("y=" + y, BASE, mapa.getPixel(4, y));
        }
        for (int y = 1; y < ALTO; y++) {
            assertTrue("el alfa no baja hacia los botones (y=" + y + ")",
                    Color.alpha(mapa.getPixel(4, y)) >= Color.alpha(mapa.getPixel(4, y - 1)));
        }
        // A media altura del degradado ya se nota, pero el contenido se sigue viendo.
        int medio = Color.alpha(mapa.getPixel(4, (ALTO - MACIZO) / 2));
        assertTrue("alfa a medio degradado: " + medio, medio > 30 && medio < 110);
    }

    @Test
    public void el_de_arriba_es_macizo_bajo_la_barra_de_estado_y_se_va_hacia_abajo() {
        Bitmap mapa = pintar(true, MACIZO);
        assertEquals("la punta de abajo, transparente", 0, Color.alpha(mapa.getPixel(4, ALTO - 1)));
        for (int y = 0; y < MACIZO - 1; y++) {
            assertEquals("y=" + y, BASE, mapa.getPixel(4, y));
        }
        for (int y = 1; y < ALTO; y++) {
            assertTrue("el alfa no sube hacia el contenido (y=" + y + ")",
                    Color.alpha(mapa.getPixel(4, y)) <= Color.alpha(mapa.getPixel(4, y - 1)));
        }
    }

    @Test
    public void con_gestos_no_hay_tramo_macizo_y_solo_el_canto_llega_al_fondo() {
        Bitmap mapa = pintar(false, 0);
        assertEquals(0, Color.alpha(mapa.getPixel(4, 0)));
        assertTrue("el canto de la pantalla, casi el fondo base", Color.alpha(mapa.getPixel(4, ALTO - 1)) > 245);
        assertTrue("a tres cuartos todavía se ve el contenido", Color.alpha(mapa.getPixel(4, ALTO * 3 / 4)) < 160);
    }
}
