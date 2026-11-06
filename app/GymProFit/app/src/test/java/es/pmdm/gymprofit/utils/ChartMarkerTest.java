package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * GP-225: el tooltip de una gráfica no tapa el punto que señala. Con sitio, va encima; si arriba
 * no cabe (el récord, que es el punto más alto), va debajo, y nunca se sale por los lados.
 */
public class ChartMarkerTest {

    private static final float W = 120, H = 60, ANCHO = 1000, ALTO = 400, HUECO = 20;

    @Test
    public void conSitioVaEncimaYCentrado() {
        float[] o = ChartMarker.desplazamiento(500, 300, W, H, ANCHO, ALTO, HUECO);
        assertEquals(-60, o[0], 0.01);
        assertEquals(-H - HUECO, o[1], 0.01);
    }

    @Test
    public void sinSitioArribaVaDebajoSinTaparElPunto() {
        float y = 30;
        float[] o = ChartMarker.desplazamiento(500, y, W, H, ANCHO, ALTO, HUECO);
        // La burbuja empieza por debajo del punto, con su hueco.
        assertEquals(HUECO, o[1], 0.01);
        assertTrue(y + o[1] > y);
    }

    @Test
    public void noSeSalePorLosLados() {
        float[] izq = ChartMarker.desplazamiento(10, 300, W, H, ANCHO, ALTO, HUECO);
        assertEquals(-10, izq[0], 0.01);
        float[] der = ChartMarker.desplazamiento(990, 300, W, H, ANCHO, ALTO, HUECO);
        assertEquals(ANCHO - W - 990, der[0], 0.01);
    }
}
