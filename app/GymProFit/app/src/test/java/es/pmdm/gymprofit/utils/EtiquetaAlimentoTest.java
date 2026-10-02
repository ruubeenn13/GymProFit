package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Map;

// ============================================================
// EtiquetaAlimentoTest — crear un alimento como en la etiqueta (decisión 17, tablero 13,
// GP-175, lote 1.6.2)
// La API guarda por 100 g; las cifras se pueden escribir por 100 g o por la ración, y la
// app las pasa a 100 g. Solo la energía es obligatoria y 0 vale. Más de 900 kcal en 100 g
// no cabe (la grasa pura son 900): casi seguro son kJ. Cada macro, de 0 a 100 g por
// 100 g, y la suma con la fibra hasta 102 g (las etiquetas redondean). El aviso de las
// calorías compara la energía con 4·P + 4·C + 9·G y no impide guardar.
// ============================================================
public class EtiquetaAlimentoTest {

    private static EtiquetaAlimento.Cifras cifras(Double e, Double g, Double c, Double f, Double p) {
        return new EtiquetaAlimento.Cifras(e, g, c, f, p);
    }

    @Test
    public void por_100_g_se_guarda_tal_cual() {
        EtiquetaAlimento.Resultado r = EtiquetaAlimento.comprobar(cifras(450.0, 18.0, 64.0, 3.0, 7.0), 100);
        assertTrue(r.errores.isEmpty());
        assertEquals(450, r.calorias);
        assertEquals(18.0, r.grasas, 0.001);
        assertEquals(64.0, r.carbohidratos, 0.001);
        assertEquals(3.0, r.fibra, 0.001);
        assertEquals(7.0, r.proteinas, 0.001);
    }

    @Test
    public void por_la_racion_se_pasa_a_100_g() {
        // Una galleta de 12 g: 54 kcal, 2,2 g de grasa, 7,7 de hidratos, 0,8 de proteína.
        EtiquetaAlimento.Resultado r = EtiquetaAlimento.comprobar(cifras(54.0, 2.2, 7.7, null, 0.8), 12);
        assertTrue(r.errores.isEmpty());
        assertEquals(450, r.calorias);
        assertEquals(18.33, r.grasas, 0.001);
        assertEquals(64.17, r.carbohidratos, 0.001);
        assertNull(r.fibra);
        assertEquals(6.67, r.proteinas, 0.001);
    }

    @Test
    public void y_de_100_g_a_la_racion_para_cambiar_el_conmutador() {
        assertEquals(54.0, EtiquetaAlimento.convertir(450, 100, 12), 0.001);
        assertEquals(450.0, EtiquetaAlimento.convertir(54, 12, 100), 0.001);
    }

    @Test
    public void solo_la_energia_es_obligatoria_y_cero_vale() {
        EtiquetaAlimento.Resultado sin = EtiquetaAlimento.comprobar(cifras(null, null, null, null, null), 100);
        assertEquals(EtiquetaAlimento.Error.FALTA, sin.errores.get(EtiquetaAlimento.Campo.ENERGIA));
        EtiquetaAlimento.Resultado agua = EtiquetaAlimento.comprobar(cifras(0.0, null, null, null, null), 100);
        assertTrue(agua.errores.isEmpty());
        assertEquals(0, agua.calorias);
        // Un macro sin escribir es 0; la fibra sin escribir no se sabe.
        assertEquals(0.0, agua.proteinas, 0.001);
        assertNull(agua.fibra);
    }

    @Test
    public void mas_de_900_kcal_en_100_g_son_kj() {
        Map<EtiquetaAlimento.Campo, EtiquetaAlimento.Error> e =
                EtiquetaAlimento.comprobar(cifras(1880.0, 18.0, 64.0, 3.0, 7.0), 100).errores;
        assertEquals(EtiquetaAlimento.Error.KJ, e.get(EtiquetaAlimento.Campo.ENERGIA));
        // 900 justas (aceite) sí.
        assertTrue(EtiquetaAlimento.comprobar(cifras(900.0, 100.0, 0.0, 0.0, 0.0), 100).errores.isEmpty());
        // Y se mira por 100 g: 120 kcal en una ración de 10 g son 1200 en 100 g.
        assertEquals(EtiquetaAlimento.Error.KJ,
                EtiquetaAlimento.comprobar(cifras(120.0, null, null, null, null), 10).errores.get(EtiquetaAlimento.Campo.ENERGIA));
    }

    @Test
    public void cada_macro_de_0_a_100_por_100_g() {
        Map<EtiquetaAlimento.Campo, EtiquetaAlimento.Error> e =
                EtiquetaAlimento.comprobar(cifras(400.0, 101.0, 0.0, 0.0, -1.0), 100).errores;
        assertEquals(EtiquetaAlimento.Error.FUERA, e.get(EtiquetaAlimento.Campo.GRASAS));
        assertEquals(EtiquetaAlimento.Error.FUERA, e.get(EtiquetaAlimento.Campo.PROTEINAS));
        assertFalse(e.containsKey(EtiquetaAlimento.Campo.HIDRATOS));
    }

    @Test
    public void la_suma_con_la_fibra_hasta_102_g() {
        // 30 + 40 + 20 + 11 = 101: cabe por el redondeo de las etiquetas.
        assertTrue(EtiquetaAlimento.comprobar(cifras(400.0, 30.0, 40.0, 11.0, 20.0), 100).errores.isEmpty());
        // 30 + 50 + 20 + 3 = 103: no.
        Map<EtiquetaAlimento.Campo, EtiquetaAlimento.Error> e =
                EtiquetaAlimento.comprobar(cifras(400.0, 30.0, 50.0, 3.0, 20.0), 100).errores;
        assertEquals(EtiquetaAlimento.Error.SUMA, e.get(EtiquetaAlimento.Campo.HIDRATOS));
    }

    @Test
    public void el_aviso_de_las_calorias() {
        // Solo con los tres macros escritos.
        assertNull(EtiquetaAlimento.kcalSegunMacros(cifras(450.0, 18.0, null, null, 7.0)));
        Integer k = EtiquetaAlimento.kcalSegunMacros(cifras(450.0, 18.0, 64.0, 3.0, 7.0));
        assertEquals(Integer.valueOf(446), k);
        assertTrue(EtiquetaAlimento.cuadra(450, 446));
        // 430 frente a 600: no cuadra; y se dice con lo que saldría, redondeado a 10.
        assertFalse(EtiquetaAlimento.cuadra(600, 430));
        assertEquals(430, EtiquetaAlimento.redondeoAviso(433));
        // Con pocas calorías, el margen mínimo de 20 kcal.
        assertTrue(EtiquetaAlimento.cuadra(30, 15));
        assertFalse(EtiquetaAlimento.cuadra(40, 15));
    }

    // GP-178: se compara siempre por 100 g. Por una ración de 12 g, 60 kcal frente a las 44
    // de los macros se separan 16 y el margen mínimo de 20 lo dejaba pasar; por 100 g son
    // 500 frente a 367, y no cuadra.
    @Test
    public void las_calorias_se_comparan_por_100_g() {
        EtiquetaAlimento.Cifras porRacion = cifras(60.0, 1.0, 6.0, null, 2.75);
        assertEquals(Boolean.FALSE, EtiquetaAlimento.cuadra(porRacion, 12));
        // Lo mismo escrito por 100 g da lo mismo.
        assertEquals(Boolean.FALSE, EtiquetaAlimento.cuadra(cifras(500.0, 8.33, 50.0, null, 22.92), 100));
        // Y lo que cuadra por 100 g cuadra por la ración: 50 frente a 48 en 12 g.
        assertEquals(Boolean.TRUE, EtiquetaAlimento.cuadra(cifras(50.0, 1.0, 6.0, null, 3.75), 12));
        // Sin los tres macros no se sabe.
        assertNull(EtiquetaAlimento.cuadra(cifras(60.0, 1.0, null, null, 2.0), 12));
    }

    // GP-181: «Créalo» lleva lo escrito al nombre del alimento nuevo.
    @Test
    public void el_nombre_sale_de_lo_que_se_buscaba() {
        assertEquals("Pan de espelta", EtiquetaAlimento.nombreDesdeBusqueda("  pan   de espelta "));
        assertEquals("Kéfir", EtiquetaAlimento.nombreDesdeBusqueda("kéfir"));
        assertNull(EtiquetaAlimento.nombreDesdeBusqueda("   "));
        assertNull(EtiquetaAlimento.nombreDesdeBusqueda(null));
    }
}
