package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.math.BigDecimal;
import java.util.Map;

// ============================================================
// PerfilAltaTest — el perfil que sube el alta (GP-103).
// ============================================================
public class PerfilAltaTest {

    @Test
    public void conSobreTiVaElPerfilEntero() {
        Map<String, Object> b = PerfilAlta.cuerpo(new AltaPasos.Respuestas("GANAR_MASA_MUSCULAR", "INTERMEDIO",
                "MUJER", 29, 165, "62.5", "MODERADO", false, "GIMNASIO", 3, 45));
        assertEquals("INTERMEDIO", b.get("nivelExperiencia"));
        assertEquals("GANAR_MASA_MUSCULAR", b.get("objetivo"));
        assertEquals("MUJER", b.get("sexo"));
        assertEquals("MODERADO", b.get("nivelActividad"));
        assertEquals(29, b.get("edad"));
        assertEquals(0, new BigDecimal("62.5").compareTo((BigDecimal) b.get("peso")));
        assertEquals(0, new BigDecimal("165").compareTo((BigDecimal) b.get("altura")));
    }

    @Test
    public void prefieroNoDecirloNoMandaNadaDelCuerpo() {
        Map<String, Object> b = PerfilAlta.cuerpo(new AltaPasos.Respuestas("PERDER_PESO", "PRINCIPIANTE",
                "", 0, 0, "", "", true, "PESO_CORPORAL", 2, 30));
        assertTrue(b.containsKey("objetivo"));
        assertTrue(b.containsKey("nivelExperiencia"));
        for (String campo : new String[]{"sexo", "nivelActividad", "edad", "altura", "peso"}) {
            assertFalse(campo, b.containsKey(campo));
        }
    }

    @Test
    public void elNivelVaEnTres() {
        Map<String, Object> b = PerfilAlta.cuerpo(new AltaPasos.Respuestas("", "EXPERTO",
                "", 0, 0, "", "", true, "", 0, 0));
        assertEquals("AVANZADO", b.get("nivelExperiencia"));
    }
}
