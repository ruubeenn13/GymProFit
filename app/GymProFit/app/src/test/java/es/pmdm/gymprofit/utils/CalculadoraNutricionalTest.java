package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

// ============================================================
// CalculadoraNutricionalTest — las cifras de «Tu plan» y los menores sin déficit
// (GP-103, decisión 6 del lienzo del alta) y la regla de proteína (GP-206, DEC-054).
// ============================================================
public class CalculadoraNutricionalTest {

    @Test
    public void lasCifrasDeLuciaSonLasDelLienzo() {
        // Lucía: 29 años, 165 cm, 62 kg, mujer, moderada, ganar músculo.
        ResultadoNutricional r = CalculadoraNutricional.calcular(62, 165, 29, false,
                CalculadoraNutricional.ACTIVIDAD_MODERADO, CalculadoraNutricional.OBJETIVO_GANAR_MASA_MUSCULAR);
        assertEquals(2397, r.calorias);
        assertEquals(111, r.proteinas);
        assertEquals(339, r.carbohidratos);
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

    // ── Proteína (GP-206) ────────────────────────────────────────────────────

    private static ResultadoNutricional sinGrasa(double peso, double alturaCm, String objetivo) {
        return CalculadoraNutricional.calcular(peso, alturaCm, 30, true,
                CalculadoraNutricional.ACTIVIDAD_MODERADO, objetivo);
    }

    private static ResultadoNutricional conGrasa(double peso, double alturaCm, String objetivo,
                                                 Double pct, int dias) {
        return CalculadoraNutricional.calcular(peso, alturaCm, 30, true,
                CalculadoraNutricional.ACTIVIDAD_MODERADO, objetivo, pct, dias);
    }

    @Test
    public void casoFijoDelVideo_78_6kg_perderGrasa_sinPorcentaje() {
        ResultadoNutricional r = sinGrasa(78.6, 178, CalculadoraNutricional.OBJETIVO_PERDER_PESO);
        assertEquals(157, r.proteinas);
        assertEquals(126, r.proteinaMinima);
        assertTrue(r.tieneRangoProteina());
        assertEquals(ResultadoNutricional.BASE_PESO, r.baseProteina);
    }

    @Test
    public void casoFijoObesidad_120kg_perderGrasa_usaPesoDeReferenciaImc25() {
        ResultadoNutricional r = sinGrasa(120, 170, CalculadoraNutricional.OBJETIVO_PERDER_PESO);
        assertEquals(144, r.proteinas);   // 25 × 1,70² = 72,25 kg × 2,0 (antes daba 240)
        assertEquals(116, r.proteinaMinima);
        assertEquals(ResultadoNutricional.BASE_PESO_REFERENCIA, r.baseProteina);
    }

    @Test
    public void sinPorcentajeCadaObjetivoTieneSuGramosPorKilo() {
        assertEquals(144, sinGrasa(80, 180, CalculadoraNutricional.OBJETIVO_GANAR_MASA_MUSCULAR).proteinas);
        assertEquals(144, sinGrasa(80, 180, CalculadoraNutricional.OBJETIVO_MEJORAR_FUERZA).proteinas);
        assertEquals(128, sinGrasa(80, 180, CalculadoraNutricional.OBJETIVO_MANTENER_PESO).proteinas);
        assertEquals(160, sinGrasa(80, 180, CalculadoraNutricional.OBJETIVO_PERDER_PESO).proteinas);
    }

    @Test
    public void sinPorcentajeElMinimoEsUnoSesentaPorKiloDelPesoDeReferencia() {
        assertEquals(128, sinGrasa(80, 180, CalculadoraNutricional.OBJETIVO_GANAR_MASA_MUSCULAR).proteinaMinima);
        assertEquals(128, sinGrasa(80, 180, CalculadoraNutricional.OBJETIVO_PERDER_PESO).proteinaMinima);
    }

    @Test
    public void siElMinimoCoincideConLaRecomendadaNoHayRangoQueEnsenar() {
        ResultadoNutricional r = sinGrasa(80, 180, CalculadoraNutricional.OBJETIVO_MANTENER_PESO);
        assertEquals(r.proteinas, r.proteinaMinima);
        assertFalse(r.tieneRangoProteina());
    }

    @Test
    public void imcTreintaOMasUsaElPesoDeReferenciaYPorDebajoElTotal() {
        // 1,80 m: IMC 30 son 97,2 kg. 95 kg → 29,3 (total); 100 kg → 30,9 (referencia 81 kg).
        ResultadoNutricional total = sinGrasa(95, 180, CalculadoraNutricional.OBJETIVO_PERDER_PESO);
        assertEquals(190, total.proteinas);
        assertEquals(ResultadoNutricional.BASE_PESO, total.baseProteina);
        ResultadoNutricional referencia = sinGrasa(100, 180, CalculadoraNutricional.OBJETIVO_PERDER_PESO);
        assertEquals(162, referencia.proteinas);
        assertEquals(ResultadoNutricional.BASE_PESO_REFERENCIA, referencia.baseProteina);
    }

    @Test
    public void conPorcentajePerderGrasaDaDosCuatroPorKiloDeMasaMagra() {
        ResultadoNutricional r = conGrasa(80, 180, CalculadoraNutricional.OBJETIVO_PERDER_PESO, 20.0, 5);
        assertEquals(153, r.proteinas);   // MM 64 kg × 2,4
        assertEquals(128, r.proteinaMinima);
        assertEquals(ResultadoNutricional.BASE_MASA_MAGRA, r.baseProteina);
    }

    @Test
    public void conPorcentajeElRestoDeObjetivosDaDosPorKiloDeMasaMagra() {
        for (String objetivo : new String[]{CalculadoraNutricional.OBJETIVO_GANAR_MASA_MUSCULAR,
                CalculadoraNutricional.OBJETIVO_MEJORAR_FUERZA, CalculadoraNutricional.OBJETIVO_MANTENER_PESO}) {
            ResultadoNutricional r = conGrasa(80, 180, objetivo, 15.0, 5);
            assertEquals(objetivo, 136, r.proteinas);   // MM 68 kg × 2,0
            assertEquals(ResultadoNutricional.BASE_MASA_MAGRA, r.baseProteina);
        }
    }

    @Test
    public void conMuchaGrasaElMinimoNuncaPasaDeLaRecomendada() {
        // 40 % de grasa: MM 48 kg × 2,0 = 96 g, por debajo de 1,6 × 80 = 128 g.
        ResultadoNutricional r = conGrasa(80, 180, CalculadoraNutricional.OBJETIVO_MANTENER_PESO, 40.0, 5);
        assertEquals(96, r.proteinas);
        assertEquals(96, r.proteinaMinima);
        assertFalse(r.tieneRangoProteina());
    }

    @Test
    public void conPorcentajeYObesidadElMinimoSigueSiendoDelPesoDeReferencia() {
        ResultadoNutricional r = conGrasa(120, 170, CalculadoraNutricional.OBJETIVO_PERDER_PESO, 30.0, 5);
        assertEquals(201, r.proteinas);   // MM 84 kg × 2,4
        assertEquals(116, r.proteinaMinima);
        assertEquals(ResultadoNutricional.BASE_MASA_MAGRA, r.baseProteina);
    }

    @Test
    public void unPorcentajeFueraDeRangoSeIgnora() {
        ResultadoNutricional sin = sinGrasa(80, 180, CalculadoraNutricional.OBJETIVO_PERDER_PESO);
        for (double pct : new double[]{0, 2.9, 60.1, 75}) {
            ResultadoNutricional r = conGrasa(80, 180, CalculadoraNutricional.OBJETIVO_PERDER_PESO, pct, 5);
            assertEquals("pct " + pct, sin.proteinas, r.proteinas);
            assertEquals("pct " + pct, ResultadoNutricional.BASE_PESO, r.baseProteina);
        }
    }

    @Test
    public void losLimitesDelRangoSiValen() {
        assertEquals(ResultadoNutricional.BASE_MASA_MAGRA,
                conGrasa(80, 180, CalculadoraNutricional.OBJETIVO_PERDER_PESO, 3.0, 5).baseProteina);
        assertEquals(ResultadoNutricional.BASE_MASA_MAGRA,
                conGrasa(80, 180, CalculadoraNutricional.OBJETIVO_PERDER_PESO, 60.0, 5).baseProteina);
    }

    @Test
    public void unPorcentajeDeHaceMasDeNoventaDiasSeIgnora() {
        assertEquals(ResultadoNutricional.BASE_MASA_MAGRA,
                conGrasa(80, 180, CalculadoraNutricional.OBJETIVO_PERDER_PESO, 20.0, 90).baseProteina);
        ResultadoNutricional viejo = conGrasa(80, 180, CalculadoraNutricional.OBJETIVO_PERDER_PESO, 20.0, 91);
        assertEquals(ResultadoNutricional.BASE_PESO, viejo.baseProteina);
        assertEquals(160, viejo.proteinas);
    }

    @Test
    public void unPorcentajeNuloEsComoNoTenerlo() {
        ResultadoNutricional nulo = conGrasa(80, 180, CalculadoraNutricional.OBJETIVO_PERDER_PESO, null, 0);
        ResultadoNutricional sin = sinGrasa(80, 180, CalculadoraNutricional.OBJETIVO_PERDER_PESO);
        assertEquals(sin.proteinas, nulo.proteinas);
        assertEquals(sin.proteinaMinima, nulo.proteinaMinima);
        assertEquals(ResultadoNutricional.BASE_PESO, nulo.baseProteina);
    }

    @Test
    public void unaFechaFuturaNoSeUsa() {
        assertEquals(ResultadoNutricional.BASE_PESO,
                conGrasa(80, 180, CalculadoraNutricional.OBJETIVO_PERDER_PESO, 20.0, -1).baseProteina);
    }

    @Test
    public void elPorcentajeNoMueveCaloriasGrasasNiAguaYLosCarbosSalenDelResto() {
        ResultadoNutricional sin = sinGrasa(80, 180, CalculadoraNutricional.OBJETIVO_PERDER_PESO);
        ResultadoNutricional con = conGrasa(80, 180, CalculadoraNutricional.OBJETIVO_PERDER_PESO, 20.0, 5);
        assertEquals(sin.calorias, con.calorias);
        assertEquals(sin.grasas, con.grasas);
        assertEquals(sin.agua, con.agua, 0.0001);
        assertEquals((con.calorias - con.proteinas * 4 - con.grasas * 9) / 4, con.carbohidratos);
    }

    @Test
    public void elMenorQuePierdeGrasaConservaSuReglaDeMantenimientoTambienEnProteina() {
        ResultadoNutricional perder = CalculadoraNutricional.calcular(60, 170, 16, true,
                CalculadoraNutricional.ACTIVIDAD_LIGERO, CalculadoraNutricional.OBJETIVO_PERDER_PESO, 20.0, 5);
        ResultadoNutricional mantener = CalculadoraNutricional.calcular(60, 170, 16, true,
                CalculadoraNutricional.ACTIVIDAD_LIGERO, CalculadoraNutricional.OBJETIVO_MANTENER_PESO, 20.0, 5);
        assertEquals(mantener.proteinas, perder.proteinas);
        assertEquals(96, perder.proteinas);   // MM 48 kg × 2,0, no × 2,4
    }
}
