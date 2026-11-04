package es.pmdm.gymprofit.ui.widget;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

// ============================================================
// TituloSinCortarTest — GP-223: con el teclado abierto, el título de Entrar no queda a
// medias bajo el aviso. Medidas de Entrar en el emulador (1080 x 2400, letra 1,0): la
// vista con scroll mide 650 px, el título va de 0 a 118, el texto empieza en 139 y la
// contraseña va de 504 a 714. El sistema baja lo justo para enseñarla: 64 px.
// ============================================================
public class TituloSinCortarTest {

    private static final int ALTO = 650, TITULO_ARRIBA = 0, TITULO_ABAJO = 118, SIGUIENTE = 139;

    private static int destino(int scroll, int focoArriba, int focoAbajo) {
        return TituloSinCortar.destino(scroll, ALTO, TITULO_ARRIBA, TITULO_ABAJO, SIGUIENTE,
                focoArriba, focoAbajo);
    }

    @Test
    public void titulo_a_medias_y_no_cabe_entero_queda_fuera_entero() {
        assertEquals(SIGUIENTE, destino(64, 504, 714));
    }

    @Test
    public void titulo_a_medias_y_cabe_entero_con_el_foco_se_ve_entero() {
        // El usuario con el foco: cabe con el título entero.
        assertEquals(TITULO_ARRIBA, destino(40, 257, 467));
    }

    @Test
    public void titulo_entero_o_fuera_no_se_toca() {
        assertEquals(0, destino(0, 504, 714));
        assertEquals(200, destino(200, 504, 714));
        assertEquals(TITULO_ABAJO, destino(TITULO_ABAJO, 504, 714));
    }

    @Test
    public void si_el_foco_no_cabe_de_ninguna_forma_no_se_toca() {
        // Un campo que llega más abajo de lo que se ve aun con el título fuera.
        assertEquals(64, destino(64, 600, 900));
    }
}
