package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import es.pmdm.gymprofit.utils.AltaPasos.Paso;
import es.pmdm.gymprofit.utils.AltaPasos.Respuestas;

// ============================================================
// AltaPasosTest — el orden del alta, su barra y cuándo se puede seguir (GP-103).
// ============================================================
public class AltaPasosTest {

    private static Respuestas vacias() {
        return new Respuestas("", "", "", 0, 0, "", "", false, "", 0, 0);
    }

    private static Respuestas completas() {
        return new Respuestas("GANAR_MASA_MUSCULAR", "INTERMEDIO", "MUJER", 29, 165, "62",
                "MODERADO", false, "GIMNASIO", 3, 45);
    }

    @Test
    public void cincoPreguntasEnCuatroCapitulosConElProgresoDelLienzo() {
        // aria-valuenow de cada tablero: 12, 25, 50, 62, 75 y 100.
        int[] esperado = {12, 25, 50, 62, 75, 100};
        for (Paso p : Paso.values()) assertEquals(p.name(), esperado[p.ordinal()], p.porcentaje());
        assertEquals(1, Paso.NIVEL.capitulo);
        assertEquals(4, Paso.PLAN.capitulo);
    }

    @Test
    public void soloAbrenCapituloObjetivoSobreTiYDonde() {
        for (Paso p : Paso.values()) {
            boolean abre = p == Paso.OBJETIVO || p == Paso.SOBRE_TI || p == Paso.DONDE;
            assertEquals(p.name(), abre, p.abreCapitulo);
        }
    }

    @Test
    public void elOrdenVaYVuelve() {
        assertEquals(Paso.NIVEL, Paso.OBJETIVO.siguiente());
        assertNull(Paso.PLAN.siguiente());
        assertNull(Paso.OBJETIVO.anterior());
        assertEquals(Paso.DONDE, Paso.DIAS.anterior());
        assertEquals(Paso.OBJETIVO, Paso.de(99));
    }

    @Test
    public void siguienteApagadoHastaResponder() {
        Respuestas nada = vacias();
        for (Paso p : Paso.values()) {
            if (p != Paso.PLAN) assertFalse(p.name(), AltaPasos.puedeSeguir(p, nada));
        }
        Respuestas todo = completas();
        for (Paso p : Paso.values()) assertTrue(p.name(), AltaPasos.puedeSeguir(p, todo));
    }

    @Test
    public void sobreTiPideLasCincoYUnaEdadDeCatorceACien() {
        Respuestas sinActividad = new Respuestas("", "", "MUJER", 29, 165, "62", "", false, "", 0, 0);
        assertFalse(AltaPasos.puedeSeguir(Paso.SOBRE_TI, sinActividad));
        Respuestas trece = new Respuestas("", "", "MUJER", 13, 165, "62", "LIGERO", false, "", 0, 0);
        assertFalse(trece.sobreTiCompleto());
    }

    @Test
    public void prefieroNoDecirloDaUnPlanSinCalorias() {
        Respuestas r = new Respuestas("PERDER_PESO", "PRINCIPIANTE", "", 0, 0, "", "", true, "", 0, 0);
        assertFalse(r.conCalorias());
        assertTrue(completas().conCalorias());
        // Y al retomar no se vuelve a preguntar «Sobre ti».
        assertEquals(Paso.DONDE, AltaPasos.primeroSinContestar(r));
    }

    @Test
    public void seRetomaEnLaPrimeraSinContestar() {
        assertEquals(Paso.OBJETIVO, AltaPasos.primeroSinContestar(vacias()));
        Respuestas hastaNivel = new Respuestas("MANTENER_PESO", "AVANZADO", "", 0, 0, "", "", false, "", 0, 0);
        assertEquals(Paso.SOBRE_TI, AltaPasos.primeroSinContestar(hastaNivel));
        assertEquals(Paso.PLAN, AltaPasos.primeroSinContestar(completas()));
    }

    @Test
    public void losDiasYLosMinutosSonLosQueAceptaLaApi() {
        assertFalse(AltaPasos.esMinutoValido(90));
        assertTrue(AltaPasos.esMinutoValido(75));
        Respuestas sieteDias = new Respuestas("", "", "", 0, 0, "", "", false, "", 7, 45);
        assertFalse(AltaPasos.puedeSeguir(Paso.DIAS, sieteDias));
    }

    @Test
    public void laSemanaDeEjemploEsLaDelLienzo() {
        assertArrayEquals(new boolean[]{true, false, false, true, false, false, false}, AltaPasos.semanaDeEjemplo(2));
        assertArrayEquals(new boolean[]{true, false, true, false, true, false, false}, AltaPasos.semanaDeEjemplo(3));
        assertArrayEquals(new boolean[]{true, true, false, true, true, false, false}, AltaPasos.semanaDeEjemplo(4));
        assertArrayEquals(new boolean[]{true, true, true, true, true, true, false}, AltaPasos.semanaDeEjemplo(6));
    }

    @Test
    public void lasRutinasSeRepartenEnLosDiasYVuelvenAEmpezar() {
        // Tres días y tres rutinas: lunes A, miércoles B y viernes C.
        assertArrayEquals(new int[]{0, -1, 1, -1, 2, -1, -1}, AltaPasos.repartirRutinas(3, 3));
        // Cuatro días y dos rutinas: A, B, A, B.
        assertArrayEquals(new int[]{0, 1, -1, 0, 1, -1, -1}, AltaPasos.repartirRutinas(4, 2));
        assertArrayEquals(new int[]{-1, -1, -1, -1, -1, -1, -1}, AltaPasos.repartirRutinas(3, 0));
    }

    @Test
    public void elRotuloCortoQuitaLoQueComparteConElPrograma() {
        assertEquals("A", AltaPasos.rotuloCorto("Cuerpo completo", "Cuerpo completo A"));
        assertEquals("Torso", AltaPasos.rotuloCorto("Torso-pierna", "Torso"));
        assertEquals("Cuerpo completo", AltaPasos.rotuloCorto("Cuerpo completo", "Cuerpo completo"));
        assertEquals("Empuje", AltaPasos.rotuloCorto(null, " Empuje "));
    }
}
