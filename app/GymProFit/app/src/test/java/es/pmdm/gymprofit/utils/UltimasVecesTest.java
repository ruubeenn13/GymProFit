package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

// ============================================================
// UltimasVecesTest — «Tus últimas veces» de la ficha del ejercicio (GP-216, tablero 30): una
// tarjeta por sesión, la más reciente primero, con el total de kilos, la diferencia con la
// sesión anterior y la serie que dio el récord (en dorado solo si esa sesión lo batió).
// ============================================================
public class UltimasVecesTest {

    private static UltimasVeces.Sesion sesion(int id, String fecha, UltimasVeces.Serie... series) {
        return new UltimasVeces.Sesion(id, fecha, Arrays.asList(series));
    }

    private static UltimasVeces.Serie s(double peso, int reps) {
        return new UltimasVeces.Serie(peso, reps);
    }

    @Test
    public void laMasRecientePrimeraAunqueLleguenDesordenadas() {
        List<UltimasVeces.Tarjeta> t = UltimasVeces.de(Arrays.asList(
                sesion(1, "2026-10-01T10:00:00", s(60, 8)),
                sesion(3, "2026-10-20T10:00:00", s(70, 8)),
                sesion(2, "2026-10-10T10:00:00", s(65, 8))), null, 5);
        assertEquals(3, t.get(0).sesionId);
        assertEquals(2, t.get(1).sesionId);
        assertEquals(1, t.get(2).sesionId);
    }

    @Test
    public void totalYDiferenciaConLaAnterior() {
        List<UltimasVeces.Tarjeta> t = UltimasVeces.de(Arrays.asList(
                sesion(1, "2026-10-01T10:00:00", s(60, 8), s(60, 8)),
                sesion(2, "2026-10-10T10:00:00", s(65, 8), s(60, 8))), null, 5);
        assertEquals(1000.0, t.get(0).totalKg, 0.001);
        assertEquals(40.0, t.get(0).diferenciaKg, 0.001);
        assertEquals(960.0, t.get(1).totalKg, 0.001);
        assertNull(t.get(1).diferenciaKg);
    }

    @Test
    public void laSerieMejorEsLaDeMasPesoYADiferenciaLaDeMasRepeticiones() {
        List<UltimasVeces.Tarjeta> t = UltimasVeces.de(Collections.singletonList(
                sesion(1, "2026-10-01T10:00:00", s(60, 8), s(65, 6), s(65, 8), s(60, 9))), null, 5);
        assertEquals(2, t.get(0).mejorIndice);
    }

    @Test
    public void elDoradoSoloEnLaSesionDelRecordVigente() {
        List<UltimasVeces.Tarjeta> t = UltimasVeces.de(Arrays.asList(
                sesion(1, "2026-10-01T10:00:00", s(60, 8)),
                sesion(2, "2026-10-10T10:00:00", s(65, 8))), 2, 5);
        assertTrue(t.get(0).esRecord);
        assertFalse(t.get(1).esRecord);
    }

    @Test
    public void sinRecordVigenteNingunaVaEnDorado() {
        List<UltimasVeces.Tarjeta> t = UltimasVeces.de(Collections.singletonList(
                sesion(1, "2026-10-01T10:00:00", s(60, 8))), null, 5);
        assertFalse(t.get(0).esRecord);
    }

    @Test
    public void respetaElLimiteYSaltaLasSesionesSinSeries() {
        List<UltimasVeces.Tarjeta> t = UltimasVeces.de(Arrays.asList(
                sesion(1, "2026-10-01T10:00:00", s(60, 8)),
                sesion(2, "2026-10-05T10:00:00"),
                sesion(3, "2026-10-10T10:00:00", s(65, 8)),
                sesion(4, "2026-10-15T10:00:00", s(70, 8))), null, 2);
        assertEquals(2, t.size());
        assertEquals(4, t.get(0).sesionId);
        assertEquals(3, t.get(1).sesionId);
    }

    @Test
    public void elPesoCorporalSumaCeroKilos() {
        List<UltimasVeces.Tarjeta> t = UltimasVeces.de(Collections.singletonList(
                sesion(1, "2026-10-01T10:00:00", s(0, 12), s(0, 10))), null, 5);
        assertEquals(0.0, t.get(0).totalKg, 0.001);
    }
}
