package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

// ============================================================
// EstadoEscanerTest — qué hace el escáner con cada lectura y cada respuesta (lote 1.6.1)
// Busca hasta leer un código; entonces pregunta y, mientras hay hoja (producto, no
// existe, sin cupo, sin red), no lee otro. «Escanear otro» vuelve a buscar; reintentar
// pregunta otra vez por el mismo código. Una respuesta que llega tarde, de un código
// que ya no es el de la hoja, no cambia nada.
// ============================================================
public class EstadoEscanerTest {

    @Test
    public void empieza_buscando_y_la_camara_lee() {
        EstadoEscaner e = new EstadoEscaner();
        assertEquals(EstadoEscaner.Estado.BUSCANDO, e.estado());
        assertTrue(e.leeCamara());
        assertFalse(e.hayHoja());
    }

    @Test
    public void leer_un_codigo_pregunta_y_no_lee_otro() {
        EstadoEscaner e = new EstadoEscaner();
        assertTrue(e.leido("8410076472915"));
        assertEquals(EstadoEscaner.Estado.CONSULTANDO, e.estado());
        assertEquals("8410076472915", e.codigo());
        assertFalse("mientras pregunta, otra lectura no cuenta", e.leido("4006381333931"));
        assertEquals("8410076472915", e.codigo());
        assertFalse(e.leeCamara());
    }

    @Test
    public void con_producto_hay_hoja_y_no_se_lee() {
        EstadoEscaner e = new EstadoEscaner();
        e.leido("8410076472915");
        e.encontrado("8410076472915");
        assertEquals(EstadoEscaner.Estado.PRODUCTO, e.estado());
        assertTrue(e.hayHoja());
        assertFalse(e.leido("4006381333931"));
    }

    @Test
    public void escanear_otro_vuelve_a_buscar() {
        EstadoEscaner e = new EstadoEscaner();
        e.leido("8410076472915");
        e.noExiste("8410076472915");
        assertEquals(EstadoEscaner.Estado.NO_EXISTE, e.estado());
        e.otroMas();
        assertEquals(EstadoEscaner.Estado.BUSCANDO, e.estado());
        assertNull(e.codigo());
        assertTrue(e.leido("4006381333931"));
    }

    @Test
    public void sin_cupo_dice_cuanto_y_reintentar_pregunta_por_el_mismo() {
        EstadoEscaner e = new EstadoEscaner();
        e.leido("8410076472915");
        e.sinCupo("8410076472915", 42);
        assertEquals(EstadoEscaner.Estado.SIN_CUPO, e.estado());
        assertEquals(42, e.segundos());
        assertTrue(e.hayHoja());
        assertTrue(e.reintentar());
        assertEquals(EstadoEscaner.Estado.CONSULTANDO, e.estado());
        assertEquals("8410076472915", e.codigo());
    }

    @Test
    public void sin_red_se_puede_reintentar() {
        EstadoEscaner e = new EstadoEscaner();
        e.leido("8410076472915");
        e.sinRed("8410076472915");
        assertEquals(EstadoEscaner.Estado.SIN_RED, e.estado());
        assertTrue(e.reintentar());
    }

    @Test
    public void reintentar_solo_tras_un_fallo() {
        EstadoEscaner e = new EstadoEscaner();
        assertFalse(e.reintentar());
        e.leido("8410076472915");
        e.encontrado("8410076472915");
        assertFalse(e.reintentar());
    }

    @Test
    public void una_respuesta_tardia_de_otro_codigo_no_cambia_nada() {
        EstadoEscaner e = new EstadoEscaner();
        e.leido("8410076472915");
        e.otroMas();
        e.leido("4006381333931");
        e.encontrado("8410076472915");
        assertEquals(EstadoEscaner.Estado.CONSULTANDO, e.estado());
        assertEquals("4006381333931", e.codigo());
    }

    @Test
    public void un_codigo_escrito_entra_igual_que_uno_leido() {
        EstadoEscaner e = new EstadoEscaner();
        assertTrue(e.escrito("96385074"));
        assertEquals(EstadoEscaner.Estado.CONSULTANDO, e.estado());
    }
}
