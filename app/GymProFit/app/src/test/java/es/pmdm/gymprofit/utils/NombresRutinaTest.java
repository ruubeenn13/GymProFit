package es.pmdm.gymprofit.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Map;

import es.pmdm.gymprofit.model.sesion.SesionEntrenamiento;

// ============================================================
// NombresRutinaTest — el historial nombra cada sesión con lo que manda la API, también
// la de una rutina desactivada (lote 1.2.1; antes GP-113 con las plantillas).
// ============================================================
public class NombresRutinaTest {

    private static SesionEntrenamiento sesion(Integer rutinaId, String nombre) {
        SesionEntrenamiento s = new SesionEntrenamiento();
        s.setRutinaId(rutinaId);
        s.setRutinaNombre(nombre);
        return s;
    }

    @Test
    public void cada_sesion_lleva_el_nombre_de_su_rutina_aunque_este_desactivada() {
        Map<Integer, String> nombres = NombresRutina.deSesiones(Arrays.asList(
                sesion(40, "Mi pierna"),
                sesion(7, "Torso A"),       // de un programa ya dejado: rutina desactivada
                sesion(null, null)));       // entrenamiento libre

        assertEquals("Mi pierna", nombres.get(40));
        assertEquals("Torso A", nombres.get(7));
        assertNull(nombres.get(99));
    }

    @Test
    public void sin_nombre_o_sin_lista_no_rompe() {
        assertTrue(NombresRutina.deSesiones(null).isEmpty());
        assertTrue(NombresRutina.deSesiones(Arrays.asList(sesion(3, ""), sesion(4, null))).isEmpty());
    }
}
