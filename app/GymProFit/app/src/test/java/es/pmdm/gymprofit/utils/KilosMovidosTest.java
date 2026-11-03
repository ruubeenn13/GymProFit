package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

// ============================================================
// KilosMovidosTest — «Kilos movidos» de Progreso (GP-216, lote 4): las semanas de cada período,
// el lunes desde el que se piden, el avance frente a la semana pasada y las etiquetas.
// ============================================================
public class KilosMovidosTest {

    // El 3 de noviembre de 2026 es martes; su lunes, el 2.
    private static final String HOY = "2026-11-03";

    @Test
    public void cuatroYOchoSemanasCuentanLaEnCurso() {
        assertEquals(4, KilosMovidos.semanas(4, null, HOY));
        assertEquals(8, KilosMovidos.semanas(8, "2020-01-01T10:00:00", HOY));
    }

    @Test
    public void todoVaDesdeLaSemanaDeLaPrimeraSesion() {
        // Primera sesión el domingo 25 de octubre: su semana empieza el 19; hasta la del 2 de
        // noviembre son tres semanas.
        assertEquals(3, KilosMovidos.semanas(0, "2026-10-25T19:00:00", HOY));
        // Una primera sesión de esta misma semana: una.
        assertEquals(1, KilosMovidos.semanas(0, "2026-11-02T08:00:00", HOY));
    }

    @Test
    public void todoSeQuedaEnElTopeDeLaApi() {
        assertEquals(KilosMovidos.TOPE_SEMANAS, KilosMovidos.semanas(0, "2019-05-01T10:00:00", HOY));
        assertEquals(104, KilosMovidos.TOPE_SEMANAS);
    }

    @Test
    public void todoSinSesionesOConFechaRaraEsUnaSemana() {
        assertEquals(1, KilosMovidos.semanas(0, null, HOY));
        assertEquals(1, KilosMovidos.semanas(0, "basura", HOY));
        // Una sesión con fecha futura no da semanas negativas.
        assertEquals(1, KilosMovidos.semanas(0, "2027-01-01T10:00:00", HOY));
    }

    @Test
    public void desdeEsElLunesALasCeroDeLaPrimeraSemana() {
        assertEquals("2026-11-02T00:00:00", KilosMovidos.desdeIso(HOY, 1));
        assertEquals("2026-09-14T00:00:00", KilosMovidos.desdeIso(HOY, 8));
        // Un domingo pertenece a la semana que empezó el lunes anterior.
        assertEquals("2026-10-26T00:00:00", KilosMovidos.desdeIso("2026-11-01", 1));
        // Y un lunes es su propio lunes.
        assertEquals("2026-11-02T00:00:00", KilosMovidos.desdeIso("2026-11-02", 1));
    }

    @Test
    public void desdeCruzaElCambioDeAnio() {
        assertEquals("2025-12-29T00:00:00", KilosMovidos.desdeIso("2026-01-07", 2));
    }

    @Test
    public void mientrasNoSuperaLaPasadaDiceSuPorcentaje() {
        KilosMovidos.Avance a = KilosMovidos.avance(12_648, 20_400);
        assertFalse(a.supera);
        assertEquals(62, a.porcentaje);
        // Sin entrenar aún esta semana: el 0 %.
        assertEquals(0, KilosMovidos.avance(0, 20_400).porcentaje);
    }

    @Test
    public void igualarLaPasadaNoEsSuperarla() {
        KilosMovidos.Avance a = KilosMovidos.avance(20_400, 20_400);
        assertFalse(a.supera);
        assertEquals(100, a.porcentaje);
    }

    @Test
    public void casiIgualNoRedondeaAlCien() {
        // 99,96 %: aún no la ha alcanzado, así que no dice «el 100 %».
        assertEquals(99, KilosMovidos.avance(20_392, 20_400).porcentaje);
    }

    @Test
    public void alSuperarlaDiceCuantoSube() {
        KilosMovidos.Avance a = KilosMovidos.avance(22_848, 20_400);
        assertTrue(a.supera);
        assertEquals(12, a.porcentaje);
    }

    @Test
    public void superarlaPorPocoNoDiceMasCero() {
        KilosMovidos.Avance a = KilosMovidos.avance(20_401, 20_400);
        assertTrue(a.supera);
        assertEquals(1, a.porcentaje);
    }

    @Test
    public void sinKilosLaPasadaNoHayPorcentaje() {
        assertNull(KilosMovidos.avance(5_000, 0));
        assertNull(KilosMovidos.avance(0, 0));
    }

    @Test
    public void lasEtiquetasNoSePisan() {
        // Caben: una por barra.
        assertEquals(1, KilosMovidos.pasoEtiquetas(40f, 36f));
        // Celdas de 3 px y etiquetas de 48: una cada 16 barras.
        assertEquals(16, KilosMovidos.pasoEtiquetas(3f, 48f));
        // Justo lo que cabe no salta a la siguiente.
        assertEquals(2, KilosMovidos.pasoEtiquetas(24f, 48f));
    }

    @Test
    public void laEtiquetaDeEstaSemanaSiempreSaleYLasDemasHaciaAtras() {
        int n = 104, paso = 16;
        assertTrue(KilosMovidos.llevaEtiqueta(103, n, paso));
        assertTrue(KilosMovidos.llevaEtiqueta(87, n, paso));
        assertFalse(KilosMovidos.llevaEtiqueta(86, n, paso));
        assertTrue(KilosMovidos.llevaEtiqueta(7, n, paso));
        assertFalse(KilosMovidos.llevaEtiqueta(0, n, paso));
        // Con paso 1, todas.
        for (int i = 0; i < 8; i++) assertTrue(KilosMovidos.llevaEtiqueta(i, 8, 1));
    }

    @Test
    public void losPeriodosSonCuatroOchoYTodo() {
        assertEquals(3, KilosMovidos.PERIODOS.length);
        assertEquals(4, KilosMovidos.PERIODOS[0]);
        assertEquals(8, KilosMovidos.PERIODOS[1]);
        assertEquals(0, KilosMovidos.PERIODOS[2]);
        assertEquals("se abre en 8 semanas, como el tablero", 8, KilosMovidos.PERIODOS[KilosMovidos.PERIODO_INICIAL]);
    }
}
