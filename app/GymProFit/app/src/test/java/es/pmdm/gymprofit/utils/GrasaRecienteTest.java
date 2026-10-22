package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import es.pmdm.gymprofit.model.medicion.MedicionCorporal;

// ============================================================
// GrasaRecienteTest — qué medición aporta el % de grasa a la proteína (GP-206):
// la más reciente que lo traiga, y un NULL nunca vale como 0 %.
// ============================================================
public class GrasaRecienteTest {

    private static MedicionCorporal medicion(String fecha, Double grasa) {
        MedicionCorporal m = new MedicionCorporal();
        m.setFecha(fecha);
        m.setGrasaCorporal(grasa);
        return m;
    }

    @Test
    public void sinMedicionesONingunaConPorcentajeNoHayMedida() {
        assertNull(GrasaReciente.elegir(null));
        assertNull(GrasaReciente.elegir(Collections.<MedicionCorporal>emptyList()));
        assertNull(GrasaReciente.elegir(Arrays.asList(
                medicion("2026-10-20T10:00:00", null), medicion("2026-10-01T10:00:00", null))));
    }

    @Test
    public void unPorcentajeNuloSeSaltaYNoCuentaComoCero() {
        GrasaReciente.Medida m = GrasaReciente.elegir(Arrays.asList(
                medicion("2026-10-20T10:00:00", null), medicion("2026-10-01T10:00:00", 22.5)));
        assertEquals(22.5, m.pct, 0.0001);
    }

    @Test
    public void conVariasEligeLaMasRecienteSeaCualSeaElOrdenDeLaLista() {
        GrasaReciente.Medida m = GrasaReciente.elegir(Arrays.asList(
                medicion("2026-09-01T10:00:00", 25.0), medicion("2026-10-15T08:00:00", 21.0),
                medicion("2026-10-02T10:00:00", 23.0)));
        assertEquals(21.0, m.pct, 0.0001);
    }

    @Test
    public void unaFechaIlegibleNoDaMedida() {
        assertNull(GrasaReciente.elegir(Collections.singletonList(medicion("ayer", 20.0))));
        assertNull(GrasaReciente.elegir(Collections.singletonList(medicion(null, 20.0))));
    }

    @Test
    public void losDiasSeCuentanDesdeElDiaDeLaMedicion() {
        GrasaReciente.Medida m = GrasaReciente.elegir(Collections.singletonList(medicion("2026-10-01T23:59:00", 20.0)));
        long diaDespues = m.fechaMs + 24L * 60 * 60 * 1000 + 3_600_000;
        assertEquals(0, m.dias(m.fechaMs + 3_600_000));
        assertEquals(1, m.dias(diaDespues));
        assertEquals(-1, m.dias(m.fechaMs - 1));
    }
}
