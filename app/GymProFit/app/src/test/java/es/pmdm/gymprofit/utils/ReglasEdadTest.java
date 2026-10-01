package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

// ============================================================
// ReglasEdadTest — GP-145: la edad de 14 a 100, sin romper con cualquier texto.
// ============================================================
public class ReglasEdadTest {

    @Test
    public void deCatorceACienEsValida() {
        assertEquals(ReglasEdad.Estado.VALIDA, ReglasEdad.estado("14"));
        assertEquals(ReglasEdad.Estado.VALIDA, ReglasEdad.estado(" 100 "));
        assertEquals(Integer.valueOf(29), ReglasEdad.leer("29"));
    }

    @Test
    public void porDebajoDeCatorceEsMenorYNoSeLee() {
        assertEquals(ReglasEdad.Estado.MENOR, ReglasEdad.estado("13"));
        assertEquals(ReglasEdad.Estado.MENOR, ReglasEdad.estado("0"));
        assertEquals(ReglasEdad.Estado.MENOR, ReglasEdad.estado("-5"));
        assertNull(ReglasEdad.leer("13"));
    }

    @Test
    public void cualquierOtroTextoQuedaFueraSinRomper() {
        assertEquals(ReglasEdad.Estado.FUERA, ReglasEdad.estado("101"));
        assertEquals(ReglasEdad.Estado.FUERA, ReglasEdad.estado("99999999999999999999"));
        assertEquals(ReglasEdad.Estado.FUERA, ReglasEdad.estado("veinte"));
        assertEquals(ReglasEdad.Estado.FUERA, ReglasEdad.estado("29,5"));
        assertNull(ReglasEdad.leer("99999999999999999999"));
    }

    @Test
    public void enBlancoEsVacia() {
        assertEquals(ReglasEdad.Estado.VACIA, ReglasEdad.estado(null));
        assertEquals(ReglasEdad.Estado.VACIA, ReglasEdad.estado("   "));
    }

    @Test
    public void elCodigoDeLaApiSeReconoce() {
        assertTrue(ReglasEdad.esEdadMinima(
                "{\"code\":400,\"message\":\"La edad mínima es 14 años\",\"cause\":\"EDAD_MINIMA\"}"));
        assertFalse(ReglasEdad.esEdadMinima("{\"cause\":\"USERNAME_NO_VALIDO\"}"));
        assertFalse(ReglasEdad.esEdadMinima(null));
    }

    @Test
    public void elMinimoEsElDeLaApi() {
        // DEC-038: ReglasPerfil.EDAD_MINIMA de la API es 14. Si cambia, cambia aquí.
        assertEquals(14, ReglasEdad.MINIMA);
    }
}
