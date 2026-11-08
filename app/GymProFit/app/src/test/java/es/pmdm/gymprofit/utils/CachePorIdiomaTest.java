package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

// ============================================================
// CachePorIdiomaTest — lo guardado en memoria solo vale para el idioma en que se pidió (GP-251):
// el catálogo de ejercicios pedido en español no se enseña con la app ya en inglés.
// ============================================================
public class CachePorIdiomaTest {

    @Test
    public void loPedidoEnUnIdiomaNoSirveParaElOtro() {
        CachePorIdioma<String> c = new CachePorIdioma<>();
        c.put("es", "Press de banca");
        assertNull(c.get("en"));
        assertEquals("Press de banca", c.get("es"));
    }

    @Test
    public void alPedirloEnElOtroIdiomaSeQuedaSoloEse() {
        CachePorIdioma<String> c = new CachePorIdioma<>();
        c.put("es", "Press de banca");
        c.put("en", "Bench press");
        assertEquals("Bench press", c.get("en"));
        assertNull(c.get("es"));
    }

    @Test
    public void sinNadaGuardadoNoHayNada() {
        assertNull(new CachePorIdioma<String>().get("es"));
    }
}
