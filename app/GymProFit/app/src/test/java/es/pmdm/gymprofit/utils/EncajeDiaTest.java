package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

// ============================================================
// EncajeDiaTest — «Cómo encaja en tu día» y el reparto de la ficha (lote 1.6.1)
// De lo que llevas a lo que llevarías, en kcal y proteína, y lo que te quedará. Si te
// pasas, se dice (sin rojo: eso es cosa de la vista). Sin objetivo, no hay encaje.
// Al editar una línea, lo que ya contaba esa línea no se suma dos veces.
// ============================================================
public class EncajeDiaTest {

    private static final double D = 0.01;

    @Test
    public void el_ejemplo_del_lienzo() {
        // Lucía: 1.236 de 2.397 kcal y 82 de 136 g; un yogur de 120 kcal y 20 g.
        EncajeDia e = EncajeDia.de(1236, 82, 2397, 136, 120, 20, 0, 0);
        assertEquals(1356, e.despuesKcal);
        assertEquals(102, e.despuesProteina, D);
        assertEquals(1041, e.quedanKcal);
        assertEquals(34, e.quedanProteina, D);
        assertFalse(e.sePasaKcal);
        assertFalse(e.sePasaProteina);
    }

    @Test
    public void las_barras_en_porcentaje_de_antes_y_de_lo_que_suma() {
        EncajeDia e = EncajeDia.de(1236, 82, 2397, 136, 120, 20, 0, 0);
        assertEquals(51.6, e.antesKcalPct, 0.1);
        assertEquals(5.0, e.sumaKcalPct, 0.1);
        assertEquals(60.3, e.antesProteinaPct, 0.1);
        assertEquals(14.7, e.sumaProteinaPct, 0.1);
    }

    @Test
    public void si_te_pasas_lo_dice_y_no_quedan_negativos() {
        EncajeDia e = EncajeDia.de(2300, 130, 2397, 136, 300, 10, 0, 0);
        assertTrue(e.sePasaKcal);
        assertEquals(0, e.quedanKcal);
        assertEquals(203, e.pasaKcalPor);
        assertTrue(e.sePasaProteina);
        assertEquals(0, e.quedanProteina, D);
        // Las barras no pasan del 100 %.
        assertEquals(100.0, e.antesKcalPct + e.sumaKcalPct, 0.1);
    }

    @Test
    public void ya_pasado_antes_de_anadir() {
        EncajeDia e = EncajeDia.de(2500, 50, 2397, 136, 100, 5, 0, 0);
        assertEquals(100.0, e.antesKcalPct, 0.1);
        assertEquals(0.0, e.sumaKcalPct, 0.1);
    }

    @Test
    public void al_editar_no_cuenta_dos_veces_la_linea() {
        // Ya llevaba 1.356 contando el yogur de 120; ahora son dos, 240.
        EncajeDia e = EncajeDia.de(1356, 102, 2397, 136, 240, 40, 120, 20);
        assertEquals(1236, e.antesKcal);
        assertEquals(1476, e.despuesKcal);
        assertEquals(122, e.despuesProteina, D);
    }

    @Test
    public void sin_objetivo_no_hay_encaje() {
        assertNull(EncajeDia.de(1236, 82, 0, 0, 120, 20, 0, 0));
    }

    @Test
    public void reparto_de_las_kcal_entre_los_macros_suma_100() {
        // Yogur por 100 g: 10 g de proteína, 4,5 de carb. y 0,2 de grasa.
        assertArrayEquals(new int[]{67, 30, 3}, EncajeDia.reparto(10, 4.5, 0.2));
        int[] r = EncajeDia.reparto(1, 1, 1);
        assertEquals(100, r[0] + r[1] + r[2]);
    }

    @Test
    public void reparto_sin_macros_no_hay() {
        assertNull(EncajeDia.reparto(0, 0, 0));
    }
}
