package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

// ============================================================
// PeriodoProgresoTest — el selector «4 sem. · 8 sem. · Todo» de la gráfica de la ficha del
// ejercicio (GP-216, tablero 30): desde qué punto se dibuja según los días que han pasado.
// ============================================================
public class PeriodoProgresoTest {

    private static final String HOY = "2026-10-23";
    private static final List<String> FECHAS = Arrays.asList(
            "2026-07-01T10:00:00",   // 114 días
            "2026-09-01T10:00:00",   // 52 días
            "2026-09-10T10:00:00",   // 43 días
            "2026-09-30T10:00:00",   // 23 días
            "2026-10-20T10:00:00");  // 3 días

    @Test
    public void cuatroSemanasSonLosVeintiochoDiasUltimos() {
        assertEquals(3, PeriodoProgreso.primerIndice(FECHAS, HOY, 4));
    }

    @Test
    public void ochoSemanasLlegaHastaLosCincuentaYSeisDias() {
        assertEquals(1, PeriodoProgreso.primerIndice(FECHAS, HOY, 8));
    }

    @Test
    public void todoEmpiezaEnElPrimero() {
        assertEquals(0, PeriodoProgreso.primerIndice(FECHAS, HOY, 0));
    }

    @Test
    public void elDiaJustoDelLimiteEntra() {
        assertEquals(0, PeriodoProgreso.primerIndice(Arrays.asList("2026-09-25T08:00:00", "2026-10-22T08:00:00"), HOY, 4));
    }

    @Test
    public void siNingunoEntraNoQuedaNingunPunto() {
        assertEquals(2, PeriodoProgreso.primerIndice(Arrays.asList("2026-01-01T10:00:00", "2026-02-01T10:00:00"), HOY, 4));
    }

    @Test
    public void sinFechasNoHayNada() {
        assertEquals(0, PeriodoProgreso.primerIndice(Collections.emptyList(), HOY, 4));
    }
}
