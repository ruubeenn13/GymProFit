package es.pmdm.gymprofit.utils;

import androidx.annotation.Nullable;

import java.util.List;

// ============================================================
// ProximaVez — la pista «La próxima vez…» de la ficha del ejercicio (GP-216, tablero 30).
//
// Solo existe una regla en los programas (su texto «De qué va»): cuando todas las series
// llegan al máximo de repeticiones de la pauta, se sube el peso lo mínimo. No se inventa
// cuánto: la app no tiene un paso de carga por ejercicio. Sin máximo conocido (el ejercicio
// no se abre desde una rutina de programa) no hay pista. Sin dependencias de Android.
// ============================================================
public final class ProximaVez {

    private ProximaVez() { }

    /**
     * @param repeticiones las repeticiones de cada serie de la última vez.
     * @param maximo       el máximo del rango de la pauta, o null si no se conoce.
     * @return true si todas las series llegaron al máximo y toca subir el peso.
     */
    public static boolean subirPeso(@Nullable List<Integer> repeticiones, @Nullable Integer maximo) {
        if (maximo == null || maximo <= 0 || repeticiones == null || repeticiones.isEmpty()) return false;
        for (Integer r : repeticiones) {
            if (r == null || r < maximo) return false;
        }
        return true;
    }
}
