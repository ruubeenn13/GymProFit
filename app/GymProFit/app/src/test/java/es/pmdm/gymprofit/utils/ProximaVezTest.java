package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

// ============================================================
// ProximaVezTest — la pista «La próxima vez, a por…» de la ficha del ejercicio (GP-216,
// tablero 30). Solo existe la regla del programa: cuando todas las series llegan al máximo de
// repeticiones, se sube el peso. Sin máximo conocido o sin llegar en todas, no hay pista.
// ============================================================
public class ProximaVezTest {

    @Test
    public void todasLasSeriesEnElMaximoSubenElPeso() {
        assertTrue(ProximaVez.subirPeso(Arrays.asList(10, 10, 10), 10));
    }

    @Test
    public void pasarseDelMaximoTambienCuenta() {
        assertTrue(ProximaVez.subirPeso(Arrays.asList(12, 11), 10));
    }

    @Test
    public void unaSerieQueNoLlegaNoSubeElPeso() {
        assertFalse(ProximaVez.subirPeso(Arrays.asList(10, 9, 10), 10));
    }

    @Test
    public void sinMaximoConocidoNoSeInventaNada() {
        assertFalse(ProximaVez.subirPeso(Arrays.asList(10, 10, 10), null));
        assertFalse(ProximaVez.subirPeso(Arrays.asList(10, 10, 10), 0));
    }

    @Test
    public void sinSeriesNoHayPista() {
        assertFalse(ProximaVez.subirPeso(Collections.emptyList(), 10));
        assertFalse(ProximaVez.subirPeso(null, 10));
    }
}
