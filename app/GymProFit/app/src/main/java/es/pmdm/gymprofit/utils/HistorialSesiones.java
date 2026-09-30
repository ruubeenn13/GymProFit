package es.pmdm.gymprofit.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import es.pmdm.gymprofit.model.sesion.SesionEntrenamiento;

// ============================================================
// HistorialSesiones — el orden del historial (GP-141), en lógica pura.
//
// De la más reciente a la más antigua por fecha de inicio y, a igual fecha, la de id
// mayor (la guardada después) primero. La API ya las manda así, pero la lista no se
// fía del orden en que llega: con «Apuntar un entrenamiento hecho» el orden de guardado
// no es el de las fechas, y es fácil que una consulta nueva vuelva a perderlo.
// Las fechas llegan como «yyyy-MM-ddTHH:mm:ss», que como texto ordenan igual que el
// calendario. Una sin fecha (no debería haberla) va al final.
// ============================================================
public final class HistorialSesiones {

    private HistorialSesiones() { }

    private static final Comparator<SesionEntrenamiento> MAS_RECIENTE_PRIMERO = (a, b) -> {
        String fa = a.getFechaInicio(), fb = b.getFechaInicio();
        if (fa == null || fb == null) {
            if (fa != null) return -1;
            if (fb != null) return 1;
        } else {
            int porFecha = fb.compareTo(fa);
            if (porFecha != 0) return porFecha;
        }
        return Integer.compare(b.getId(), a.getId());
    };

    /**
     * Copia ordenada para el historial; la lista recibida no se toca.
     *
     * @param sesiones las sesiones en el orden que sea, o null.
     * @return una lista nueva, de la más reciente a la más antigua.
     */
    @NonNull
    public static List<SesionEntrenamiento> masRecientePrimero(@Nullable List<SesionEntrenamiento> sesiones) {
        List<SesionEntrenamiento> copia = sesiones == null ? new ArrayList<>() : new ArrayList<>(sesiones);
        Collections.sort(copia, MAS_RECIENTE_PRIMERO);
        return copia;
    }
}
