package es.pmdm.gymprofit.utils;

import androidx.annotation.Nullable;

import java.util.Locale;

// ============================================================
// Siglas — las dos letras de cada día en la tira de la semana de Entrenar y del programa
// (GP-216, tableros 14 y 31): «Torso A» es TA y «Empuje» es EM. Sin dependencias de Android.
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
}
