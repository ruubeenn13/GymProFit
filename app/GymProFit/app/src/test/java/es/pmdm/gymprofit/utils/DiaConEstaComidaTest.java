package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import es.pmdm.gymprofit.model.comida.AlimentoComida;
import es.pmdm.gymprofit.model.comida.Comida;

/**
 * GP-225 (tercera pasada del revisor): al quitar las líneas de una comida, «Para tus N g de
 * proteína» seguía contando lo quitado. El borrado se manda al irse el aviso de «Deshacer» (5 s),
 * así que la API aún devolvía las líneas. El día se cuenta con esta comida tal como está en la
 * pantalla y las demás como las da la API.
 */
public class DiaConEstaComidaTest {

    private static final ResultadoNutricional OBJETIVO = new ResultadoNutricional(2000, 135, 200, 70, 2.5);

    private static Comida comida(String tipo, int kcal, double p) {
        Comida x = new Comida();
        x.setTipoComida(tipo);
        x.setTotalCalorias(kcal);
        x.setTotalProteinas(p);
        return x;
    }

    private static AlimentoComida linea(int kcal, double p) {
        AlimentoComida l = new AlimentoComida();
        l.setCaloriasTotales(kcal);
        l.setProteinasTotales(p);
        return l;
    }

    @Test
    public void loQuitadoEnLaPantallaYaNoCuenta() {
        // La API aún trae la merienda con 10 g; en la pantalla ya está vacía.
        DiaNutricion d = DiaNutricion.deConEsta(Arrays.asList(comida("DESAYUNO", 400, 20), comida("MERIENDA", 300, 10)),
                "MERIENDA", Collections.<AlimentoComida>emptyList(), OBJETIVO);
        assertEquals(400, d.kcal);
        assertEquals(20, d.proteinas, 0.001);
        assertEquals(115, d.objetivoProteinas - d.proteinas, 0.001);
    }

    @Test
    public void loDeLaPantallaManda() {
        DiaNutricion d = DiaNutricion.deConEsta(Collections.singletonList(comida("CENA", 300, 10)),
                "CENA", Arrays.asList(linea(120, 8), linea(80, 4)), OBJETIVO);
        assertEquals(200, d.kcal);
        assertEquals(12, d.proteinas, 0.001);
    }

    @Test
    public void sinEstaComidaEnLaApiSeSumanSusLineas() {
        DiaNutricion d = DiaNutricion.deConEsta(Collections.singletonList(comida("ALMUERZO", 600, 40)),
                "CENA", Collections.singletonList(linea(100, 5)), OBJETIVO);
        assertEquals(700, d.kcal);
        assertEquals(45, d.proteinas, 0.001);
    }
}
