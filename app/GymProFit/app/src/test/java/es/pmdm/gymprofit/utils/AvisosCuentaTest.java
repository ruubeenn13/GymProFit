package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Map;

// ============================================================
// AvisosCuentaTest — los tres avisos de la cuenta y sus valores de serie (DEC-037).
// ============================================================
public class AvisosCuentaTest {

    @Test
    public void deSerieEntrenarSiComidasNoProgresoSi() {
        AvisosCuenta.Estado e = AvisosCuenta.Estado.deSerie();
        assertTrue(e.entrenar);
        assertFalse(e.comidas);
        assertTrue(e.progreso);
    }

    @Test
    public void loQueNoMandaLaApiEsDeSerie() {
        AvisosCuenta.Estado e = AvisosCuenta.Estado.de(false, null, null);
        assertFalse(e.entrenar);
        assertFalse(e.comidas);
        assertTrue(e.progreso);
    }

    @Test
    public void elPatchLlevaLosTresConLosNombresDeLaApi() {
        Map<String, Object> b = AvisosCuenta.cuerpo(new AvisosCuenta.Estado(true, true, false));
        assertEquals(3, b.size());
        assertEquals(Boolean.TRUE, b.get("avisosEntrenar"));
        assertEquals(Boolean.TRUE, b.get("avisosComidas"));
        assertEquals(Boolean.FALSE, b.get("avisosProgreso"));
    }
}
