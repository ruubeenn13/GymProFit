package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import es.pmdm.gymprofit.model.logro.LogroProgreso;

// ============================================================
// LogrosProgresoTest — «Logros» de Progreso (GP-216, lote 4): los dos últimos conseguidos y los
// dos más cerca, del catálogo real y sin los que hoy no se pueden conseguir.
// ============================================================
public class LogrosProgresoTest {

    private static LogroProgreso conseguido(String tipo, String fecha) {
        return new LogroProgreso(tipo, true, fecha, 1, 1);
    }

    private static LogroProgreso pendiente(String tipo, int progreso, int umbral) {
        return new LogroProgreso(tipo, false, null, progreso, umbral);
    }

    private static List<String> tipos(List<LogroProgreso> l) {
        List<String> r = new ArrayList<>();
        for (LogroProgreso x : l) r.add(x.getTipo());
        return r;
    }

    @Test
    public void dosConseguidosDelMasRecienteYDosMasCerca() {
        List<LogroProgreso> r = LogrosProgreso.elegir(Arrays.asList(
                conseguido("PRIMERA_SESION", "2026-09-01T10:00:00"),
                conseguido("CONSTANCIA", "2026-10-20T10:00:00"),
                conseguido("CENTENARIO", "2026-10-30T10:00:00"),
                pendiente("DEDICADO", 13, 30),
                pendiente("OTRO", 25, 50)));
        assertEquals(Arrays.asList("CENTENARIO", "CONSTANCIA", "OTRO", "DEDICADO"), tipos(r));
    }

    @Test
    public void aIgualParteVaAntesElQueSeConsigueAntes() {
        List<LogroProgreso> r = LogrosProgreso.elegir(Arrays.asList(
                pendiente("GRANDE", 50, 100),
                pendiente("PEQUENO", 5, 10)));
        assertEquals(Arrays.asList("PEQUENO", "GRANDE"), tipos(r));
    }

    @Test
    public void unLogroSinAvanceNoEstaCerca() {
        // Cuenta nueva: todo a cero → nada que enseñar (la sección dice su vacío).
        assertTrue(LogrosProgreso.elegir(Arrays.asList(
                pendiente("PRIMERA_SESION", 0, 1), pendiente("DEDICADO", 0, 30))).isEmpty());
    }

    @Test
    public void losQueHoyNoSePuedenConseguirNoSalen() {
        List<LogroProgreso> r = LogrosProgreso.elegir(Arrays.asList(
                conseguido("OBJETIVO_CUMPLIDO", "2026-10-30T10:00:00"),
                pendiente("MAQUINA", 9, 10),
                pendiente("DEDICADO", 3, 30)));
        assertEquals(Arrays.asList("DEDICADO"), tipos(r));
    }

    @Test
    public void unConseguidoSinFechaVaAlFinal() {
        List<LogroProgreso> r = LogrosProgreso.elegir(Arrays.asList(
                conseguido("SIN_FECHA", null),
                conseguido("CON_FECHA", "2026-01-01T00:00:00")));
        assertEquals(Arrays.asList("CON_FECHA", "SIN_FECHA"), tipos(r));
    }

    @Test
    public void sinLista() {
        assertTrue(LogrosProgreso.elegir(null).isEmpty());
    }
}
