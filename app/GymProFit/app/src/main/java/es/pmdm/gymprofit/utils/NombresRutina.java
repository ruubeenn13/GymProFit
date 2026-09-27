package es.pmdm.gymprofit.utils;

import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import es.pmdm.gymprofit.model.rutina.Rutina;

// ============================================================
// NombresRutina — de id de rutina a su nombre, para el historial (GP-113).
//
// El historial solo conocía los nombres de las rutinas propias, así que una sesión
// hecha con una plantilla de GymProFit salía como «Sin rutina asociada». Ahora el
// mapa junta las propias y las plantillas.
// ============================================================
public final class NombresRutina {

    private NombresRutina() { }

    /** Junta las listas en un mapa id → nombre. Las listas null se saltan. */
    @SafeVarargs
    public static Map<Integer, String> de(@Nullable List<Rutina>... listas) {
        Map<Integer, String> nombres = new HashMap<>();
        for (List<Rutina> l : listas) {
            if (l == null) continue;
            for (Rutina r : l) nombres.put(r.getId(), r.getNombre());
        }
        return nombres;
    }
}
