package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

// ============================================================
// PrimerosPasosTest — se marcan solos, se ocultan y desaparecen al completarse (GP-103).
// ============================================================
public class PrimerosPasosTest {

    @Test
    public void elPrimerDiaSaleConElPlanHecho() {
        PrimerosPasos p = new PrimerosPasos(true, false, false);
        assertEquals(1, p.hechos());
        assertTrue(p.visible(true, false));
    }

    @Test
    public void soloSaleElPrimerDiaYSiNoSeOculto() {
        PrimerosPasos p = new PrimerosPasos(true, true, false);
        assertFalse(p.visible(false, false));
        assertFalse(p.visible(true, true));
    }

    @Test
    public void desapareceAlCompletarse() {
        PrimerosPasos p = new PrimerosPasos(true, true, true);
        assertTrue(p.completos());
        assertFalse(p.visible(true, false));
    }
}
