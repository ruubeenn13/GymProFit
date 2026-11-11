package es.pmdm.gymprofit.utils;

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
}
