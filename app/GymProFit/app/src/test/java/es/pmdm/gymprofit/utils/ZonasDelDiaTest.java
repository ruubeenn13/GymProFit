package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

// ============================================================
// ZonasDelDiaTest — las zonas de «Hoy toca» (GP-216, tablero 13) salen de los ejercicios de la
// rutina. La API reduce cada ejercicio a una clave de músculo (Musculos, en la API); la app
// hace lo mismo con el ejercicio que le llega.
// ============================================================
public class ZonasDelDiaTest {

    // ── Musculos: la misma tabla que la API ─────────────────────────────────────

    @Test
    public void elMusculoPrimarioManda() {
        assertEquals("biceps", Musculos.normalizar("Bíceps", "BRAZOS"));
        assertEquals("cuadriceps", Musculos.normalizar("cuadriceps", "PIERNAS"));
        assertEquals("gluteos", Musculos.normalizar("Abductores", "PIERNAS"));
        assertEquals("dorsales", Musculos.normalizar("espalda media", "ESPALDA"));
    }

    @Test
    public void sinMusculoPrimarioCaeAlGrupoGrueso() {
        assertEquals("pecho", Musculos.normalizar(null, "PECHO"));
        assertEquals("dorsales", Musculos.normalizar("", "espalda"));
        assertEquals("cuadriceps", Musculos.normalizar("  ", "PIERNAS"));
        assertEquals("abdominales", Musculos.normalizar(null, "ABDOMEN"));
        assertEquals("pecho", Musculos.normalizar(null, "FULLBODY"));
    }

    @Test
    public void elCardioNoEnciendeNingunMusculo() {
        assertNull(Musculos.normalizar(null, "CARDIO"));
        assertNull(Musculos.normalizar(null, null));
        assertNull(Musculos.normalizar("algo raro", "CARDIO"));
    }

    // ── De los ejercicios a las zonas ───────────────────────────────────────────

    @Test
    public void lasSeriesDeCadaEjercicioSumanASuMusculo() {
        ZonasDelDia z = ZonasDelDia.de(Arrays.asList(
                new ZonasDelDia.Ejercicio("cuadriceps", 4),
                new ZonasDelDia.Ejercicio("cuadriceps", 3),
                new ZonasDelDia.Ejercicio("isquiotibiales", 3)));
        Map<String, Integer> v = z.seriesPorMusculo;
        assertEquals(7, (int) v.get("cuadriceps"));
        assertEquals(3, (int) v.get("isquiotibiales"));
    }

    @Test
    public void lasZonasDelDiaSalenOrdenadasPorSeriesYSinRepetir() {
        ZonasDelDia z = ZonasDelDia.de(Arrays.asList(
                new ZonasDelDia.Ejercicio("pecho", 4),
                new ZonasDelDia.Ejercicio("triceps", 3),
                new ZonasDelDia.Ejercicio("hombros", 5),
                new ZonasDelDia.Ejercicio("triceps", 3)));
        List<Zonas.Zona> zonas = z.zonas;
        assertEquals(Arrays.asList(Zonas.BRAZOS, Zonas.HOMBROS, Zonas.PECHO), zonas);
    }

    @Test
    public void unaRutinaSinMusculosConocidosNoTieneZonas() {
        ZonasDelDia z = ZonasDelDia.de(Arrays.asList(new ZonasDelDia.Ejercicio(null, 3)));
        assertTrue(z.zonas.isEmpty());
        assertTrue(z.seriesPorMusculo.isEmpty());
        assertTrue(ZonasDelDia.de(null).zonas.isEmpty());
    }

    @Test
    public void unEjercicioSinSeriesCuentaUnaParaQueSeEnciendaSuMusculo() {
        ZonasDelDia z = ZonasDelDia.de(Arrays.asList(new ZonasDelDia.Ejercicio("pecho", 0)));
        assertEquals(1, (int) z.seriesPorMusculo.get("pecho"));
    }
}
