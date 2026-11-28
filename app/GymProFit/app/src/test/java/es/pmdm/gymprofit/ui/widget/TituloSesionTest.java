package es.pmdm.gymprofit.ui.widget;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

// ============================================================
// TituloSesionTest — cuándo se ve el programa bajo el título de la Sesión en vivo (GP-285,
// remate del lote 2; la regla de GP-288). Solo las cuentas; el caso medido de verdad, con la
// fuente y el ancho de un móvil, está en CabeceraSesionTest (instrumentado).
// ============================================================
public class TituloSesionTest {

    // El caso de las capturas: «Cuerpo completo» bajo «Cuerpo completo C».
    @Test
    public void el_programa_repite_el_titulo_si_sus_palabras_ya_estan_en_el() {
        assertTrue(TituloSesion.repite("Cuerpo completo C", "Cuerpo completo"));
        assertTrue(TituloSesion.repite("Cuerpo completo", "Cuerpo completo"));
        assertTrue("sin contar mayúsculas ni tildes", TituloSesion.repite("TORSO Á", "torso a"));
        assertTrue("ni espacios de más o duros", TituloSesion.repite("Full  body C", " full body "));
    }

    @Test
    public void no_lo_repite_si_dice_otra_cosa() {
        assertFalse(TituloSesion.repite("Torso A", "Torso y pierna"));
        assertFalse("media palabra no es repetir", TituloSesion.repite("Torsos", "Torso"));
        assertFalse(TituloSesion.repite("Pierna", "Empuje, tirón, pierna"));
        assertFalse("sin programa no hay nada que repetir", TituloSesion.repite("Pierna", ""));
        assertFalse(TituloSesion.repite("Pierna", null));
        assertFalse(TituloSesion.repite(null, "Pierna"));
    }

    @Test
    public void por_debajo_de_letra_1_5_el_programa_se_ve_siempre() {
        for (float escala : new float[]{ 0.85f, 1.0f, 1.15f, 1.3f }) {
            assertTrue(TituloSesion.programaVisible(escala, true, false));
            assertTrue(TituloSesion.programaVisible(escala, false, true));
        }
    }

    @Test
    public void desde_letra_1_5_solo_si_no_repite_el_titulo_y_cabe_en_una_linea() {
        for (float escala : new float[]{ 1.5f, 1.8f, 2.0f }) {
            assertFalse("repite el título", TituloSesion.programaVisible(escala, true, true));
            assertFalse("no cabe", TituloSesion.programaVisible(escala, false, false));
            assertTrue(TituloSesion.programaVisible(escala, false, true));
        }
    }
}
