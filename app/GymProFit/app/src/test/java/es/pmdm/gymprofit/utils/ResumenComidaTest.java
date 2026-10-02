package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import es.pmdm.gymprofit.model.comida.AlimentoComida;

// ============================================================
// ResumenComidaTest — el resumen de una comida (decisión 16, tablero 2, lote 1.6.2)
// Con los datos del lienzo: pollo, arroz, ensalada y aceite son 535 kcal, 41 g de
// proteína, 59 de carbohidratos y 13 de grasa; con el objetivo de 2397 kcal (136, 314 y
// 66 g), la comida es el 22 % del día. El reparto es el de la ficha (4, 4 y 9 kcal por
// gramo, enteros que suman 100). Sin objetivo, no hay porcentajes del día.
// ============================================================
public class ResumenComidaTest {

    private static AlimentoComida linea(int kcal, double p, double c, double g) {
        AlimentoComida a = new AlimentoComida();
        a.setCaloriasTotales(kcal);
        a.setProteinasTotales(p);
        a.setCarbohidratosTotales(c);
        a.setGrasasTotales(g);
        return a;
    }

    private static List<AlimentoComida> lienzo() {
        return new ArrayList<>(Arrays.asList(linea(165, 35, 0, 2), linea(260, 5, 56, 1), linea(20, 1, 3, 0),
                linea(90, 0, 0, 10)));
    }

    private static final ResultadoNutricional OBJETIVO = new ResultadoNutricional(2397, 136, 314, 66, 2.5);

    @Test
    public void suma_las_lineas() {
        ResumenComida r = ResumenComida.de(lienzo(), OBJETIVO);
        assertEquals(535, r.kcal);
        assertEquals(41, r.proteinas, 0.001);
        assertEquals(59, r.carbohidratos, 0.001);
        assertEquals(13, r.grasas, 0.001);
    }

    @Test
    public void el_reparto_es_el_de_la_ficha() {
        ResumenComida r = ResumenComida.de(lienzo(), OBJETIVO);
        assertArrayEquals(EncajeDia.reparto(41, 59, 13), r.reparto);
        int suma = r.reparto[0] + r.reparto[1] + r.reparto[2];
        assertEquals(100, suma);
    }

    @Test
    public void que_parte_del_dia() {
        ResumenComida r = ResumenComida.de(lienzo(), OBJETIVO);
        assertFalse(r.sinObjetivo);
        assertEquals(22, r.pctDia());
        assertEquals(30, r.pctMacro(0));   // 41 de 136
        assertEquals(19, r.pctMacro(1));   // 59 de 314
        assertEquals(20, r.pctMacro(2));   // 13 de 66
        assertEquals(535f / 2397f, r.barraDia(), 0.0001f);
    }

    @Test
    public void la_barra_del_dia_no_pasa_de_llena() {
        List<AlimentoComida> mucho = Arrays.asList(linea(3000, 10, 10, 10));
        assertEquals(1f, ResumenComida.de(mucho, OBJETIVO).barraDia(), 0.0001f);
        assertEquals(125, ResumenComida.de(mucho, OBJETIVO).pctDia());
    }

    @Test
    public void sin_objetivo_solo_el_anillo_y_los_gramos() {
        ResumenComida r = ResumenComida.de(lienzo(), null);
        assertTrue(r.sinObjetivo);
        assertEquals(535, r.kcal);
        assertEquals(-1, r.pctDia());
        assertEquals(-1, r.pctMacro(0));
    }

    @Test
    public void vacia_o_sin_macros() {
        ResumenComida r = ResumenComida.de(new ArrayList<>(), OBJETIVO);
        assertTrue(r.vacia);
        assertEquals(0, r.kcal);
        assertNull(r.reparto);
        // Un alimento con kcal y sin macros: no hay reparto que dibujar, pero no está vacía.
        ResumenComida s = ResumenComida.de(Arrays.asList(linea(50, 0, 0, 0)), OBJETIVO);
        assertFalse(s.vacia);
        assertNull(s.reparto);
    }
}
