package es.pmdm.gymprofit.utils;

import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import es.pmdm.gymprofit.model.rutina.Rutina;
import es.pmdm.gymprofit.model.sesion.SesionEntrenamiento;

// ============================================================
// HoyToca — qué rutina toca hoy (GP-105), en lógica pura para poder probarla.
//
// De las rutinas propias activas, la que más tiempo lleva sin hacerse. Las que no se
// han hecho nunca van primero; a igualdad, manda el orden de la lista (el de «Mis
// rutinas»). Una sesión cuenta como hecha el día en que empieza.
//
// Las fechas llegan como las da la API, «yyyy-MM-ddTHH:mm:ss»; solo importa el día,
// y los días se comparan como texto ISO, que ordena igual que el calendario.
// ============================================================
public final class HoyToca {

    private HoyToca() { }

    /** Lo que se enseña en la tarjeta: la rutina y cuándo se hizo por última vez. */
    public static final class Eleccion {
        public final Rutina rutina;
        /** Día de la última vez («yyyy-MM-dd»), o {@code null} si no se ha hecho nunca. */
        @Nullable public final String ultimoDia;

        Eleccion(Rutina rutina, @Nullable String ultimoDia) {
            this.rutina = rutina;
            this.ultimoDia = ultimoDia;
        }
    }

    /**
     * Elige la rutina de hoy.
     *
     * @param propias  rutinas propias activas, en el orden de la lista.
     * @param sesiones sesiones del usuario, en cualquier orden.
     * @return la elección, o {@code null} si no hay rutinas propias.
     */
    @Nullable
    public static Eleccion elegir(@Nullable List<Rutina> propias,
                                  @Nullable List<SesionEntrenamiento> sesiones) {
        if (propias == null || propias.isEmpty()) return null;

        Map<Integer, String> ultimaVez = ultimoDiaPorRutina(sesiones);

        Rutina elegida = null;
        String diaElegida = null;
        for (Rutina r : propias) {
            String dia = ultimaVez.get(r.getId());
            if (elegida == null) {
                elegida = r;
                diaElegida = dia;
                continue;
            }
            // Nunca hecha gana a hecha; entre hechas, la más antigua. El empate se
            // queda con la que ya estaba, que va antes en la lista.
            if (diaElegida == null) continue;
            if (dia == null || dia.compareTo(diaElegida) < 0) {
                elegida = r;
                diaElegida = dia;
            }
        }
        return new Eleccion(elegida, diaElegida);
    }

    /**
     * La sesión más reciente de hoy, si la hay: la tarjeta pasa a «Hecho hoy».
     *
     * @param hoy día de hoy, «yyyy-MM-dd».
     */
    @Nullable
    public static SesionEntrenamiento sesionDeHoy(@Nullable List<SesionEntrenamiento> sesiones, String hoy) {
        if (sesiones == null) return null;
        SesionEntrenamiento ultima = null;
        for (SesionEntrenamiento s : sesiones) {
            String inicio = s.getFechaInicio();
            if (inicio == null || !hoy.equals(dia(inicio))) continue;
            if (ultima == null || inicio.compareTo(ultima.getFechaInicio()) > 0) ultima = s;
        }
        return ultima;
    }

    // Último día en que se empezó una sesión de cada rutina.
    private static Map<Integer, String> ultimoDiaPorRutina(@Nullable List<SesionEntrenamiento> sesiones) {
        Map<Integer, String> ultimaVez = new HashMap<>();
        if (sesiones == null) return ultimaVez;
        for (SesionEntrenamiento s : sesiones) {
            if (s.getRutinaId() == null || s.getFechaInicio() == null) continue;
            String dia = dia(s.getFechaInicio());
            String previo = ultimaVez.get(s.getRutinaId());
            if (previo == null || dia.compareTo(previo) > 0) ultimaVez.put(s.getRutinaId(), dia);
        }
        return ultimaVez;
    }

    // La parte de día de una fecha ISO.
    static String dia(String iso) {
        return iso.length() >= 10 ? iso.substring(0, 10) : iso;
    }
}
