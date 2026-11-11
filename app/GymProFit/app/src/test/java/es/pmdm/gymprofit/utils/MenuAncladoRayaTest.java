package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

// El menú anclado (revisor, lote 12): con una sola opción, destructiva («Quitar ejercicio» en la
// sesión en vivo), su raya se juntaba con la de la cabecera en una doble raya.
public class MenuAncladoRayaTest {

    @Test
    public void la_raya_de_lo_destructivo_solo_va_si_hay_opciones_encima() {
        assertFalse("sola, sin raya", UIHelper.rayaAntes(true, false, 0));
        assertTrue("tras opciones normales, con raya", UIHelper.rayaAntes(true, false, 3));
        assertFalse("una sola raya aunque haya dos destructivas", UIHelper.rayaAntes(true, true, 4));
        assertFalse("las normales no llevan raya", UIHelper.rayaAntes(false, false, 2));
    }

    // Las dos rayas del mismo grosor (revisor, lote 13): la de la cabecera es un 1dp del XML,
    // que Android redondea (3 px a 2,625), y la de antes de lo destructivo se truncaba a 2 px.
    @Test
    public void la_raya_de_lo_destructivo_redondea_como_la_del_xml() {
        assertEquals(3, UIHelper.grosorRaya(2.625f));
        assertEquals(3, UIHelper.grosorRaya(3f));
        assertEquals(2, UIHelper.grosorRaya(2f));
        assertEquals("nunca desaparece", 1, UIHelper.grosorRaya(0.75f));
    }
}
