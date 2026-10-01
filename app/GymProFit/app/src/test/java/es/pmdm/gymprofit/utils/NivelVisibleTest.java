package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

// ============================================================
// NivelVisibleTest — el nivel en tres, sin cambiar lo guardado (GP-103).
// ============================================================
public class NivelVisibleTest {

    @Test
    public void expertoSeEnsenaComoAvanzado() {
        assertEquals("AVANZADO", NivelVisible.valor("EXPERTO"));
        assertEquals("AVANZADO", NivelVisible.valor("experto"));
        assertEquals("INTERMEDIO", NivelVisible.valor("INTERMEDIO"));
        assertEquals("", NivelVisible.valor(null));
    }

    @Test
    public void unExpertoQueNoCambiaSigueSiendoExperto() {
        assertEquals("EXPERTO", NivelVisible.aGuardar("AVANZADO", "EXPERTO"));
        assertEquals("INTERMEDIO", NivelVisible.aGuardar("INTERMEDIO", "EXPERTO"));
        assertEquals("AVANZADO", NivelVisible.aGuardar("AVANZADO", "INTERMEDIO"));
        assertEquals("AVANZADO", NivelVisible.aGuardar("AVANZADO", null));
    }

    @Test
    public void seOfrecenTres() {
        assertEquals(3, NivelVisible.OFRECIDOS.length);
    }
}
