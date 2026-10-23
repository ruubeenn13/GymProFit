package es.pmdm.gymprofit.utils;

import java.util.List;

// ============================================================
// PeriodoProgreso — el selector «4 sem. · 8 sem. · Todo» de la gráfica de la ficha del ejercicio
// (GP-216, tablero 30). Las fechas de la progresión llegan en orden cronológico, así que el
// periodo es siempre «desde un punto hasta el final». Sin dependencias de Android.
// ============================================================
public final class PeriodoProgreso {

    private PeriodoProgreso() { }

    /**
     * Índice del primer punto que cae dentro de las últimas {@code semanas} semanas.
     *
     * @param fechas  fechas ISO en orden cronológico.
     * @param hoy     el día de hoy, ISO.
     * @param semanas 4, 8…; 0 o menos es «todo».
     * @return 0 si es todo; {@code fechas.size()} si ningún punto entra.
     */
    public static int primerIndice(List<String> fechas, String hoy, int semanas) {
        if (semanas <= 0) return 0;
        int limite = semanas * 7;
        for (int i = 0; i < fechas.size(); i++) {
            int dias = TiempoRelativo.diasEntre(fechas.get(i), hoy);
            // Una fecha que no se entiende o futura no puede ser anterior al periodo.
            if (dias < 0 || dias <= limite) return i;
        }
        return fechas.size();
    }
}
