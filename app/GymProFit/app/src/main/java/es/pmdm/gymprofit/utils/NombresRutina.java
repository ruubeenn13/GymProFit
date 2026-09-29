package es.pmdm.gymprofit.utils;

import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import es.pmdm.gymprofit.model.sesion.SesionEntrenamiento;

// ============================================================
// NombresRutina — de id de rutina a su nombre, para el historial (GP-113, lote 1.2.1).
//
// El historial sacaba los nombres de las rutinas activas (propias y plantillas), así
// que una sesión de una rutina borrada salía como «Sin rutina asociada». Con programas
// pasaría con todas las suyas al cambiar el tiempo o dejarlo, porque sus rutinas se
// desactivan. Desde la 1.2.1 la API manda el nombre en cada sesión (rutinaNombre),
// también con la rutina desactivada, y el mapa sale de ahí.
// ============================================================
public final class NombresRutina {

    private NombresRutina() { }

    /** Id de rutina → nombre, sacado de las propias sesiones. Las sin nombre se saltan. */
    public static Map<Integer, String> deSesiones(@Nullable List<SesionEntrenamiento> sesiones) {
        Map<Integer, String> nombres = new HashMap<>();
        if (sesiones == null) return nombres;
        for (SesionEntrenamiento s : sesiones) {
            String nombre = s.getRutinaNombre();
            if (s.getRutinaId() != null && nombre != null && !nombre.isEmpty()) {
                nombres.put(s.getRutinaId(), nombre);
            }
        }
        return nombres;
    }
}
