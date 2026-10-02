package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

// ============================================================
// CodigoBarrasTest — «Escribir el código» valida antes de preguntar (lote 1.6.1)
// La longitud (8, 12 o 13 cifras: EAN-8 o UPC-E, UPC-A y EAN-13) y el dígito
// de control de EAN-13, EAN-8, UPC-A y UPC-E. Un código mal escrito no gasta una
// petición ni cupo de Open Food Facts.
// ============================================================
public class CodigoBarrasTest {

    @Test
    public void ean13_valido() {
        assertTrue(CodigoBarras.valido("8410076472915"));
        assertTrue(CodigoBarras.valido("4006381333931"));
    }

    @Test
    public void ean13_con_el_digito_cambiado_no_vale() {
        assertFalse(CodigoBarras.valido("8410076472916"));
        assertEquals(CodigoBarras.Problema.DIGITO, CodigoBarras.problema("8410076472916"));
    }

    @Test
    public void ean8_valido_e_invalido() {
        assertTrue(CodigoBarras.valido("96385074"));
        assertFalse(CodigoBarras.valido("96385075"));
    }

    @Test
    public void upca_valido_e_invalido() {
        assertTrue(CodigoBarras.valido("036000291452"));
        assertFalse(CodigoBarras.valido("036000291453"));
    }

    @Test
    public void upce_valido_se_comprueba_expandido() {
        // 0 425261 4 ↔ UPC-A 042100005264.
        assertTrue(CodigoBarras.valido("04252614"));
        assertFalse(CodigoBarras.valido("04252615"));
        assertTrue(CodigoBarras.valido("042100005264"));
    }

    @Test
    public void longitudes_que_no_son_de_producto() {
        assertEquals(CodigoBarras.Problema.LONGITUD, CodigoBarras.problema("12345"));
        assertEquals(CodigoBarras.Problema.LONGITUD, CodigoBarras.problema("12345678901234"));
        assertEquals(CodigoBarras.Problema.LONGITUD, CodigoBarras.problema(""));
    }

    @Test
    public void solo_cifras() {
        assertEquals(CodigoBarras.Problema.CIFRAS, CodigoBarras.problema("84100764729a5"));
        assertEquals(CodigoBarras.Problema.LONGITUD, CodigoBarras.problema(null));
    }

    @Test
    public void limpia_espacios_y_guiones_de_lo_escrito() {
        assertEquals("8410076472915", CodigoBarras.limpiar(" 8 410076 472915 "));
        assertEquals("8410076472915", CodigoBarras.limpiar("8410076-472915"));
        assertNull(CodigoBarras.problema(CodigoBarras.limpiar("8 410076 472915")));
    }
}
