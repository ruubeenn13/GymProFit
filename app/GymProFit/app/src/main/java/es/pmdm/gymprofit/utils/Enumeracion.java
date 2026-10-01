package es.pmdm.gymprofit.utils;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.Locale;

// ============================================================
// Enumeracion — «a, b y c» (GP-103, «Tu plan»).
//
// Para las frases que se arman con una lista: los tres primeros ejercicios de una rutina
// o los días del programa. La conjunción la da el idioma («y», «and»); aquí solo se
// ponen las comas. Sin Android, para probarlo en la JVM.
// ============================================================
public final class Enumeracion {

    private Enumeracion() {}

    /**
     * Une los elementos con comas y la conjunción antes del último.
     *
     * @param partes      lo que se enumera; vacío da "".
     * @param conjuncion  « y » o « and », con sus espacios.
     */
    @NonNull
    public static String unir(@NonNull List<String> partes, @NonNull String conjuncion) {
        if (partes.isEmpty()) return "";
        if (partes.size() == 1) return partes.get(0);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < partes.size() - 1; i++) {
            if (i > 0) sb.append(", ");
            sb.append(partes.get(i));
        }
        return sb.append(conjuncion).append(partes.get(partes.size() - 1)).toString();
    }

    /**
     * Pasa a minúscula la primera letra, para lo que va en medio de una frase («Press de
     * banca» → «press de banca»). Deja igual lo que empieza por dos mayúsculas («TRX»).
     */
    @NonNull
    public static String enMedio(@NonNull String texto, @NonNull Locale idioma) {
        if (texto.length() < 2) return texto.toLowerCase(idioma);
        if (Character.isUpperCase(texto.charAt(1))) return texto;
        return texto.substring(0, 1).toLowerCase(idioma) + texto.substring(1);
    }

    /** La primera letra en mayúscula, para el principio de una frase. */
    @NonNull
    public static String alPrincipio(@NonNull String texto, @NonNull Locale idioma) {
        if (texto.isEmpty()) return texto;
        return texto.substring(0, 1).toUpperCase(idioma) + texto.substring(1);
    }
}
