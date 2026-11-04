package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Locale;

// ============================================================
// FechaUtilsTest — la fecha de «Conseguido · …» de los logros (GP-079), la fecha y hora del
// historial y la regla única de los días relativos (GP-228).
//
// No se compara con una cadena exacta: el formato medio sale de los datos de
// idioma del sistema, que no son iguales en la JVM de las pruebas y en Android.
// Lo que se fija es que usa el idioma pedido y que no revienta.
// ============================================================
public class FechaUtilsTest {

    @Test
    public void formatea_en_el_idioma_pedido() {
        String es = FechaUtils.formatearFechaMedia("2026-09-23T10:15:00", new Locale("es", "ES"));
        String en = FechaUtils.formatearFechaMedia("2026-09-23T10:15:00", Locale.US);
        assertTrue(es, es.contains("23") && es.contains("2026") && es.toLowerCase(Locale.ROOT).contains("sept"));
        assertTrue(en, en.contains("23") && en.contains("2026") && en.contains("Sep"));
        assertNotEquals(es, en);
    }

    @Test
    public void sin_fecha_o_ilegible_devuelve_null() {
        assertNull(FechaUtils.formatearFechaMedia(null, Locale.US));
        assertNull(FechaUtils.formatearFechaMedia("ayer", Locale.US));
        assertNull(FechaUtils.formatearFechaMedia("2026-13-45", Locale.US));
    }

    @Test
    public void fechaYHoraEnElIdiomaPedido() {
        String en = FechaUtils.formatearFechaHoraMedia("2026-09-30T18:05:00", Locale.US);
        assertTrue(en, en.contains("Sep") && en.contains("30") && en.contains("6:05"));
        String es = FechaUtils.formatearFechaHoraMedia("2026-09-30T18:05:00", ES);
        assertTrue(es, es.contains("30") && es.contains("18:05") && !es.contains("/"));
        // Sin hora, solo la fecha; ilegible, null.
        assertEquals(FechaUtils.formatearFechaMedia("2026-09-30", Locale.US),
                FechaUtils.formatearFechaHoraMedia("2026-09-30", Locale.US));
        assertNull(FechaUtils.formatearFechaHoraMedia("2026-09-30Tbasura", Locale.US));
        assertNull(FechaUtils.formatearFechaHoraMedia(null, Locale.US));
    }

    @Test
    public void fechaCortaConElPatronDelIdioma() {
        assertEquals("Sep 28", FechaUtils.fechaCorta("2026-09-28T10:00:00", Locale.US, "MMM d"));
        String es = FechaUtils.fechaCorta("2026-09-28T10:00:00", ES, "d MMM");
        assertTrue(es, es.startsWith("28 sep"));
        assertNull(FechaUtils.fechaCorta("28/09", Locale.US, "MMM d"));
    }

    @Test
    public void unaFechaImposibleNoSeEnseña() {
        assertNull(FechaUtils.formatearFechaHoraMedia("2026-13-45T10:00:00", Locale.US));
        assertNull(FechaUtils.fechaCorta("2026-02-30", Locale.US, "MMM d"));
    }

    // ── Días relativos ───────────────────────────────────────────────────────

    private static final Locale ES = new Locale("es", "ES");
    // El 3 de noviembre de 2026 es martes.
    private static final String HOY = "2026-11-03";
    private static final FechaUtils.TextosDia TEXTOS_ES =
            new FechaUtils.TextosDia("Hoy", "Ayer", "%1$s · %2$s", "%1$s pasado", "d MMM");
    private static final FechaUtils.TextosDia TEXTOS_EN =
            new FechaUtils.TextosDia("Today", "Yesterday", "%1$s · %2$s", "Last %1$s", "MMM d");

    @Test
    public void losTramosDeLaRegla() {
        assertEquals(FechaUtils.Relativo.HOY, FechaUtils.relativo("2026-11-03T07:00:00", HOY));
        assertEquals(FechaUtils.Relativo.AYER, FechaUtils.relativo("2026-11-02", HOY));
        assertEquals(FechaUtils.Relativo.ESTA_SEMANA, FechaUtils.relativo("2026-11-01", HOY));
        assertEquals(FechaUtils.Relativo.ESTA_SEMANA, FechaUtils.relativo("2026-10-28", HOY));    // 6 días
        assertEquals(FechaUtils.Relativo.SEMANA_PASADA, FechaUtils.relativo("2026-10-27", HOY));  // 7
        assertEquals(FechaUtils.Relativo.SEMANA_PASADA, FechaUtils.relativo("2026-10-21", HOY));  // 13
        assertEquals(FechaUtils.Relativo.FECHA, FechaUtils.relativo("2026-10-20", HOY));          // 14
        assertNull("futura", FechaUtils.relativo("2026-11-04", HOY));
        assertNull(FechaUtils.relativo("ayer", HOY));
    }

    @Test
    public void diaRelativoEnEspanol() {
        assertEquals("Hoy", FechaUtils.diaRelativo("2026-11-03T07:00:00", HOY, ES, TEXTOS_ES));
        assertEquals("Ayer · lunes", FechaUtils.diaRelativo("2026-11-02", HOY, ES, TEXTOS_ES));
        assertEquals("Domingo", FechaUtils.diaRelativo("2026-11-01", HOY, ES, TEXTOS_ES));
        assertEquals("Jueves pasado", FechaUtils.diaRelativo("2026-10-22", HOY, ES, TEXTOS_ES));
        String corta = FechaUtils.diaRelativo("2026-10-05", HOY, ES, TEXTOS_ES);
        assertTrue(corta, corta.startsWith("5 oct"));
    }

    @Test
    public void diaRelativoEnIngles() {
        assertEquals("Today", FechaUtils.diaRelativo("2026-11-03", HOY, Locale.US, TEXTOS_EN));
        assertEquals("Yesterday · Monday", FechaUtils.diaRelativo("2026-11-02", HOY, Locale.US, TEXTOS_EN));
        assertEquals("Sunday", FechaUtils.diaRelativo("2026-11-01", HOY, Locale.US, TEXTOS_EN));
        assertEquals("Last Thursday", FechaUtils.diaRelativo("2026-10-22", HOY, Locale.US, TEXTOS_EN));
        assertEquals("Oct 5", FechaUtils.diaRelativo("2026-10-05", HOY, Locale.US, TEXTOS_EN));
    }

    @Test
    public void unaSesionDeHaceUnMesNoEsUnLunes() {
        // «Tus últimas veces» ponía el día de la semana a cualquier fecha: el 5 de octubre, «Lunes».
        assertEquals("Oct 5", FechaUtils.diaRelativo("2026-10-05T18:00:00", HOY, Locale.US, TEXTOS_EN));
    }

    @Test
    public void diaRelativoFuturoOIlegibleEsVacio() {
        assertEquals("", FechaUtils.diaRelativo("2026-11-05", HOY, Locale.US, TEXTOS_EN));
        assertEquals("", FechaUtils.diaRelativo(null, HOY, Locale.US, TEXTOS_EN));
    }
}
