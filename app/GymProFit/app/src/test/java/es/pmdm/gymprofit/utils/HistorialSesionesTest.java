package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import es.pmdm.gymprofit.model.sesion.SesionEntrenamiento;

// ============================================================
// HistorialSesionesTest — el historial sale por fecha (GP-141), sin depender del
// orden en que llegue de la API: de la más reciente a la más antigua por fecha de
// inicio y, a igual fecha, la de id mayor (la guardada después) primero.
// ============================================================
public class HistorialSesionesTest {

    private static SesionEntrenamiento sesion(int id, String inicio) {
        SesionEntrenamiento s = new SesionEntrenamiento();
        s.setId(id);
        s.setFechaInicio(inicio);
        return s;
    }

    private static List<Integer> ids(List<SesionEntrenamiento> l) {
        List<Integer> ids = new ArrayList<>();
        for (SesionEntrenamiento s : l) ids.add(s.getId());
        return ids;
    }

    @Test
    public void una_de_ayer_guardada_despues_sale_debajo_de_la_de_hoy() {
        List<SesionEntrenamiento> guardadas = Arrays.asList(
                sesion(1, "2026-09-30T18:00:00"),
                sesion(2, "2026-09-29T18:00:00"),
                sesion(3, "2026-09-30T08:00:00"));

        assertEquals(Arrays.asList(1, 3, 2), ids(HistorialSesiones.masRecientePrimero(guardadas)));
    }

    @Test
    public void a_igual_fecha_la_guardada_despues_primero_en_cualquier_orden() {
        List<SesionEntrenamiento> a = Arrays.asList(
                sesion(5, "2026-09-30T18:00:00"), sesion(9, "2026-09-30T18:00:00"));
        List<SesionEntrenamiento> b = new ArrayList<>(a);
        Collections.reverse(b);

        assertEquals(Arrays.asList(9, 5), ids(HistorialSesiones.masRecientePrimero(a)));
        assertEquals(Arrays.asList(9, 5), ids(HistorialSesiones.masRecientePrimero(b)));
    }

    @Test
    public void sin_fecha_va_al_final_y_no_toca_la_lista_de_entrada() {
        List<SesionEntrenamiento> l = Arrays.asList(sesion(1, null), sesion(2, "2026-09-01T10:00:00"));

        assertEquals(Arrays.asList(2, 1), ids(HistorialSesiones.masRecientePrimero(l)));
        assertEquals("la de entrada no se reordena", Integer.valueOf(1), Integer.valueOf(l.get(0).getId()));
        assertEquals(0, HistorialSesiones.masRecientePrimero(null).size());
    }
}
