package es.pmdm.gymprofit.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// ============================================================
// Siglas — las dos letras de cada día en la tira de la semana de Entrenar y del programa
// (GP-216, tableros 14 y 31): «Torso A» es TA y «Empuje» es EM. Sin dependencias de Android.
// En un ciclo, dos rutinas distintas nunca comparten siglas (GP-232: en inglés «Push» y «Pull»
// daban PU las dos y la tira no decía qué tocaba cada día).
// ============================================================
public final class Siglas {

    private Siglas() { }

    /** Dos o más palabras dan la inicial de la primera y la de la última («Full body C» es FC, no FB: así las rutinas de un mismo programa no se confunden); una, sus dos primeras letras; sin nombre, un guion. */
    public static String de(@Nullable String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) return "—";
        String[] palabras = nombre.trim().split("[ \t]+");
        String s;
        if (palabras.length >= 2) {
            s = palabras[0].substring(0, 1) + palabras[palabras.length - 1].substring(0, 1);
        } else {
            s = palabras[0].length() >= 2 ? palabras[0].substring(0, 2) : palabras[0];
        }
        return s.toUpperCase(Locale.ROOT);
    }

    /**
     * Las siglas de las rutinas de un ciclo, por nombre: las de {@link #de}, salvo cuando dos
     * nombres distintos dan las mismas; entonces cada uno lleva su inicial y la primera letra en
     * que se distingue de los otros («Push» PS y «Pull» PL).
     *
     * @param nombres los del ciclo, con repeticiones y nulos si los hay.
     * @return siglas por nombre (también la del nulo, «—»).
     */
    @NonNull
    public static Map<String, String> deCiclo(@NonNull Collection<String> nombres) {
        List<String> distintos = new ArrayList<>(new LinkedHashSet<>(nombres));
        Map<String, List<String>> porSiglas = new HashMap<>();
        for (String n : distintos) {
            porSiglas.computeIfAbsent(de(n), k -> new ArrayList<>()).add(n);
        }
        Map<String, String> resultado = new HashMap<>();
        for (Map.Entry<String, List<String>> e : porSiglas.entrySet()) {
            List<String> grupo = e.getValue();
            for (String n : grupo) {
                resultado.put(n, grupo.size() == 1 || n == null ? e.getKey() : distinguir(n, grupo));
            }
        }
        return resultado;
    }

    // La inicial y la primera letra en que este nombre se separa de todos los del grupo; si es el
    // principio de otro, su última letra.
    private static String distinguir(@NonNull String nombre, @NonNull List<String> grupo) {
        String propio = letras(nombre);
        for (int k = 1; k < propio.length(); k++) {
            boolean unica = true;
            for (String otro : grupo) {
                if (otro == null || otro.equals(nombre)) continue;
                String o = letras(otro);
                if (k < o.length() && o.charAt(k) == propio.charAt(k)) {
                    unica = false;
                    break;
                }
            }
            if (unica) return ("" + propio.charAt(0) + propio.charAt(k)).toUpperCase(Locale.ROOT);
        }
        return de(nombre);
    }

    private static String letras(@NonNull String s) {
        return s.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }
}
