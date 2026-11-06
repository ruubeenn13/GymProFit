package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * GP-225: el tooltip de una gráfica no tapa el punto que señala ni las etiquetas de los ejes, y no
 * se sale: va dentro del área de datos (izq, arriba, der, abajo). Con sitio, encima; si arriba no
 * cabe (el récord, que es el punto más alto), debajo.
 */
public class ChartMarkerTest {

    private static final float W = 120, H = 60, HUECO = 20;
    // El área de datos: a la izquierda quedan las etiquetas del eje Y y abajo las del X.
    private static final float IZQ = 100, ARRIBA = 10, DER = 990, ABAJO = 400;

    private static float[] d(float x, float y) {
        return ChartMarker.desplazamiento(x, y, W, H, IZQ, ARRIBA, DER, ABAJO, HUECO);
    }

    @Test
    public void conSitioVaEncimaYCentrado() {
        float[] o = d(500, 300);
        assertEquals(-60, o[0], 0.01);
        assertEquals(-H - HUECO, o[1], 0.01);
    }

    @Test
    public void sinSitioArribaVaDebajoSinTaparElPunto() {
        float y = 40;
        float[] o = d(500, y);
        assertEquals(HUECO, o[1], 0.01);
        assertTrue(y + o[1] > y);
    }

    // Visto en el revisor: con letra 1,3 el tooltip del récord, en el último punto, se salía por
    // la derecha de la gráfica y quedaba cortado.
    @Test
    public void noSeSalePorLaDerecha() {
        float[] o = d(985, 300);
        assertEquals(DER, 985 + o[0] + W, 0.01);
    }

    // Y el del primer punto tapaba las cifras del eje Y.
    @Test
    public void noTapaLasEtiquetasDelEjeY() {
        float[] o = d(110, 300);
        assertEquals(IZQ, 110 + o[0], 0.01);
    }

    @Test
    public void debajoNoBajaDelAreaDeDatos() {
        // Ni arriba ni abajo cabe entero: se queda dentro por abajo, sin tapar el eje X.
        float[] o = d(500, 70);
        assertTrue(70 + o[1] + H <= ABAJO + 0.01);
    }
}
