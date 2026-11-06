package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * GP-229: las tres gráficas por fecha dicen bien las cosas con pocos datos. Con 0 puntos, el
 * vacío; con 1, el punto con su valor y su fecha y la pista de apuntar otro; con 2 o más, la
 * línea. Los puntos son días: dos pesajes del mismo día son un punto.
 */
public class PuntosGraficaTest {

    @Test
    public void ceroUnoYDos() {
        assertEquals(PuntosGrafica.Que.VACIO, PuntosGrafica.de(0));
        assertEquals(PuntosGrafica.Que.UN_PUNTO, PuntosGrafica.de(1));
        assertEquals(PuntosGrafica.Que.LINEA, PuntosGrafica.de(2));
        assertEquals(PuntosGrafica.Que.LINEA, PuntosGrafica.de(40));
    }

    @Test
    public void dosPesajesDelMismoDiaSonUnPunto() {
        List<String> fechas = Arrays.asList("2026-11-06T08:00:00", "2026-11-06T21:30:00");
        List<Float> pesos = Arrays.asList(80.1f, 79.6f);
        List<EjeFechas.Punto> p = EjeFechas.porDia(fechas, pesos, EjeFechas.DelDia.ULTIMO);
        assertEquals(PuntosGrafica.Que.UN_PUNTO, PuntosGrafica.de(p.size()));
        assertEquals(79.6f, p.get(0).y, 0.001f);
    }

    @Test
    public void dosDiasSonLaLinea() {
        List<EjeFechas.Punto> p = EjeFechas.porDia(Arrays.asList("2026-11-05", "2026-11-06"),
                Arrays.asList(80f, 79.5f), EjeFechas.DelDia.ULTIMO);
        assertEquals(PuntosGrafica.Que.LINEA, PuntosGrafica.de(p.size()));
    }

    @Test
    public void sinPuntosEsElVacio() {
        List<EjeFechas.Punto> p = EjeFechas.porDia(Collections.<String>emptyList(),
                Collections.<Float>emptyList(), EjeFechas.DelDia.ULTIMO);
        assertEquals(PuntosGrafica.Que.VACIO, PuntosGrafica.de(p.size()));
    }
}
