package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import es.pmdm.gymprofit.R;

// ============================================================
// MarcaSerieTest — una serie hecha y una sin hacer se distinguen por la forma y por lo que dice
// TalkBack, no solo por el color (GP-252, DEC-019): sin hacer, el aro; hecha, el círculo relleno.
// ============================================================
public class MarcaSerieTest {

    @Test
    public void sinHacerEsElAroDeApuntarEntreno() {
        assertEquals(R.drawable.bg_check_pendiente, MarcaSerie.fondo(false));
        assertEquals(R.string.envivo_sin_hacer, MarcaSerie.estado(false));
    }

    @Test
    public void hechaEsElCirculoRellenoYLoDice() {
        assertEquals(R.drawable.bg_check, MarcaSerie.fondo(true));
        assertEquals(R.string.envivo_hecha, MarcaSerie.estado(true));
    }
}
