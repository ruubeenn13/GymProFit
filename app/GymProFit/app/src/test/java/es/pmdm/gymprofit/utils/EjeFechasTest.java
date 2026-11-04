package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

// ============================================================
// EjeFechasTest — las gráficas con período en semanas van por fecha (GP-228): la conversión de
// una fecha a la x del eje, de la x a su etiqueta y la regla de un punto por día.
// ============================================================
public class EjeFechasTest {

    private static final Locale ES = new Locale("es", "ES");

    @Test
    public void laXEsElDiaDeCalendario() {
        assertEquals(Long.valueOf(0), EjeFechas.dia("1970-01-01"));
        assertEquals(Long.valueOf(20_394), EjeFechas.dia("2025-11-02T08:30:00"));
        // La hora no cuenta: el mismo día es la misma x.
        assertEquals(EjeFechas.dia("2026-11-02T00:00:00"), EjeFechas.dia("2026-11-02T23:59:59"));
    }

    @Test
    public void laDistanciaEsLaDeLosDias() {
        // Un día y un mes: la una treinta veces la otra, no las dos iguales como por orden.
        List<EjeFechas.Punto> p = EjeFechas.porDia(
                Arrays.asList("2026-09-01T08:00:00", "2026-10-01T08:00:00", "2026-10-02T08:00:00"),
                Arrays.asList(80f, 79f, 78.8f), EjeFechas.DelDia.ULTIMO);
        assertEquals(30f, p.get(1).x - p.get(0).x, 0f);
        assertEquals(1f, p.get(2).x - p.get(1).x, 0f);
    }

    @Test
    public void cruzaElCambioDeHoraYElDeAnio() {
        // 25 de octubre de 2026: el día de 25 horas en España.
        assertEquals(1L, EjeFechas.dia("2026-10-26") - EjeFechas.dia("2026-10-25"));
        assertEquals(1L, EjeFechas.dia("2027-01-01") - EjeFechas.dia("2026-12-31"));
    }

    @Test
    public void unaFechaQueNoSeEntiendeNoEsPunto() {
        assertNull(EjeFechas.dia(null));
        assertNull(EjeFechas.dia("2026"));
        assertNull(EjeFechas.dia("2026-13-45"));
        assertNull(EjeFechas.dia("ayer-y-hoy"));
        List<EjeFechas.Punto> p = EjeFechas.porDia(Arrays.asList("basura", "2026-10-02"),
                Arrays.asList(1f, 2f), EjeFechas.DelDia.ULTIMO);
        assertEquals(1, p.size());
        assertEquals(2f, p.get(0).y, 0f);
    }

    @Test
    public void deLaXSeVuelveASuFecha() {
        float x = EjeFechas.dia("2026-09-28T19:00:00");
        assertEquals("2026-09-28", EjeFechas.iso(x));
        // Una x entre dos días (la de una etiqueta del eje) va al día más cercano.
        assertEquals("2026-09-29", EjeFechas.iso(x + 0.6f));
    }

    @Test
    public void laEtiquetaEsLaFechaCortaDelIdioma() {
        float x = EjeFechas.dia("2026-09-28");
        assertEquals("Sep 28", EjeFechas.etiqueta(x, Locale.US, "MMM d"));
        String es = EjeFechas.etiqueta(x, ES, "d MMM");
        assertTrue(es, es.startsWith("28 sep"));
    }

    @Test
    public void laEtiquetaNoCambiaDeDiaConElHusoDelMovil() {
        // Al oeste de Greenwich, la medianoche UTC de un día es aún el día anterior: la etiqueta y
        // la fecha se sacan en UTC para que el 28 siga siendo el 28.
        java.util.TimeZone antes = java.util.TimeZone.getDefault();
        try {
            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("America/Los_Angeles"));
            float x = EjeFechas.dia("2026-09-28");
            assertEquals("Sep 28", EjeFechas.etiqueta(x, Locale.US, "MMM d"));
            assertEquals("2026-09-28", EjeFechas.iso(x));
        } finally {
            java.util.TimeZone.setDefault(antes);
        }
    }

    @Test
    public void unPuntoPorDiaElUltimoOElMayor() {
        List<String> f = Arrays.asList("2026-10-02T20:00:00", "2026-10-02T08:00:00", "2026-10-01T09:00:00");
        List<Float> v = Arrays.asList(79.6f, 80.4f, 80.0f);
        List<EjeFechas.Punto> ultimo = EjeFechas.porDia(f, v, EjeFechas.DelDia.ULTIMO);
        assertEquals(2, ultimo.size());
        assertEquals("en orden de fecha", "2026-10-01", EjeFechas.iso(ultimo.get(0).x));
        assertEquals("el pesaje de la noche corrige al de la mañana", 79.6f, ultimo.get(1).y, 0f);
        List<EjeFechas.Punto> mayor = EjeFechas.porDia(f, v, EjeFechas.DelDia.MAYOR);
        assertEquals("la mejor marca del día", 80.4f, mayor.get(1).y, 0f);
    }

    @Test
    public void aIgualHoraSeQuedaElQueVieneDespues() {
        List<EjeFechas.Punto> p = EjeFechas.porDia(Arrays.asList("2026-10-02T08:00:00", "2026-10-02T08:00:00"),
                Arrays.asList(1f, 2f), EjeFechas.DelDia.ULTIMO);
        assertEquals(2f, p.get(0).y, 0f);
    }

    @Test
    public void conUnSoloPuntoSoloSuEtiqueta() {
        List<EjeFechas.Punto> uno = EjeFechas.porDia(Collections.singletonList("2026-10-02"),
                Collections.singletonList(80f), EjeFechas.DelDia.ULTIMO);
        float x = uno.get(0).x;
        assertTrue(EjeFechas.llevaEtiqueta(x, uno));
        assertFalse("el «ayer» que añade la gráfica no lleva fecha", EjeFechas.llevaEtiqueta(x - 1, uno));
        assertFalse(EjeFechas.llevaEtiqueta(x + 1, uno));
        List<EjeFechas.Punto> dos = EjeFechas.porDia(Arrays.asList("2026-10-02", "2026-10-09"),
                Arrays.asList(80f, 79f), EjeFechas.DelDia.ULTIMO);
        assertTrue(EjeFechas.llevaEtiqueta(dos.get(0).x + 3, dos));
    }

    @Test
    public void sinFechasNingunPunto() {
        assertTrue(EjeFechas.porDia(Collections.emptyList(), Collections.emptyList(), EjeFechas.DelDia.MAYOR).isEmpty());
    }
}
