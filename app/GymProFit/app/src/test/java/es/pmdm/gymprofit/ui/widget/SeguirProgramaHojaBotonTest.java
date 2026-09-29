package es.pmdm.gymprofit.ui.widget;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import es.pmdm.gymprofit.R;

// ============================================================
// SeguirProgramaHojaBotonTest — GP-135: qué dice el botón de la hoja de seguir.
//
// Con otro programa, o sin ninguno, «Seguir». Con el mismo y otros minutos, «Cambiar a
// N min». Con el mismo y los minutos que ya tiene no cambia el tiempo: rehace las
// rutinas, y el botón lo dice.
// ============================================================
public class SeguirProgramaHojaBotonTest {

    @Test
    public void otro_programa_o_ninguno_dice_seguir() {
        assertEquals(R.string.seguir_boton, SeguirProgramaHoja.textoBoton(false, 0, 60));
        assertEquals(R.string.seguir_boton, SeguirProgramaHoja.textoBoton(false, 45, 45));
    }

    @Test
    public void el_mismo_con_otros_minutos_dice_cambiar() {
        assertEquals(R.string.seguir_boton_mismo, SeguirProgramaHoja.textoBoton(true, 45, 60));
    }

    @Test
    public void el_mismo_con_sus_minutos_dice_rehacer() {
        assertEquals(R.string.seguir_boton_rehacer, SeguirProgramaHoja.textoBoton(true, 60, 60));
    }
}
