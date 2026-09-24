package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

// ============================================================
// ValoracionTest — GP-077: sin tocar las estrellas no se manda valoración.
// ============================================================
public class ValoracionTest {

    @Test
    public void sin_estrellas_no_se_manda_nada() {
        // El caso que motivó GP-077: quien no toca el RatingBar no ha valorado.
        assertNull(Valoracion.paraEnviar(0f));
    }

    @Test
    public void de_una_a_cinco_se_manda_tal_cual() {
        for (int i = 1; i <= 5; i++) {
            assertEquals(Integer.valueOf(i), Valoracion.paraEnviar(i));
        }
    }

    @Test
    public void fuera_de_rango_no_se_manda() {
        assertNull(Valoracion.paraEnviar(-1f));
        assertNull(Valoracion.paraEnviar(6f));
    }
}
