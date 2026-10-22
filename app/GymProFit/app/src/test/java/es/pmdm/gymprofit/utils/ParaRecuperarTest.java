package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

// ============================================================
// ParaRecuperarTest — la regla de los 20 g de «Para recuperar» (GP-210 paso 1, decisión 12
// del lienzo de nutrición).
//
// Solo sale si a la proteína del día le faltan 20 g o más, contados como los enseña la
// pantalla (redondeados), y sin tocar los objetivos del día: lo que se apunta desde aquí
// solo suma a lo comido.
// ============================================================
public class ParaRecuperarTest {

    @Test
    public void con_20_g_que_faltan_sale() {
        assertTrue(ParaRecuperar.sale(136, 116, false));
    }

    @Test
    public void con_19_g_que_faltan_no_sale() {
        assertFalse(ParaRecuperar.sale(136, 117, false));
    }

    @Test
    public void se_cuenta_lo_que_enseña_la_pantalla_redondeado() {
        // 19,5 g que faltan se enseñan como «20 g»; 19,4, como «19 g».
        assertTrue(ParaRecuperar.sale(136, 116.5, false));
        assertFalse(ParaRecuperar.sale(136, 116.6, false));
        assertEquals(20, ParaRecuperar.falta(136, 116.5));
        assertEquals(19, ParaRecuperar.falta(136, 116.6));
    }

    @Test
    public void con_la_proteina_ya_conseguida_no_sale_y_no_hay_falta_negativa() {
        assertFalse(ParaRecuperar.sale(136, 140, false));
        assertEquals(0, ParaRecuperar.falta(136, 140));
        assertTrue(ParaRecuperar.conseguida(136, 136));
        assertFalse(ParaRecuperar.conseguida(136, 135.4));
    }

    @Test
    public void sin_objetivo_no_hay_nada_que_recuperar() {
        // «Prefiero no decirlo» (GP-103): no hay objetivo de proteína que completar.
        assertFalse(ParaRecuperar.sale(0, 0, true));
        assertFalse(ParaRecuperar.sale(136, 0, true));
        assertFalse(ParaRecuperar.sale(0, 10, false));
    }

    @Test
    public void apuntar_una_idea_acerca_el_total_sin_cambiar_el_objetivo() {
        // 82 de 136: faltan 54. Un yogur de 20 g deja 34; con el pollo de 35 g, conseguida.
        assertEquals(54, ParaRecuperar.falta(136, 82));
        assertEquals(34, ParaRecuperar.falta(136, 82 + 20));
        assertTrue(ParaRecuperar.conseguida(136, 82 + 20 + 35));
    }
}
