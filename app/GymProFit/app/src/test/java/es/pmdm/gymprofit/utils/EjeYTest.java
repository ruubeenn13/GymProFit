package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

// EjeY (lote 12): el eje de valores de las gráficas de línea va en pasos redondos y empieza y
// acaba en múltiplos del paso. Antes MPAndroidChart elegía 0,6 o 0,8 y la línea de 14,5 % caía
// entre 14,4 y 15,0; con un solo punto, cada métrica salía con un eje distinto.
public class EjeYTest {

    private static final float D = 1e-4f;

    @Test
    public void el_paso_es_redondo_y_los_limites_multiplos_del_paso() {
        EjeY e = EjeY.de(78.5f, 79.2f);
        assertEquals(0.2f, e.paso, D);
        assertEquals(78.4f, e.inicio, D);
        assertEquals(79.2f, e.fin, D);
        assertTrue(e.etiquetas() >= 3 && e.etiquetas() <= 6);
    }

    @Test
    public void una_linea_plana_queda_en_medio_con_su_valor_en_una_etiqueta() {
        EjeY grasa = EjeY.de(14.5f, 14.5f);
        assertEquals(0.5f, grasa.paso, D);
        assertEquals(14.0f, grasa.inicio, D);
        assertEquals(15.0f, grasa.fin, D);
        EjeY cintura = EjeY.de(86f, 86f);
        assertEquals("mismo paso con un punto, sea cual sea la métrica", grasa.paso, cintura.paso, D);
        assertEquals(85.5f, cintura.inicio, D);
        assertEquals(86.5f, cintura.fin, D);
    }

    @Test
    public void con_mucho_rango_el_paso_crece() {
        EjeY e = EjeY.de(60f, 85f);
        assertEquals(10f, e.paso, D);
        assertEquals(60f, e.inicio, D);
        assertEquals(90f, e.fin, D);
        assertEquals(4, e.etiquetas());
    }

    @Test
    public void ningun_paso_es_de_0_6_ni_de_0_8() {
        for (float a = 10f; a < 120f; a += 3.7f) {
            for (float r = 0f; r < 9f; r += 0.35f) {
                EjeY e = EjeY.de(a, a + r);
                float p = e.paso;
                assertTrue("paso " + p, esRedondo(p));
                assertTrue(e.inicio <= a + D && e.fin >= a + r - D);
            }
        }
    }

    private static boolean esRedondo(float p) {
        for (float base : new float[]{0.1f, 0.2f, 0.5f, 1f, 2f, 5f, 10f, 20f, 50f, 100f}) {
            if (Math.abs(p - base) < D) return true;
        }
        return false;
    }
}
