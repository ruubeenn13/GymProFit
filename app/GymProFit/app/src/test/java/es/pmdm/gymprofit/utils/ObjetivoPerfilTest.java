package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import es.pmdm.gymprofit.R;

// ============================================================
// ObjetivoPerfilTest — la frase del objetivo en el Perfil (GP-242, punto 7): una por objetivo
// de CalculadoraNutricional, en español y en inglés, y la nota de menores cuando «Perder
// grasa» se queda en mantenimiento por la edad.
// ============================================================
public class ObjetivoPerfilTest {

    @Test
    public void perder_grasa() {
        assertEquals(R.string.perfil_objetivo_frase_perder,
                ObjetivoPerfil.frase(CalculadoraNutricional.OBJETIVO_PERDER_PESO, false));
        assertEquals("Comes un poco menos de lo que gastas y mucha proteína, para que lo que bajes sea grasa y no músculo.",
                Cadenas.es().s("perfil_objetivo_frase_perder"));
        assertEquals("You eat a little less than you burn, with plenty of protein, so what you lose is fat, not muscle.",
                Cadenas.en().s("perfil_objetivo_frase_perder"));
        assertEquals(R.drawable.ic_ms_local_fire_department, ObjetivoPerfil.icono(CalculadoraNutricional.OBJETIVO_PERDER_PESO));
    }

    @Test
    public void ganar_musculo() {
        assertEquals(R.string.perfil_objetivo_frase_ganar,
                ObjetivoPerfil.frase(CalculadoraNutricional.OBJETIVO_GANAR_MASA_MUSCULAR, false));
        assertEquals("Comes algo más de lo que gastas y mucha proteína, para que lo que subas sea sobre todo músculo.",
                Cadenas.es().s("perfil_objetivo_frase_ganar"));
        assertEquals("You eat a little more than you burn, with plenty of protein, so what you gain is mostly muscle.",
                Cadenas.en().s("perfil_objetivo_frase_ganar"));
        assertEquals(R.drawable.ic_ms_fitness_center, ObjetivoPerfil.icono(CalculadoraNutricional.OBJETIVO_GANAR_MASA_MUSCULAR));
    }

    @Test
    public void ganar_fuerza() {
        assertEquals(R.string.perfil_objetivo_frase_fuerza,
                ObjetivoPerfil.frase(CalculadoraNutricional.OBJETIVO_MEJORAR_FUERZA, false));
        assertEquals("Comes algo más de lo que gastas, con la proteína y la energía para entrenar pesado.",
                Cadenas.es().s("perfil_objetivo_frase_fuerza"));
        assertEquals("You eat a little more than you burn, with the protein and energy to lift heavy.",
                Cadenas.en().s("perfil_objetivo_frase_fuerza"));
        assertEquals(R.drawable.ic_ms_bolt, ObjetivoPerfil.icono(CalculadoraNutricional.OBJETIVO_MEJORAR_FUERZA));
    }

    @Test
    public void mantenerme() {
        assertEquals(R.string.perfil_objetivo_frase_mantener,
                ObjetivoPerfil.frase(CalculadoraNutricional.OBJETIVO_MANTENER_PESO, false));
        assertEquals("Comes lo que gastas, con la proteína que necesitas para conservar tu músculo.",
                Cadenas.es().s("perfil_objetivo_frase_mantener"));
        assertEquals("You eat what you burn, with the protein you need to keep your muscle.",
                Cadenas.en().s("perfil_objetivo_frase_mantener"));
        assertEquals(R.drawable.ic_ms_balance, ObjetivoPerfil.icono(CalculadoraNutricional.OBJETIVO_MANTENER_PESO));
    }

    @Test
    public void con_17_anos_perder_grasa_lleva_la_nota_de_menores() {
        // Con la calculadora de verdad: a los 17 «Perder grasa» se queda en mantenimiento.
        ResultadoNutricional plan = CalculadoraNutricional.calcular(70, 175, 17, true,
                CalculadoraNutricional.ACTIVIDAD_MODERADO, CalculadoraNutricional.OBJETIVO_PERDER_PESO);
        assertEquals(R.string.alta_plan_nota_menores,
                ObjetivoPerfil.frase(CalculadoraNutricional.OBJETIVO_PERDER_PESO, plan.mantenimientoPorEdad));
        ResultadoNutricional adulto = CalculadoraNutricional.calcular(70, 175, 18, true,
                CalculadoraNutricional.ACTIVIDAD_MODERADO, CalculadoraNutricional.OBJETIVO_PERDER_PESO);
        assertEquals(R.string.perfil_objetivo_frase_perder,
                ObjetivoPerfil.frase(CalculadoraNutricional.OBJETIVO_PERDER_PESO, adulto.mantenimientoPorEdad));
        // Solo afecta a perder: los demás objetivos no tienen nota.
        assertEquals(R.string.perfil_objetivo_frase_ganar,
                ObjetivoPerfil.frase(CalculadoraNutricional.OBJETIVO_GANAR_MASA_MUSCULAR, true));
    }

    @Test
    public void sin_objetivo_no_hay_frase() {
        assertEquals(0, ObjetivoPerfil.frase(null, false));
        assertEquals(0, ObjetivoPerfil.frase("OTRO", false));
    }

    @Test
    public void las_columnas_de_los_gramos_no_bajan_del_30_por_ciento() {
        // El caso del revisor: 128/446/85 g reparten 17, 61 y 26 %; antes «de proteína» se partía.
        int[] reparto = ObjetivoPerfil.reparto(128, 446, 85);
        int[] c = ObjetivoPerfil.columnas(reparto, 30);
        assertEquals(100, c[0] + c[1] + c[2]);
        for (int x : c) assertTrue("cada columna, al menos el 30 %: " + x, x >= 30);
        assertTrue("la ancha sigue siendo la ancha", c[1] > c[0] && c[1] > c[2]);
        // Si ya pasan del mínimo, se quedan como el reparto.
        assertArrayEquals(new int[]{30, 39, 31}, ObjetivoPerfil.columnas(new int[]{30, 39, 31}, 30));
        assertArrayEquals(new int[]{34, 33, 33}, ObjetivoPerfil.columnas(new int[]{0, 0, 0}, 30));
    }

    @Test
    public void el_reparto_de_la_barra_suma_100_con_las_kcal_de_cada_macro() {
        // El tablero: 160 g de proteína, 210 de hidratos y 74 de grasa son 30, 39 y 31 %.
        assertArrayEquals(new int[]{30, 39, 31}, ObjetivoPerfil.reparto(160, 210, 74));
        assertArrayEquals(new int[]{0, 0, 0}, ObjetivoPerfil.reparto(0, 0, 0));
        int[] r = ObjetivoPerfil.reparto(1, 1, 1);
        assertEquals(100, r[0] + r[1] + r[2]);
    }
}
