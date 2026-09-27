package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import es.pmdm.gymprofit.model.rutina.Rutina;
import es.pmdm.gymprofit.model.sesion.SesionEntrenamiento;

// ============================================================
// HoyTocaTest — GP-105: la regla de «Hoy toca».
//
// De las rutinas propias activas, la que más tiempo lleva sin hacerse; primero las
// que no se han hecho nunca y, a igualdad, la primera de la lista.
// ============================================================
public class HoyTocaTest {

    private static Rutina rutina(int id) {
        Rutina r = new Rutina();
        r.setId(id);
        r.setNombre("R" + id);
        return r;
    }

    private static SesionEntrenamiento sesion(Integer rutinaId, String inicio) {
        SesionEntrenamiento s = new SesionEntrenamiento();
        s.setRutinaId(rutinaId);
        s.setFechaInicio(inicio);
        return s;
    }

    @Test
    public void sin_rutinas_propias_no_hay_eleccion() {
        assertNull(HoyToca.elegir(Collections.emptyList(), Collections.emptyList()));
        assertNull(HoyToca.elegir(null, null));
    }

    @Test
    public void la_que_mas_tiempo_lleva_sin_hacerse() {
        List<SesionEntrenamiento> s = Arrays.asList(
                sesion(1, "2026-09-25T18:00:00"),
                sesion(2, "2026-09-20T18:00:00"),
                sesion(3, "2026-09-23T18:00:00"),
                sesion(2, "2026-09-10T18:00:00"));   // una más antigua no cuenta: manda la última

        HoyToca.Eleccion e = HoyToca.elegir(Arrays.asList(rutina(1), rutina(2), rutina(3)), s);

        assertEquals(2, e.rutina.getId());
        assertEquals("2026-09-20", e.ultimoDia);
    }

    @Test
    public void primero_las_que_no_se_han_hecho_nunca() {
        List<SesionEntrenamiento> s = Collections.singletonList(sesion(1, "2025-01-01T10:00:00"));

        HoyToca.Eleccion e = HoyToca.elegir(Arrays.asList(rutina(1), rutina(2), rutina(3)), s);

        assertEquals(2, e.rutina.getId());
        assertNull(e.ultimoDia);
    }

    @Test
    public void a_igualdad_la_primera_de_la_lista() {
        // Ninguna hecha: la primera.
        assertEquals(5, HoyToca.elegir(Arrays.asList(rutina(5), rutina(6)), null).rutina.getId());

        // Las dos hechas el mismo día, a horas distintas: cuenta el día, no la hora.
        List<SesionEntrenamiento> s = Arrays.asList(
                sesion(6, "2026-09-20T08:00:00"),
                sesion(5, "2026-09-20T20:00:00"));
        assertEquals(5, HoyToca.elegir(Arrays.asList(rutina(5), rutina(6)), s).rutina.getId());
    }

    @Test
    public void el_entrenamiento_libre_y_las_rutinas_ajenas_no_cuentan() {
        List<SesionEntrenamiento> s = new ArrayList<>(Arrays.asList(
                sesion(null, "2026-09-26T10:00:00"),
                sesion(99, "2026-09-26T10:00:00"),
                sesion(1, "2026-09-26T10:00:00")));

        HoyToca.Eleccion e = HoyToca.elegir(Arrays.asList(rutina(1), rutina(2)), s);

        assertEquals(2, e.rutina.getId());
    }

    @Test
    public void la_sesion_de_hoy_es_la_mas_reciente_del_dia() {
        List<SesionEntrenamiento> s = Arrays.asList(
                sesion(1, "2026-09-26T23:00:00"),
                sesion(2, "2026-09-27T09:00:00"),
                sesion(3, "2026-09-27T19:30:00"));

        assertEquals(Integer.valueOf(3), HoyToca.sesionDeHoy(s, "2026-09-27").getRutinaId());
        assertNull(HoyToca.sesionDeHoy(s, "2026-09-28"));
        assertNull(HoyToca.sesionDeHoy(null, "2026-09-28"));
    }
}
