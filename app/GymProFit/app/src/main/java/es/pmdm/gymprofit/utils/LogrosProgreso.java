package es.pmdm.gymprofit.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import es.pmdm.gymprofit.model.logro.LogroProgreso;

// ============================================================
// LogrosProgreso — «Logros» de Progreso (GP-216, lote 4, tablero 16): los dos últimos conseguidos
// y los dos que están más cerca de conseguirse.
//
// Salen del catálogo real (GET /logros/progreso, el enum TipoLogro de la API), sin los que hoy no
// se pueden conseguir (LogrosVisibles). El tablero dibuja logros de racha y de récords que la API
// no tiene: no se inventan (hueco 14 de DESIGN.md). Un logro sin ningún avance no está «cerca»:
// «0 de 30» sería un cero en la cara de una cuenta nueva, así que no sale.
// Sin dependencias de Android, para probarla en la JVM.
// ============================================================
public final class LogrosProgreso {

    /** Cuántos de cada grupo se enseñan. */
    public static final int POR_GRUPO = 2;

    private LogrosProgreso() { }

    /**
     * Los que se enseñan: primero los conseguidos (el más reciente delante) y luego los más cerca.
     * Vacía si no hay ni uno conseguido ni uno empezado.
     */
    @NonNull
    public static List<LogroProgreso> elegir(@Nullable List<LogroProgreso> todos) {
        List<LogroProgreso> conseguidos = new ArrayList<>();
        List<LogroProgreso> empezados = new ArrayList<>();
        for (LogroProgreso l : LogrosVisibles.filtrar(todos)) {
            if (l.isConseguido()) conseguidos.add(l);
            else if (l.getProgreso() > 0 && l.getUmbral() > 0) empezados.add(l);
        }
        // ISO-8601 se ordena como texto; sin fecha, al final.
        Collections.sort(conseguidos, (a, b) -> {
            String fa = a.getFechaObtenido(), fb = b.getFechaObtenido();
            if (fa == null) return fb == null ? 0 : 1;
            if (fb == null) return -1;
            return fb.compareTo(fa);
        });
        // El que lleva más parte hecha; a igual parte, el de umbral más bajo (se consigue antes).
        Collections.sort(empezados, (a, b) -> {
            int c = Double.compare(parte(b), parte(a));
            return c != 0 ? c : Integer.compare(a.getUmbral(), b.getUmbral());
        });
        List<LogroProgreso> r = new ArrayList<>(conseguidos.subList(0, Math.min(POR_GRUPO, conseguidos.size())));
        r.addAll(empezados.subList(0, Math.min(POR_GRUPO, empezados.size())));
        return r;
    }

    private static double parte(LogroProgreso l) {
        return (double) l.getProgreso() / l.getUmbral();
    }
}
