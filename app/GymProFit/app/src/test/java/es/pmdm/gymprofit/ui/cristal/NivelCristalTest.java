package es.pmdm.gymprofit.ui.cristal;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

// ============================================================
// NivelCristalTest — GP-285, DEC-070: el nivel del cristal vivo sale de tres datos y de nada más.
//
//   · La versión de Android: 33 o más, desenfoque y refracción; 31 y 32, solo desenfoque; 30 o
//     menos, superficie opaca (no hay RenderEffect).
//   · El texto de alto contraste del sistema: quien lo enciende pide leer, no ver a través.
//   · El techo de rendimiento: lo mejor que el móvil ha demostrado aguantar (RendimientoCristal).
// ============================================================
public class NivelCristalTest {

    private static final NivelCristal SIN_TECHO = NivelCristal.REFRACCION;

    @Test
    public void android_13_o_mas_desenfoca_y_refracta() {
        assertEquals(NivelCristal.REFRACCION, NivelCristal.elegir(33, false, SIN_TECHO));
        assertEquals(NivelCristal.REFRACCION, NivelCristal.elegir(36, false, SIN_TECHO));
    }

    @Test
    public void android_12_solo_desenfoca() {
        assertEquals(NivelCristal.DESENFOQUE, NivelCristal.elegir(31, false, SIN_TECHO));
        assertEquals(NivelCristal.DESENFOQUE, NivelCristal.elegir(32, false, SIN_TECHO));
    }

    @Test
    public void android_11_o_menos_es_opaco() {
        assertEquals(NivelCristal.OPACO, NivelCristal.elegir(24, false, SIN_TECHO));
        assertEquals(NivelCristal.OPACO, NivelCristal.elegir(30, false, SIN_TECHO));
    }

    @Test
    public void el_alto_contraste_fuerza_el_opaco_en_cualquier_version() {
        for (int api : new int[]{24, 30, 31, 32, 33, 36}) {
            assertEquals("API " + api, NivelCristal.OPACO, NivelCristal.elegir(api, true, SIN_TECHO));
        }
    }

    @Test
    public void el_techo_de_rendimiento_baja_el_nivel_pero_nunca_lo_sube() {
        // Un móvil con Android 16 que no aguanta la refracción se queda en el desenfoque…
        assertEquals(NivelCristal.DESENFOQUE, NivelCristal.elegir(36, false, NivelCristal.DESENFOQUE));
        // …y si tampoco aguanta el desenfoque, en el opaco.
        assertEquals(NivelCristal.OPACO, NivelCristal.elegir(36, false, NivelCristal.OPACO));
        assertEquals(NivelCristal.OPACO, NivelCristal.elegir(32, false, NivelCristal.OPACO));
        // El techo no da lo que la versión no tiene.
        assertEquals(NivelCristal.DESENFOQUE, NivelCristal.elegir(32, false, NivelCristal.REFRACCION));
        assertEquals(NivelCristal.OPACO, NivelCristal.elegir(30, false, NivelCristal.DESENFOQUE));
    }

    @Test
    public void bajar_un_nivel_se_para_en_el_opaco() {
        assertEquals(NivelCristal.DESENFOQUE, NivelCristal.REFRACCION.unoMenos());
        assertEquals(NivelCristal.OPACO, NivelCristal.DESENFOQUE.unoMenos());
        assertEquals(NivelCristal.OPACO, NivelCristal.OPACO.unoMenos());
    }

    @Test
    public void el_techo_guardado_se_lee_por_su_numero_y_lo_desconocido_no_limita() {
        assertEquals(NivelCristal.REFRACCION, NivelCristal.deNumero(1));
        assertEquals(NivelCristal.DESENFOQUE, NivelCristal.deNumero(2));
        assertEquals(NivelCristal.OPACO, NivelCristal.deNumero(3));
        assertEquals(NivelCristal.REFRACCION, NivelCristal.deNumero(0));
        assertEquals(NivelCristal.REFRACCION, NivelCristal.deNumero(99));
        assertEquals(2, NivelCristal.DESENFOQUE.numero);
    }
}
