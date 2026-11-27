package es.pmdm.gymprofit.ui.cristal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

// ============================================================
// BrilloMarcaTest — GP-285: dónde va el brillo de un botón de marca (Cristal.brilloMarca).
//
// En «Empezar» (ancho) el brillo se leía como un punto a la izquierda: va centrado y ocupa en
// torno al 60 % de su ancho. En el «+» (redondo) se queda como estaba, pequeño arriba a la
// izquierda. Las medidas son las de un móvil de 2,625 px por dp.
// ============================================================
public class BrilloMarcaTest {

    private static final float DP = 2.625f;

    @Test
    public void en_empezar_va_centrado_y_ocupa_en_torno_al_60_por_ciento() {
        float ancho = 160 * DP, alto = 48 * DP;
        float[] b = Cristal.brilloMarca(ancho, alto);
        assertEquals("centrado", ancho / 2f, b[0], 0.5f);
        float ocupa = 2 * b[2] / ancho;
        assertTrue("ocupa " + ocupa + " del ancho", ocupa >= 0.55f && ocupa <= 0.65f);
        // Con la altura y el alto del brillo del «+»: solo cambia a lo ancho.
        assertEquals(alto * Cristal.BRILLO_Y, b[1], 0.01f);
        assertEquals(alto * Cristal.BRILLO_RADIO, b[3], 0.01f);
    }

    @Test
    public void con_letra_grande_y_apilado_sigue_centrado() {
        // «Empezar» solo en su fila, a lo ancho de la tarjeta, y más alto por la letra 2,0.
        float ancho = 296 * DP, alto = 64 * DP;
        float[] b = Cristal.brilloMarca(ancho, alto);
        assertEquals(ancho / 2f, b[0], 0.5f);
        assertEquals(0.6f, 2 * b[2] / ancho, 0.05f);
    }

    @Test
    public void en_el_mas_se_queda_como_estaba() {
        float lado = 56 * DP;
        float[] b = Cristal.brilloMarca(lado, lado);
        assertEquals(lado * Cristal.BRILLO_X, b[0], 0.01f);
        assertEquals(lado * Cristal.BRILLO_Y, b[1], 0.01f);
        assertEquals(lado * Cristal.BRILLO_RADIO, b[2], 0.01f);
        assertEquals(b[2], b[3], 0f);
    }
}
