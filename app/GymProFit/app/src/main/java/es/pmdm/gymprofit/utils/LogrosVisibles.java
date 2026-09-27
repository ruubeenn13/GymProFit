package es.pmdm.gymprofit.utils;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import es.pmdm.gymprofit.model.logro.LogroProgreso;

// ============================================================
// LogrosVisibles — qué logros se enseñan (GP-114).
//
// OBJETIVO_CUMPLIDO y MAQUINA se consiguen completando objetivos personales, y la
// app no tiene pantalla para crearlos: enseñarlos es prometer algo que el usuario no
// puede hacer. No salen, ni en la lista ni en la cuenta de logros de Progreso,
// mientras no haya esa pantalla. Si se añade, esta lista se vacía.
// ============================================================
public final class LogrosVisibles {

    /** Logros que dependen de objetivos personales, que hoy no se pueden crear. */
    public static final Set<String> OCULTOS =
            Collections.unmodifiableSet(new HashSet<>(Arrays.asList("OBJETIVO_CUMPLIDO", "MAQUINA")));

    private LogrosVisibles() { }

    /** Los logros que se pueden conseguir hoy, en el orden en que llegan. */
    public static List<LogroProgreso> filtrar(@Nullable List<LogroProgreso> logros) {
        List<LogroProgreso> visibles = new ArrayList<>();
        if (logros == null) return visibles;
        for (LogroProgreso l : logros) {
            if (!OCULTOS.contains(l.getTipo())) visibles.add(l);
        }
        return visibles;
    }

    /** Cuántos de los visibles están conseguidos (la cifra de Progreso). */
    public static int conseguidos(@Nullable List<LogroProgreso> logros) {
        int n = 0;
        for (LogroProgreso l : filtrar(logros)) if (l.isConseguido()) n++;
        return n;
    }
}
