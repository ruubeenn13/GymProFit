package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

import es.pmdm.gymprofit.model.sesion.ResumenSesion;

// ============================================================
// ResumenCascadaTest — la cabecera del resumen del entreno en cascada (GP-250, DEC-060).
// Antes una sesión a 0 kg decía «−100 % que la última vez»: comparaba 0 kg con la anterior.
// Ahora, con kilos, como siempre; con 0 kg y series, las series son la cifra grande, sin
// comparación ni ficha de series; sin series, el aviso de la cascada.
// ============================================================
public class ResumenCascadaTest {

    private static final ResumenSesion.SesionAnterior ANTERIOR =
            ResumenSesion.SesionAnterior.de(9, "2026-11-03T18:00:00", 8000);

    private static ResumenSesion.Ejercicio ej(double kilos, int series) {
        return ResumenSesion.Ejercicio.de(1, "Dominadas", kilos, series, false, null);
    }

    @Test
    public void con_kilos_la_cifra_son_los_kilos_con_su_comparacion_y_sus_series() {
        ResumenSesion r = ResumenSesion.de(10, 8400, 18, ANTERIOR, Arrays.asList(ej(8400, 18)));
        ComparacionEntreno.Cabecera c = ComparacionEntreno.cabecera(r);

        assertEquals(CifraTarjeta.Tipo.KILOS, c.cifra.tipo);
        assertEquals(8400, c.cifra.kilos);
        assertEquals(5, c.variacion.porcentaje);
        assertTrue(c.fichaSeries);
    }

    @Test
    public void a_0_kg_con_series_la_cifra_son_las_series_y_no_hay_menos_100() {
        ResumenSesion r = ResumenSesion.de(10, 0, 12, ANTERIOR, Arrays.asList(ej(0, 12)));
        ComparacionEntreno.Cabecera c = ComparacionEntreno.cabecera(r);

        assertEquals(CifraTarjeta.Tipo.SERIES, c.cifra.tipo);
        assertEquals(12, c.cifra.series);
        assertNull("nada de «−100 % que la última vez»", c.variacion);
        assertFalse("las series ya son la cifra: sin su ficha", c.fichaSeries);
        assertNull(ComparacionEntreno.variacionDeSesion(r));
        assertEquals("12 series", Cadenas.es().p("tarjeta_series", 12, 12).replace(' ', ' '));
        assertEquals("12 sets", Cadenas.en().p("tarjeta_series", 12, 12).replace(' ', ' '));
    }

    @Test
    public void sin_series_el_aviso_de_la_cascada() {
        ComparacionEntreno.Cabecera sinEjercicios = ComparacionEntreno.cabecera(
                ResumenSesion.de(10, 0, 0, ANTERIOR, new ArrayList<>()));
        assertEquals(CifraTarjeta.Tipo.SIN_EJERCICIOS, sinEjercicios.cifra.tipo);
        assertNull(sinEjercicios.variacion);
        assertFalse(sinEjercicios.fichaSeries);

        ComparacionEntreno.Cabecera sinSeries = ComparacionEntreno.cabecera(
                ResumenSesion.de(10, 0, 0, ANTERIOR, Arrays.asList(ej(0, 0))));
        assertEquals(CifraTarjeta.Tipo.SIN_SERIES, sinSeries.cifra.tipo);
        assertNull(sinSeries.variacion);
        assertEquals("Sin ejercicios apuntados", Cadenas.es().s("tarjeta_sin_ejercicios"));
        assertEquals("No sets done", Cadenas.en().s("tarjeta_sin_series"));
    }

    @Test
    public void la_primera_vez_con_kilos_no_tiene_comparacion() {
        ComparacionEntreno.Cabecera c = ComparacionEntreno.cabecera(
                ResumenSesion.de(10, 3000, 9, null, Arrays.asList(ej(3000, 9))));
        assertEquals(CifraTarjeta.Tipo.KILOS, c.cifra.tipo);
        assertNull(c.variacion);
        assertTrue(c.fichaSeries);
    }
}
