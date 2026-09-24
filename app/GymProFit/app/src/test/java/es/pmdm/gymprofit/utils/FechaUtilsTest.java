package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Locale;

// ============================================================
// FechaUtilsTest — la fecha de «Conseguido · …» de los logros (GP-079).
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
}
