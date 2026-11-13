package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

// ============================================================
// ResumenPerfilTest — GP-265, lo que enseñan «Tu objetivo» y «Tus datos» en Perfil
//
// El fallo: una cuenta sin objetivo (alta sin él, cuestionario sin acabar) veía el
// rótulo «Tu objetivo» con el nombre vacío, y «Tus datos» enseñaba 70 kg, 170 cm y
// «Moderada», que son los valores por defecto de PreferencesManager, no datos suyos.
// Aquí se decide qué es un objetivo de verdad y qué es un dato guardado.
// ============================================================
public class ResumenPerfilTest {

    @Test
    public void sinObjetivoNoHayNombreQuePintar() {
        // Antes: la tarjeta pintaba "" como nombre del objetivo.
        assertNull(ResumenPerfil.objetivo(null, ""));
        assertNull(ResumenPerfil.objetivo("", ""));
        assertNull(ResumenPerfil.objetivo(null, null));
        assertNull(ResumenPerfil.objetivo("  ", null));
        // Un valor que la app no conoce tampoco es un objetivo que enseñar.
        assertNull(ResumenPerfil.objetivo("REDUCIR_GRASA_CORPORAL", null));
    }

    @Test
    public void laApiMandaYLoGuardadoCubreMientrasLlega() {
        assertEquals("PERDER_PESO", ResumenPerfil.objetivo("PERDER_PESO", "MANTENER_PESO"));
        assertEquals("MANTENER_PESO", ResumenPerfil.objetivo(null, "MANTENER_PESO"));
        assertEquals("GANAR_MASA_MUSCULAR", ResumenPerfil.objetivo("ganar_masa_muscular", ""));
        assertEquals("MEJORAR_FUERZA", ResumenPerfil.objetivo(null, "MEJORAR_FUERZA"));
    }

    @Test
    public void lasFichasSoloConObjetivoYPlan() {
        assertTrue(ResumenPerfil.conFichas("PERDER_PESO", true));
        assertFalse(ResumenPerfil.conFichas("PERDER_PESO", false));
        assertFalse(ResumenPerfil.conFichas(null, true));
    }

    @Test
    public void unDatoSinGuardarNoSeInventa() {
        // Antes: getPeso() daba 70 y getAltura() 170 sin nada guardado.
        assertNull(ResumenPerfil.medida(null));
        assertNull(ResumenPerfil.medida("0.0"));
        assertNull(ResumenPerfil.medida("no"));
        assertEquals(82.5, ResumenPerfil.medida("82.5"), 0.001);
    }

    @Test
    public void laEdadDeLaApiOLaGuardadaNuncaLaDeSerie() {
        assertNull(ResumenPerfil.edad(null, null));
        assertNull(ResumenPerfil.edad(0, null));
        assertEquals(Integer.valueOf(30), ResumenPerfil.edad(30, null));
        assertEquals(Integer.valueOf(41), ResumenPerfil.edad(null, "41"));
        assertEquals(Integer.valueOf(30), ResumenPerfil.edad(30, "41"));
    }
}
