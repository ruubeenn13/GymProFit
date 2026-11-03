package es.pmdm.gymprofit.utils;

import androidx.annotation.NonNull;

import java.text.NumberFormat;
import java.util.Locale;

// ============================================================
// NombreMedida — lo que TalkBack dice de una fila de Medidas (GP-227): «Peso, 79,2 kilos».
//
// La fila enseña «79,2 kg», que TalkBack lee letra a letra; su nombre dice la unidad entera. El
// número va con un decimal como mucho y en el idioma de la app («79,2» / «79.2»). La cantidad que
// elige singular o plural es 1 solo para un 1 justo: «1 kilo», «1,5 kilos», «0,5 kilos».
// Sin dependencias de Android, para probarla en la JVM.
// ============================================================
public final class NombreMedida {

    private NombreMedida() { }

    /** El número como se dice: un decimal como mucho, sin «,0». */
    @NonNull
    public static String numero(double valor, @NonNull Locale idioma) {
        NumberFormat nf = NumberFormat.getNumberInstance(idioma);
        nf.setMinimumFractionDigits(0);
        nf.setMaximumFractionDigits(1);
        return nf.format(valor);
    }

    /** La cantidad para {@code getQuantityString}: 1 si es un uno justo; si no, una que da el plural. */
    public static int cantidad(double valor) {
        return Math.round(valor * 10) == 10 ? 1 : 2;
    }
}
