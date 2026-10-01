package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

// ============================================================
// CalculadoraNutricionalTest — las cifras de «Tu plan» y los menores sin déficit
// (GP-103, decisión 6 del lienzo del alta).
// ============================================================
public class CalculadoraNutricionalTest {

    @Test
    public void lasCifrasDeLuciaSonLasDelLienzo() {
        // Lucía: 29 años, 165 cm, 62 kg, mujer, moderada, ganar músculo.
        ResultadoNutricional r = CalculadoraNutricional.calcular(62, 165, 29, false,
                CalculadoraNutricional.ACTIVIDAD_MODERADO, CalculadoraNutricional.OBJETIVO_GANAR_MASA_MUSCULAR);
        assertEquals(2397, r.calorias);
        assertEquals(136, r.proteinas);
        assertEquals(314, r.carbohidratos);
        assertEquals(66, r.grasas);
        assertEquals(2.2, r.agua, 0.001);
        assertFalse(r.mantenimientoPorEdad);
    }

    @Test
    public void deCatorceADiecisieteAniosPerderGrasaDaMantenimientoYLoDice() {
        for (int edad = 14; edad <= 17; edad++) {
            ResultadoNutricional perder = CalculadoraNutricional.calcular(60, 170, edad, true,
                    CalculadoraNutricional.ACTIVIDAD_LIGERO, CalculadoraNutricional.OBJETIVO_PERDER_PESO);
            ResultadoNutricional mantener = CalculadoraNutricional.calcular(60, 170, edad, true,
                    CalculadoraNutricional.ACTIVIDAD_LIGERO, CalculadoraNutricional.OBJETIVO_MANTENER_PESO);
            assertEquals("edad " + edad, mantener.calorias, perder.calorias);
            assertEquals(mantener.proteinas, perder.proteinas);
            assertTrue("edad " + edad, perder.mantenimientoPorEdad);
            assertFalse(mantener.mantenimientoPorEdad);
        }
    }

    @Test
    public void conDieciochoYaHayDeficit() {
        ResultadoNutricional perder = CalculadoraNutricional.calcular(60, 170, 18, true,
                CalculadoraNutricional.ACTIVIDAD_LIGERO, CalculadoraNutricional.OBJETIVO_PERDER_PESO);
        ResultadoNutricional mantener = CalculadoraNutricional.calcular(60, 170, 18, true,
                CalculadoraNutricional.ACTIVIDAD_LIGERO, CalculadoraNutricional.OBJETIVO_MANTENER_PESO);
        assertTrue(perder.calorias < mantener.calorias);
        assertFalse(perder.mantenimientoPorEdad);
    }

    @Test
    public void unMenorQueQuiereGanarMusculoNoSeToca() {
        ResultadoNutricional r = CalculadoraNutricional.calcular(60, 170, 16, true,
                CalculadoraNutricional.ACTIVIDAD_LIGERO, CalculadoraNutricional.OBJETIVO_GANAR_MASA_MUSCULAR);
        assertFalse(r.mantenimientoPorEdad);
    }
}
