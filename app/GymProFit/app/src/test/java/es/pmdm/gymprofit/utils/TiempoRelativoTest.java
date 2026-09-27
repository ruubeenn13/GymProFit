package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

// ============================================================
// TiempoRelativoTest — GP-105: los días se cuentan por calendario, no por horas.
// ============================================================
public class TiempoRelativoTest {

    @Test
    public void cuenta_dias_de_calendario() {
        assertEquals(0, TiempoRelativo.diasEntre("2026-09-27T23:59:00", "2026-09-27"));
        assertEquals(1, TiempoRelativo.diasEntre("2026-09-26T23:00:00", "2026-09-27"));
        assertEquals(4, TiempoRelativo.diasEntre("2026-09-23", "2026-09-27"));
        // Cruza el cambio de hora de octubre y el fin de año.
        assertEquals(7, TiempoRelativo.diasEntre("2026-10-22", "2026-10-29"));
        assertEquals(2, TiempoRelativo.diasEntre("2025-12-31", "2026-01-02"));
    }

    @Test
    public void una_fecha_que_no_se_entiende_da_menos_uno() {
        assertEquals(-1, TiempoRelativo.diasEntre(null, "2026-09-27"));
        assertEquals(-1, TiempoRelativo.diasEntre("ayer", "2026-09-27"));
    }
}
