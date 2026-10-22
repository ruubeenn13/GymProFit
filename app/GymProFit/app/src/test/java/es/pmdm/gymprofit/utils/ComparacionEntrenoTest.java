package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import es.pmdm.gymprofit.model.sesion.ResumenSesion;

// ============================================================
// ComparacionEntrenoTest — «Frente a la última vez» del resumen (GP-210 paso 1, GP-023).
//
// Por ejercicio, los kilos de hoy y el % frente a la última vez que se hizo ese ejercicio.
// La primera vez no tiene porcentaje («Primera vez: es tu punto de partida»), y lo que no
// mueve kilos (por tiempo) o no tiene con qué compararse no inventa un cero.
// ============================================================
public class ComparacionEntrenoTest {

    private static ResumenSesion.Ejercicio ej(String nombre, double kilos, Double antes, boolean tiempo) {
        return ResumenSesion.Ejercicio.de(1, nombre, kilos, 3, tiempo,
                antes == null ? null : ResumenSesion.UltimaMarca.de("2026-10-15T18:00:00", antes));
    }

    private static ResumenSesion con(ResumenSesion.Ejercicio... e) {
        return ResumenSesion.de(10, 5000, 12, null, Arrays.asList(e));
    }

    @Test
    public void sube_baja_e_igual_se_redondean_al_entero() {
        List<ComparacionEntreno.Fila> f = ComparacionEntreno.filas(con(
                ej("Press de banca", 2065, 1912.0, false),   // +8,0 %
                ej("Sentadilla", 2400, 2308.0, false),       // +3,99 → +4 %
                ej("Remo con barra", 1640, 1640.0, false),   // igual
                ej("Curl", 900, 1000.0, false)));            // −10 %

        assertEquals(4, f.size());
        assertEquals(ComparacionEntreno.Tendencia.SUBE, f.get(0).variacion.tendencia);
        assertEquals(8, f.get(0).variacion.porcentaje);
        assertEquals(4, f.get(1).variacion.porcentaje);
        assertEquals(ComparacionEntreno.Tendencia.IGUAL, f.get(2).variacion.tendencia);
        assertEquals(0, f.get(2).variacion.porcentaje);
        assertEquals(ComparacionEntreno.Tendencia.BAJA, f.get(3).variacion.tendencia);
        assertEquals(-10, f.get(3).variacion.porcentaje);
    }

    @Test
    public void una_diferencia_que_redondea_a_cero_es_igual_y_no_un_mas_cero() {
        ComparacionEntreno.Variacion v = ComparacionEntreno.variacion(1002, 1000);
        assertEquals(ComparacionEntreno.Tendencia.IGUAL, v.tendencia);
        assertEquals(0, v.porcentaje);
    }

    @Test
    public void la_primera_vez_no_tiene_variacion() {
        List<ComparacionEntreno.Fila> f = ComparacionEntreno.filas(con(ej("Dominadas", 840, null, false)));

        assertEquals(1, f.size());
        assertTrue(f.get(0).primeraVez());
        assertNull(f.get(0).variacion);
        assertEquals(840, f.get(0).kilos, 0.001);
    }

    @Test
    public void una_marca_anterior_sin_kilos_no_da_porcentaje_imposible() {
        // Antes sin peso (peso corporal): dividir entre 0 no es un +∞ %, es no tener con qué comparar.
        List<ComparacionEntreno.Fila> f = ComparacionEntreno.filas(con(ej("Fondos", 600, 0.0, false)));

        assertEquals(1, f.size());
        assertNull(f.get(0).variacion);
        assertTrue(f.get(0).primeraVez());
    }

    @Test
    public void los_ejercicios_por_tiempo_y_los_de_cero_kilos_no_salen() {
        List<ComparacionEntreno.Fila> f = ComparacionEntreno.filas(con(
                ej("Plancha", 0, null, true),
                ej("Burpees", 0, 0.0, false),
                ej("Remo", 1200, 1100.0, false)));

        assertEquals(1, f.size());
        assertEquals("Remo", f.get(0).nombre);
    }

    @Test
    public void sin_ejercicios_no_hay_filas() {
        assertTrue(ComparacionEntreno.filas(con()).isEmpty());
        assertTrue(ComparacionEntreno.filas(null).isEmpty());
    }

    @Test
    public void la_variacion_de_la_sesion_frente_a_la_anterior_de_la_rutina() {
        ComparacionEntreno.Variacion v = ComparacionEntreno.variacionDeSesion(ResumenSesion.de(10, 8420, 16,
                ResumenSesion.SesionAnterior.de(9, "2026-10-15T18:00:00", 7943), Collections.emptyList()));

        assertNotNull(v);
        assertEquals(ComparacionEntreno.Tendencia.SUBE, v.tendencia);
        assertEquals(6, v.porcentaje);
    }

    @Test
    public void sin_sesion_anterior_la_ficha_no_lleva_variacion() {
        assertNull(ComparacionEntreno.variacionDeSesion(ResumenSesion.de(10, 8420, 16, null, Collections.emptyList())));
        assertNull(ComparacionEntreno.variacionDeSesion(null));
    }
}
