package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

// ============================================================
// CambioPesoTest — «Tu cuerpo» de Progreso (GP-216, lote 4): el último peso, su cambio en las
// últimas 8 semanas, las semanas que de verdad abarca y los puntos de la línea.
// ============================================================
public class CambioPesoTest {

    private static CambioPeso.Punto p(String fecha, double peso) {
        return new CambioPeso.Punto(fecha, peso);
    }

    @Test
    public void elCambioEsContraElPesoMasAntiguoDeLaVentana() {
        CambioPeso.Resultado r = CambioPeso.de(Arrays.asList(
                p("2026-11-02T08:00:00", 79.4),
                p("2026-10-12T08:00:00", 80.6),
                p("2026-09-07T08:00:00", 82.1),
                p("2026-07-01T08:00:00", 85.0)));   // fuera: más de 56 días antes del último
        assertNotNull(r);
        assertEquals(79.4, r.ultimo.peso, 1e-9);
        assertEquals(-2.7, r.cambio, 1e-9);
        assertEquals(8, r.semanas);
        assertEquals(3, r.puntos.size());
        assertEquals("del más antiguo al último", 82.1, r.puntos.get(0).peso, 1e-9);
    }

    @Test
    public void conPocasSemanasDiceLasQueSon() {
        CambioPeso.Resultado r = CambioPeso.de(Arrays.asList(
                p("2026-11-02T08:00:00", 80.0),
                p("2026-10-12T08:00:00", 79.2)));
        assertEquals(0.8, r.cambio, 1e-9);
        assertEquals(3, r.semanas);
    }

    @Test
    public void dosPesosElMismoDiaSonUnaSemanaComoMinimo() {
        CambioPeso.Resultado r = CambioPeso.de(Arrays.asList(
                p("2026-11-02T20:00:00", 80.0),
                p("2026-11-02T08:00:00", 80.4)));
        assertEquals(1, r.semanas);
    }

    @Test
    public void unSoloPesoNoTieneCambio() {
        CambioPeso.Resultado r = CambioPeso.de(Collections.singletonList(p("2026-11-02T08:00:00", 80.0)));
        assertNotNull(r);
        assertNull(r.cambio);
        assertEquals(1, r.puntos.size());
    }

    @Test
    public void lasMedicionesSinPesoNoCuentan() {
        // La más reciente es solo de cintura (peso 0): el último peso es el de antes.
        CambioPeso.Resultado r = CambioPeso.de(Arrays.asList(
                p("2026-11-03T08:00:00", 0),
                p("2026-11-02T08:00:00", 79.4),
                p("2026-10-26T08:00:00", 79.9)));
        assertEquals(79.4, r.ultimo.peso, 1e-9);
        assertEquals(-0.5, r.cambio, 1e-9);
    }

    @Test
    public void sinNingunPesoNoHayNada() {
        assertNull(CambioPeso.de(Collections.emptyList()));
        assertNull(CambioPeso.de(Collections.singletonList(p("2026-11-02T08:00:00", 0))));
    }

    @Test
    public void lasSemanasSeRedondeanALaMasCercana() {
        // 17 días son 2,4 semanas → 2; 18 días, 2,6 → 3.
        assertEquals(2, CambioPeso.de(Arrays.asList(
                p("2026-11-02T08:00:00", 80.0), p("2026-10-16T08:00:00", 81.0))).semanas);
        assertEquals(3, CambioPeso.de(Arrays.asList(
                p("2026-11-02T08:00:00", 80.0), p("2026-10-15T08:00:00", 81.0))).semanas);
    }

    @Test
    public void unaMedicionPosteriorAlUltimoNoCuenta() {
        // La lista llega desordenada: la del 5 va detrás de la del 2, que se toma por el último.
        CambioPeso.Resultado r = CambioPeso.de(Arrays.asList(
                p("2026-11-02T08:00:00", 79.4),
                p("2026-11-05T08:00:00", 70.0),
                p("2026-10-26T08:00:00", 79.9)));
        assertEquals(-0.5, r.cambio, 1e-9);
        assertEquals(2, r.puntos.size());
    }

    @Test
    public void elCambioSeRedondeaAlDecimo() {
        CambioPeso.Resultado r = CambioPeso.de(Arrays.asList(
                p("2026-11-02T08:00:00", 79.44),
                p("2026-10-26T08:00:00", 82.11)));
        assertEquals(-2.7, r.cambio, 1e-9);
    }
}
