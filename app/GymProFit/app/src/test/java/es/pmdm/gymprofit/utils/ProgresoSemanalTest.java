package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

// ============================================================
// ProgresoSemanalTest — «Tu progreso» de Inicio (GP-216, tablero 13): las barras de kilos por
// semana y el «+12 %». La semana en curso se dibuja pero no cuenta para el porcentaje:
// un jueves a medias parecería una caída.
// ============================================================
public class ProgresoSemanalTest {

    @Test
    public void laBarraMasAltaOcupaTodoElAlto() {
        float[] a = ProgresoSemanal.alturas(new double[]{18200, 20400, 10200});
        assertEquals(1f, a[1], 1e-6);
        assertEquals(18200 / 20400f, a[0], 1e-6);
        assertEquals(0.5f, a[2], 1e-6);
    }

    @Test
    public void unaSemanaSinEntrenoTieneBarraCeroYSiTodoEsCeroNoHayNinguna() {
        assertEquals(0f, ProgresoSemanal.alturas(new double[]{0, 100})[0], 0);
        float[] todo = ProgresoSemanal.alturas(new double[]{0, 0, 0});
        for (float f : todo) assertEquals(0f, f, 0);
    }

    @Test
    public void elPorcentajeCompararLaUltimaSemanaCompletaConLaPrimeraConDatos() {
        // 18.200 → 19.800 (la penúltima); la última, en curso, no cuenta.
        ProgresoSemanal.Variacion v = ProgresoSemanal.variacion(
                new double[]{18200, 18900, 18600, 19500, 19800, 4000}, true);
        assertEquals(9, v.porcentaje);          // 8,8 % redondeado
        assertEquals(5, v.semanas);
    }

    @Test
    public void sinSemanaEnCursoEnLosDatosSeUsanTodas() {
        ProgresoSemanal.Variacion v = ProgresoSemanal.variacion(
                new double[]{18200, 18900, 18600, 19500, 19800, 20400}, false);
        assertEquals(12, v.porcentaje);
        assertEquals(6, v.semanas);
    }

    @Test
    public void bajarSeDiceConSignoMenos() {
        ProgresoSemanal.Variacion v = ProgresoSemanal.variacion(new double[]{10000, 8000, 5000}, true);
        assertEquals(-20, v.porcentaje);
    }

    @Test
    public void lasSemanasVaciasDelPrincipioNoSonLaBase() {
        ProgresoSemanal.Variacion v = ProgresoSemanal.variacion(new double[]{0, 0, 10000, 12000, 3000}, true);
        assertEquals(20, v.porcentaje);
        assertEquals(2, v.semanas);
    }

    @Test
    public void conUnaSolaSemanaConDatosNoHayPorcentaje() {
        assertNull(ProgresoSemanal.variacion(new double[]{0, 0, 10000, 3000}, true));
        assertNull(ProgresoSemanal.variacion(new double[]{10000}, true));
        assertNull(ProgresoSemanal.variacion(new double[]{}, true));
    }

    @Test
    public void lasSemanasDeDescansoDelFinalSeSaltanParaLaComparacion() {
        // La última completa sin entreno (descanso) no es una caída del 100 %: se compara con la
        // última que sí tiene.
        ProgresoSemanal.Variacion v = ProgresoSemanal.variacion(new double[]{10000, 12000, 0, 500}, true);
        assertEquals(20, v.porcentaje);
    }

    @Test
    public void hayAlgoQueDibujarSoloSiAlgunaSemanaTieneKilos() {
        assertTrue(ProgresoSemanal.hayDatos(new double[]{0, 5, 0}));
        assertFalse(ProgresoSemanal.hayDatos(new double[]{0, 0}));
        assertFalse(ProgresoSemanal.hayDatos(new double[]{}));
    }
}
