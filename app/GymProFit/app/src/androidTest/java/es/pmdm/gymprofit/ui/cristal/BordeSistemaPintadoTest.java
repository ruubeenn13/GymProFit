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

    // Holgura por canal: al componer la imagen de la rampa, cada versión de Android redondea
    // a su manera (en Android 7, un nivel arriba o abajo).
    private static final int TRAMADO = 2;

    private static void transparente(String donde, int pixel) {
        assertTrue(donde + ", transparente: alfa " + Color.alpha(pixel), Color.alpha(pixel) <= TRAMADO);
    }

    private static void macizo(String donde, int pixel) {
        assertEquals(donde + ": opaco", 255, Color.alpha(pixel));
        assertTrue(donde + ": el fondo base y es #" + Integer.toHexString(pixel),
                Math.abs(Color.red(pixel) - Color.red(BASE)) <= TRAMADO
                        && Math.abs(Color.green(pixel) - Color.green(BASE)) <= TRAMADO
                        && Math.abs(Color.blue(pixel) - Color.blue(BASE)) <= TRAMADO);
    }

    private static Bitmap pintar(boolean arriba, int macizo) {
        return pintar(arriba, macizo, ALTO);
    }

    private static Bitmap pintar(boolean arriba, int macizo, int alto) {
        BordeSistema borde = new BordeSistema(arriba);
        borde.poner(BASE, macizo);
        borde.setBounds(0, 0, 8, alto);
        Bitmap mapa = Bitmap.createBitmap(8, alto, Bitmap.Config.ARGB_8888);
        borde.draw(new Canvas(mapa));
        return mapa;
    }

    @Test
    public void el_de_abajo_es_macizo_bajo_los_botones_y_se_va_hacia_arriba() {
        Bitmap mapa = pintar(false, MACIZO);
        transparente("la punta de arriba", mapa.getPixel(4, 0));
        // La franja de los botones entera: el fondo base, opaco.
        for (int y = ALTO - MACIZO + 1; y < ALTO; y++) {
            macizo("y=" + y, mapa.getPixel(4, y));
        }
        for (int y = 1; y < ALTO; y++) {
            assertTrue("el alfa no baja hacia los botones (y=" + y + ")",
                    Color.alpha(mapa.getPixel(4, y)) >= Color.alpha(mapa.getPixel(4, y - 1)) - TRAMADO);
        }
        // A media altura del degradado ya se nota, pero el contenido se sigue viendo.
        int medio = Color.alpha(mapa.getPixel(4, (ALTO - MACIZO) / 2));
        assertTrue("alfa a medio degradado: " + medio, medio > 30 && medio < 110);
    }

    @Test
    public void el_de_arriba_es_macizo_bajo_la_barra_de_estado_y_se_va_hacia_abajo() {
        Bitmap mapa = pintar(true, MACIZO);
        transparente("la punta de abajo", mapa.getPixel(4, ALTO - 1));
        for (int y = 0; y < MACIZO - 1; y++) {
            macizo("y=" + y, mapa.getPixel(4, y));
        }
        for (int y = 1; y < ALTO; y++) {
            assertTrue("el alfa no sube hacia el contenido (y=" + y + ")",
                    Color.alpha(mapa.getPixel(4, y)) <= Color.alpha(mapa.getPixel(4, y - 1)) + TRAMADO);
        }
    }

    // Con gestos, en ese móvil: 110 px de alto (el hueco de 42 y los 68 hasta la cápsula) con
    // el hueco macizo, que es donde va el asa del sistema.
    @Test
    public void con_gestos_el_hueco_del_asa_es_macizo_y_el_degradado_corto() {
        int alto = 110, hueco = 42;
        Bitmap mapa = pintar(false, BordesSistema.macizoInferior(hueco), alto);
        transparente("junto a la cápsula", mapa.getPixel(4, 0));
        // El asa va de 17 a 22 px del canto: ahí, y en todo el hueco, el fondo base.
        for (int y = alto - hueco + 1; y < alto; y++) {
            macizo("y=" + y, mapa.getPixel(4, y));
        }
        assertTrue("a media altura del degradado todavía se ve el contenido",
                Color.alpha(mapa.getPixel(4, (alto - hueco) / 2)) < 110);
    }

    // ── Con fondo ambiental (lote 1b) ────────────────────────────────────────
    // El borde desvanece el contenido hacia el fondo que hay, no hacia el color base liso: en el
    // tramo macizo lleva los mismos píxeles que el fondo ambiental en ese sitio. Pintado en liso
    // tapaba el brillo que hay bajo la barra de estado y dejaba una banda oscura.

    // Un fondo de mentira en el que cada fila tiene su color: así se ve de qué fila sale cada una.
    private static final int ALTO_FONDO = 600;

    private static Bitmap fondoDeFilas() {
        Bitmap fondo = Bitmap.createBitmap(8, ALTO_FONDO, Bitmap.Config.ARGB_8888);
        for (int y = 0; y < ALTO_FONDO; y++) {
            int color = Color.rgb(40 + y % 200, 20 + (y / 3) % 200, 90);
            for (int x = 0; x < 8; x++) fondo.setPixel(x, y, color);
        }
        return fondo;
    }

    private static Bitmap pintarConFondo(boolean arriba, float mezcla) {
        BordeSistema borde = new BordeSistema(arriba);
        borde.poner(BASE, MACIZO);
        borde.setBounds(0, 0, 8, ALTO);
        borde.ponerFondo(fondoDeFilas());
        borde.mezcla(mezcla);
        Bitmap mapa = Bitmap.createBitmap(8, ALTO, Bitmap.Config.ARGB_8888);
        borde.draw(new Canvas(mapa));
        return mapa;
    }

    private static void igual(String donde, int esperado, int pixel) {
        assertEquals(donde + ": opaco", 255, Color.alpha(pixel));
        assertTrue(donde + ": tenía que ser #" + Integer.toHexString(esperado) + " y es #" + Integer.toHexString(pixel),
                Math.abs(Color.red(pixel) - Color.red(esperado)) <= TRAMADO
                        && Math.abs(Color.green(pixel) - Color.green(esperado)) <= TRAMADO
                        && Math.abs(Color.blue(pixel) - Color.blue(esperado)) <= TRAMADO);
    }

    @Test
    public void con_fondo_ambiental_el_tramo_macizo_es_ese_fondo() {
        Bitmap fondo = fondoDeFilas();
        // El de arriba está pegado al canto de arriba del fondo: sus filas son las primeras.
        Bitmap arriba = pintarConFondo(true, 1f);
        for (int y = 0; y < MACIZO - 1; y++) igual("arriba, y=" + y, fondo.getPixel(4, y), arriba.getPixel(4, y));
        transparente("arriba, la punta", arriba.getPixel(4, ALTO - 1));
        // El de abajo, al canto de abajo: sus filas son las últimas.
        Bitmap abajo = pintarConFondo(false, 1f);
        for (int y = ALTO - MACIZO + 1; y < ALTO; y++) {
            igual("abajo, y=" + y, fondo.getPixel(4, ALTO_FONDO - ALTO + y), abajo.getPixel(4, y));
        }
        transparente("abajo, la punta", abajo.getPixel(4, 0));
    }

    // Al irse a una pestaña sin fondo ambiental, el borde vuelve al color liso de esa pestaña.
    @Test
    public void sin_mezcla_el_borde_es_el_color_liso() {
        Bitmap abajo = pintarConFondo(false, 0f);
        for (int y = ALTO - MACIZO + 1; y < ALTO; y++) macizo("y=" + y, abajo.getPixel(4, y));
    }
}
