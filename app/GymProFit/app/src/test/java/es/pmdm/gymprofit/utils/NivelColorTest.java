package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import es.pmdm.gymprofit.R;

// ============================================================
// NivelColorTest — el punto de color del nivel en la Biblioteca (GP-216, tablero 29): verde,
// ámbar y rojo; Experto como Avanzado (GP-103); lo desconocido, ámbar y no verde ni rojo.
// ============================================================
public class NivelColorTest {

    @Test
    public void principianteEsVerde() {
        assertEquals(R.color.gp_success, NivelColor.de("PRINCIPIANTE"));
    }

    @Test
    public void intermedioEsAmbar() {
        assertEquals(R.color.gp_nivel_intermedio, NivelColor.de("INTERMEDIO"));
    }

    @Test
    public void avanzadoYExpertoSonRojos() {
        assertEquals(R.color.gp_error, NivelColor.de("AVANZADO"));
        assertEquals(R.color.gp_error, NivelColor.de("EXPERTO"));
    }

    @Test
    public void noDistingueMayusculas() {
        assertEquals(R.color.gp_success, NivelColor.de("principiante"));
    }

    @Test
    public void sinNivelNiVerdeNiRojo() {
        assertEquals(R.color.gp_nivel_intermedio, NivelColor.de(null));
        assertEquals(R.color.gp_nivel_intermedio, NivelColor.de("otro"));
    }
}
