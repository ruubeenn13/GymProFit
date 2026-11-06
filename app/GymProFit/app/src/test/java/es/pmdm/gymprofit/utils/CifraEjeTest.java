package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Locale;

/**
 * GP-225 (revisor): con la app en inglés y el sistema en español, el eje Y de Medidas y de la ficha
 * decía «79,2» junto a «79.2 kg»: MPAndroidChart formatea con el idioma del sistema. Las cifras del
 * eje van en el idioma de la app, con los decimales que pide el eje.
 */
public class CifraEjeTest {

    @Test
    public void enInglesConPunto() {
        assertEquals("79.2", ChartStyler.cifraEje(79.2f, 1, Locale.US));
        assertEquals("1,250", ChartStyler.cifraEje(1250f, 0, Locale.US));
    }

    @Test
    public void enEspanolConComa() {
        assertEquals("79,2", ChartStyler.cifraEje(79.2f, 1, new Locale("es", "ES")));
        assertEquals("87,0", ChartStyler.cifraEje(87f, 1, new Locale("es", "ES")));
    }
}
