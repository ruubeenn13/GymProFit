package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import es.pmdm.gymprofit.model.rutina.Rutina;

// ============================================================
// NombresRutinaTest — GP-113: una sesión hecha con una plantilla dice el nombre de
// la plantilla en el historial, no «Sin rutina asociada».
// ============================================================
public class NombresRutinaTest {

    private static Rutina rutina(int id, String nombre) {
        Rutina r = new Rutina();
        r.setId(id);
        r.setNombre(nombre);
        return r;
    }

    @Test
    public void la_plantilla_tambien_tiene_nombre() {
        Map<Integer, String> nombres = NombresRutina.de(
                Collections.singletonList(rutina(40, "Mi pierna")),
                Collections.singletonList(rutina(2, "Full Body")));

        assertEquals("Mi pierna", nombres.get(40));
        assertEquals("Full Body", nombres.get(2));
        assertNull(nombres.get(99));
    }

    @Test
    public void una_lista_que_no_llego_no_rompe() {
        assertEquals("Full Body", NombresRutina.de(null, Collections.singletonList(rutina(2, "Full Body"))).get(2));
    }
}
