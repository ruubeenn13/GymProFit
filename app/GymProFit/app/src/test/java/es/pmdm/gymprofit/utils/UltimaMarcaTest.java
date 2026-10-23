package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

// ============================================================
// UltimaMarcaTest — «Última: 80 kg × 8 · récord» de cada ejercicio en el detalle de la rutina
// (GP-216, tablero 32): la mejor serie de la última vez, y si fue el récord vigente.
// ============================================================
public class UltimaMarcaTest {

    private static UltimasVeces.Serie s(double peso, int reps) {
        return new UltimasVeces.Serie(peso, reps);
    }

    @Test
    public void laMejorSerieEsLaDeMasPeso() {
        UltimaMarca m = UltimaMarca.mejorDe(Arrays.asList(s(70, 10), s(80, 6), s(75, 8)));
        assertEquals(80.0, m.peso, 0.001);
        assertEquals(6, m.repeticiones);
    }

    @Test
    public void conElMismoPesoGanaLaDeMasRepeticiones() {
        UltimaMarca m = UltimaMarca.mejorDe(Arrays.asList(s(80, 6), s(80, 8), s(80, 7)));
        assertEquals(8, m.repeticiones);
    }

    @Test
    public void sinPesoManganLasRepeticiones() {
        UltimaMarca m = UltimaMarca.mejorDe(Arrays.asList(s(0, 12), s(0, 15), s(0, 10)));
        assertEquals(0.0, m.peso, 0.001);
        assertEquals(15, m.repeticiones);
    }

    @Test
    public void sinSeriesNoHayMarca() {
        assertNull(UltimaMarca.mejorDe(Collections.emptyList()));
        assertNull(UltimaMarca.mejorDe(null));
    }

    @Test
    public void esRecordSiCoincideElDiaYLaMarca() {
        UltimaMarca m = new UltimaMarca(80, 8);
        assertTrue(m.esRecord("2026-10-19T10:00:00", "2026-10-19T10:41:00", 80.0, 8));
    }

    @Test
    public void unRecordDeOtroDiaNoCuenta() {
        UltimaMarca m = new UltimaMarca(80, 8);
        assertFalse(m.esRecord("2026-10-19T10:00:00", "2026-10-12T10:41:00", 80.0, 8));
    }

    @Test
    public void unRecordConOtraMarcaNoCuenta() {
        UltimaMarca m = new UltimaMarca(77.5, 8);
        assertFalse(m.esRecord("2026-10-19T10:00:00", "2026-10-19T10:41:00", 80.0, 8));
    }

    @Test
    public void sinRecordVigenteNoEsRecord() {
        assertFalse(new UltimaMarca(80, 8).esRecord("2026-10-19T10:00:00", null, null, 0));
    }
}
