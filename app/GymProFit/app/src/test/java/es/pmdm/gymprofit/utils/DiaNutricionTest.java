package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;

import es.pmdm.gymprofit.model.comida.Comida;

// ============================================================
// DiaNutricionTest — GP-105: Inicio y Nutrición calculan el día en un solo sitio,
// con las reglas de color de siempre (la proteína es un suelo; carbos y grasas, un
// techo).
// ============================================================
public class DiaNutricionTest {

    private static final ResultadoNutricional OBJETIVO = new ResultadoNutricional(2000, 150, 200, 70, 2.5);

    private static Comida comida(int kcal, double p, double c, double g) {
        Comida x = new Comida();
        x.setTotalCalorias(kcal);
        x.setTotalProteinas(p);
        x.setTotalCarbohidratos(c);
        x.setTotalGrasas(g);
        return x;
    }

    @Test
    public void suma_las_comidas_del_dia() {
        DiaNutricion d = DiaNutricion.de(Arrays.asList(comida(500, 30, 60, 10), comida(740, 56, 60, 31)), OBJETIVO);
        assertEquals(1240, d.kcal);
        assertEquals(86, d.proteinas, 0.001);
        assertEquals(760, d.kcalRestantes());
        assertEquals(62, DiaNutricion.porcentaje(d.kcal, d.objetivoKcal));
    }

    @Test
    public void sin_comidas_todo_a_cero() {
        DiaNutricion d = DiaNutricion.de(null, OBJETIVO);
        assertEquals(0, d.kcal);
        assertEquals(2000, d.kcalRestantes());
        assertEquals(DiaNutricion.Estado.NORMAL, d.estadoProteinas());
    }

    @Test
    public void proteina_es_suelo_y_carbos_grasas_techo() {
        DiaNutricion d = DiaNutricion.de(Arrays.asList(comida(2300, 150, 201, 70)), OBJETIVO);
        assertEquals(DiaNutricion.Estado.LOGRADO, d.estadoProteinas());
        assertEquals(DiaNutricion.Estado.PASADO, d.estadoCarbohidratos());
        assertEquals(DiaNutricion.Estado.NORMAL, d.estadoGrasas());
        assertEquals(-300, d.kcalRestantes());
        assertEquals(100, DiaNutricion.porcentaje(d.kcal, d.objetivoKcal));
    }

    @Test
    public void sin_objetivo_cuenta_lo_comido_y_nada_se_pasa() {
        // «Prefiero no decirlo» (GP-103): sin peso ni altura no hay objetivo que inventar.
        DiaNutricion d = DiaNutricion.de(Arrays.asList(comida(2300, 150, 201, 70)), null);
        assertTrue(d.sinObjetivo);
        assertEquals(2300, d.kcal);
        assertEquals(0, d.objetivoKcal);
        assertEquals(0, DiaNutricion.porcentaje(d.kcal, d.objetivoKcal));
        assertEquals(DiaNutricion.Estado.NORMAL, d.estadoProteinas());
        assertEquals(DiaNutricion.Estado.NORMAL, d.estadoCarbohidratos());
        assertEquals(DiaNutricion.Estado.NORMAL, d.estadoGrasas());
    }
}
