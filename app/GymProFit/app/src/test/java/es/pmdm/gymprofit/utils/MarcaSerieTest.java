package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import es.pmdm.gymprofit.R;

// ============================================================
// MarcaSerieTest — una serie hecha y una sin hacer se distinguen por la forma y por lo que dice
// TalkBack, no solo por el color (GP-252, DEC-019): sin hacer, el aro (la que toca, el suyo en el
// color principal, nunca un círculo relleno); hecha, el círculo relleno con borde.
// ============================================================
public class MarcaSerieTest {

    @Test
    public void sinHacerEsElAroDeApuntarEntreno() {
        assertEquals(R.drawable.bg_check_pendiente, MarcaSerie.fondo(false, false));
        assertEquals(R.string.envivo_sin_hacer, MarcaSerie.estado(false));
        assertEquals(R.string.apuntar_apuntar_serie_a11y, MarcaSerie.nombre(false));
    }

    @Test
    public void laQueTocaEsUnAroYNoUnCirculoRelleno() {
        assertEquals(R.drawable.bg_check_toca, MarcaSerie.fondo(false, true));
    }

    @Test
    public void hechaEsElCirculoRellenoYLoDice() {
        assertEquals(R.drawable.bg_check_hecha, MarcaSerie.fondo(true, false));
        assertEquals(R.drawable.bg_check_hecha, MarcaSerie.fondo(true, true));
        assertEquals(R.string.envivo_hecha, MarcaSerie.estado(true));
        assertEquals(R.string.apuntar_serie_apuntada_a11y, MarcaSerie.nombre(true));
    }
}
