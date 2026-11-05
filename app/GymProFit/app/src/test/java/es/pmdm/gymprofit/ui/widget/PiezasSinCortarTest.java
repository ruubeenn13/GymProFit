package es.pmdm.gymprofit.ui.widget;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

// ============================================================
// PiezasSinCortarTest — GP-223: con el teclado abierto, ninguna pieza de Entrar queda a
// medias en los bordes de la vista con scroll. Medidas de Entrar en el emulador (1080 x
// 2400, letra 1,0), relativas al contenido: la vista mide 650 px con el teclado y el scroll
// llega hasta 243. Título 0–118, texto 139–194, bloque del usuario 257–467, bloque de la
// contraseña 504–714 y «¿Has olvidado tu contraseña?» 725–851. Con el foco en la
// contraseña, el sistema baja lo justo: 64 px, y el título queda a medias (cuarta pasada
// del revisor); bajando hasta el texto, 139, quedaba a medias el enlace (quinta).
// ============================================================
public class PiezasSinCortarTest {

    private static final int ALTO = 650, MAXIMO = 243;
    private static final int[] ARRIBA = {0, 139, 257, 504, 725};
    private static final int[] ABAJO = {118, 194, 467, 714, 851};

    private static int destino(int scroll, int focoArriba, int focoAbajo) {
        return PiezasSinCortar.destino(scroll, ALTO, MAXIMO, ARRIBA, ABAJO, focoArriba, focoAbajo);
    }

    @Test
    public void con_el_foco_en_la_contrasena_ni_el_titulo_ni_el_enlace_quedan_a_medias() {
        // 201: arriba, el texto ya fuera; abajo, el enlace entero hasta su borde.
        assertEquals(201, destino(64, 504, 714));
    }

    @Test
    public void de_varias_posiciones_sin_cortes_va_a_la_mas_cercana() {
        // Piezas 0–100, 200–300 y 400–500 en una vista de 250. Desde 50 (la primera a
        // medias) valen 100 y 150; la más cercana es 100.
        int[] arriba = {0, 200, 400}, abajo = {100, 300, 500};
        assertEquals(100, PiezasSinCortar.destino(50, 250, 300, arriba, abajo, 200, 300));
    }

    @Test
    public void sin_ninguna_pieza_a_medias_no_se_toca() {
        assertEquals(201, destino(201, 504, 714));
        assertEquals(230, destino(230, 504, 714));
    }

    @Test
    public void con_el_foco_en_el_usuario_va_donde_no_corta_nada() {
        // Desde 40, con el título a medias: 0 corta la contraseña abajo y 118, 139 y 194
        // cortan el enlace. 201 es la más cercana con el usuario entero.
        assertEquals(201, destino(40, 257, 467));
    }

    @Test
    public void si_no_hay_posicion_sin_cortes_con_el_foco_entero_no_se_toca() {
        // Un foco más alto que la vista: ninguna posición lo enseña entero.
        assertEquals(64, destino(64, 100, 800));
    }

    @Test
    public void nunca_pasa_del_maximo_del_scroll() {
        int[] arriba = {0, 300}, abajo = {280, 900};
        // La única sin cortes con el foco entero sería 900 - 650 = 250 > máximo 200: nada.
        assertEquals(100, PiezasSinCortar.destino(100, 650, 200, arriba, abajo, 300, 900));
    }
}
