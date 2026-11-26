package es.pmdm.gymprofit.ui.widget;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * La barra de la sesión en dos líneas (GP-285): se parte por el primer separador y ninguna de
 * las dos líneas se queda con un «·» colgando.
 */
public class TextoBarraSesionTest {

    @Test
    public void parte_por_el_primer_separador_y_lo_quita() {
        assertEquals("Sesión en curso\nTorso A \u00B7 23:14",
                TextoBarraSesion.enDosLineas("Sesión en curso \u00B7 Torso A \u00B7 23:14"));
    }

    @Test
    public void ninguna_linea_empieza_ni_acaba_en_separador() {
        for (String linea : TextoBarraSesion.enDosLineas("Workout in progress \u00B7 Upper A \u00B7 0:08").split("\n")) {
            assertEquals(linea, linea.trim());
            assertEquals(false, linea.startsWith("\u00B7") || linea.endsWith("\u00B7"));
        }
        assertEquals(2, TextoBarraSesion.enDosLineas("Workout in progress \u00B7 Upper A \u00B7 0:08").split("\n").length);
    }

    @Test
    public void con_un_solo_separador_quedan_dos_lineas_limpias() {
        assertEquals("Sin guardar\nPierna B", TextoBarraSesion.enDosLineas("Sin guardar \u00B7 Pierna B"));
    }

    @Test
    public void sin_separador_no_se_toca() {
        assertEquals("Guardando", TextoBarraSesion.enDosLineas("Guardando"));
    }
}
