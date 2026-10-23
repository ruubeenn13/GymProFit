package es.pmdm.gymprofit.utils;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// ============================================================
// UltimasVeces — «Tus últimas veces» de la ficha del ejercicio (GP-216, tablero 30): una
// tarjeta por sesión, la más reciente primero, con el total de kilos, la diferencia con la
// sesión anterior y la serie que más pesó. El dorado solo va en la sesión que tiene el récord
// vigente del ejercicio (/records): una sesión que no lo batió no se pinta de oro.
// Sin dependencias de Android, para probarla en la JVM.
// ============================================================
public final class UltimasVeces {

    private UltimasVeces() { }

    /** Una serie: peso 0 si es de peso corporal. */
    public static final class Serie {
        public final double peso;
        public final int repeticiones;

        public Serie(double peso, int repeticiones) {
            this.peso = peso;
            this.repeticiones = repeticiones;
        }
    }

    /** Lo que llega de la API para una sesión: su id, su inicio ISO y las series del ejercicio. */
    public static final class Sesion {
        public final int sesionId;
        public final String fecha;
        public final List<Serie> series;

        public Sesion(int sesionId, String fecha, List<Serie> series) {
            this.sesionId = sesionId;
            this.fecha = fecha;
            this.series = series;
        }
    }

    /** Lo que se pinta de cada sesión. */
    public static final class Tarjeta {
        public final int sesionId;
        public final String fecha;
        public final List<Serie> series;
        public final double totalKg;
        /** Kilos de más o de menos que la sesión anterior; null si no hay anterior. */
        @Nullable public final Double diferenciaKg;
        /** Índice de la serie que más pesó; las empatadas, la de más repeticiones. */
        public final int mejorIndice;
        /** Si esta sesión tiene el récord vigente del ejercicio. */
        public final boolean esRecord;

        Tarjeta(Sesion s, double totalKg, @Nullable Double diferenciaKg, int mejorIndice, boolean esRecord) {
            this.sesionId = s.sesionId;
            this.fecha = s.fecha;
            this.series = s.series;
            this.totalKg = totalKg;
            this.diferenciaKg = diferenciaKg;
            this.mejorIndice = mejorIndice;
            this.esRecord = esRecord;
        }
    }

    /**
     * @param sesiones         las sesiones en que se hizo el ejercicio, en cualquier orden.
     * @param sesionDelRecord  la sesión del récord vigente del ejercicio, o null.
     * @param limite           cuántas tarjetas como máximo.
     */
    public static List<Tarjeta> de(@Nullable List<Sesion> sesiones, @Nullable Integer sesionDelRecord, int limite) {
        List<Sesion> validas = new ArrayList<>();
        if (sesiones != null) {
            for (Sesion s : sesiones) {
                if (s.series != null && !s.series.isEmpty()) validas.add(s);
            }
        }
        // Las fechas ISO se ordenan como texto; de la más antigua a la más reciente para poder
        // restar con la anterior, y al final se da la vuelta.
        Collections.sort(validas, (a, b) -> String.valueOf(a.fecha).compareTo(String.valueOf(b.fecha)));

        List<Tarjeta> todas = new ArrayList<>();
        Double anterior = null;
        for (Sesion s : validas) {
            double total = 0;
            int mejor = 0;
            for (int i = 0; i < s.series.size(); i++) {
                Serie x = s.series.get(i);
                total += x.peso * x.repeticiones;
                Serie m = s.series.get(mejor);
                if (x.peso > m.peso || (x.peso == m.peso && x.repeticiones > m.repeticiones)) mejor = i;
            }
            Double dif = anterior == null ? null : total - anterior;
            boolean record = sesionDelRecord != null && sesionDelRecord == s.sesionId;
            todas.add(new Tarjeta(s, total, dif, mejor, record));
            anterior = total;
        }
        Collections.reverse(todas);
        return todas.size() > limite ? new ArrayList<>(todas.subList(0, Math.max(limite, 0))) : todas;
    }
}
