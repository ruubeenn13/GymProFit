package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import es.pmdm.gymprofit.model.sesion.VolumenSemanal;
import es.pmdm.gymprofit.utils.MedidaSemanal.Medida;

// ============================================================
// MedidaSemanalTest — kilos o series en las barras por semana (GP-249): un período sin kilos,
// con algunos y con todos; sin nada; y la API anterior a GP-249, que no manda series.
// ============================================================
public class MedidaSemanalTest {

    @Test
    public void unPeriodoSinKilosCuentaSeries() {
        List<VolumenSemanal> s = semanas(new double[]{0, 0, 0}, new Integer[]{9, 0, 12});
        assertEquals(Medida.SERIES, MedidaSemanal.de(s));
        assertArrayEquals(new double[]{9, 0, 12}, MedidaSemanal.valores(Medida.SERIES, s), 0);
    }

    @Test
    public void conKilosEnAlgunaSemanaSiguenLosKilosYLaDePesoCorporalVaACero() {
        List<VolumenSemanal> s = semanas(new double[]{0, 5400, 0}, new Integer[]{10, 12, 8});
        assertEquals(Medida.KILOS, MedidaSemanal.de(s));
        assertArrayEquals(new double[]{0, 5400, 0}, MedidaSemanal.valores(Medida.KILOS, s), 0);
    }

    @Test
    public void conKilosEnTodasLasSemanasSonKilos() {
        List<VolumenSemanal> s = semanas(new double[]{4200, 5400, 3000}, new Integer[]{10, 12, 8});
        assertEquals(Medida.KILOS, MedidaSemanal.de(s));
    }

    @Test
    public void sinKilosNiSeriesNoHayNadaQueEnsenar() {
        assertEquals(Medida.NINGUNA, MedidaSemanal.de(semanas(new double[]{0, 0}, new Integer[]{0, 0})));
        assertEquals(Medida.NINGUNA, MedidaSemanal.de(new ArrayList<>()));
        assertEquals(Medida.NINGUNA, MedidaSemanal.de(null));
    }

    @Test
    public void unaApiSinSeriesNoSeConfundeConSeriesACeroDeMas() {
        // La API anterior a GP-249 no manda «series»: sin kilos, no hay nada que contar.
        assertEquals(Medida.NINGUNA, MedidaSemanal.de(semanas(new double[]{0, 0}, new Integer[]{null, null})));
    }

    @Test
    public void elAvanceDeLasSeriesSigueLaReglaDeLosKilos() {
        double[] v = MedidaSemanal.valores(Medida.SERIES, semanas(new double[]{0, 0}, new Integer[]{10, 12}));
        KilosMovidos.Avance a = KilosMovidos.avance(v[1], v[0]);
        assertEquals(true, a.supera);
        assertEquals(20, a.porcentaje);
        // Sin series la semana pasada, nada con qué comparar.
        assertNull(KilosMovidos.avance(5, 0));
    }

    private static List<VolumenSemanal> semanas(double[] kilos, Integer[] series) {
        List<VolumenSemanal> l = new ArrayList<>();
        for (int i = 0; i < kilos.length; i++) {
            VolumenSemanal v = new VolumenSemanal();
            v.setInicio("2026-10-" + (10 + i * 7) + "T00:00:00");
            v.setVolumenKg(kilos[i]);
            v.setSeries(series[i]);
            l.add(v);
        }
        return l;
    }
}
