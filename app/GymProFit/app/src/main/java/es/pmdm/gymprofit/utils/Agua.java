package es.pmdm.gymprofit.utils;

import androidx.annotation.NonNull;

import java.text.NumberFormat;
import java.util.Locale;

// ============================================================
// Agua — el agua del día, a vasos (lote 1.6.8, GP-166, decisión 11 del lienzo, tablero 1)
//
// Se guarda en mililitros (la API, por día); se enseña a vasos hacia el objetivo de agua
// del plan. El vaso es de 250 ml y se cambia en el perfil, de una lista cerrada; se guarda
// en el móvil (PreferencesManager). Tocar el último vaso lleno lo quita; tocar uno vacío
// añade uno. Sin recordatorios.
// ============================================================
public final class Agua {

    /** El vaso de siempre. */
    public static final int VASO_POR_DEFECTO = 250;
    /** Los tamaños de vaso que se pueden elegir, en ml. */
    public static final int[] VASOS = {200, 250, 330, 500};
    /** Como mucho, tantos vasos en la fila. */
    public static final int MAXIMO_VASOS = 20;
    /** Lo más que acepta la API en un día. */
    public static final int MAXIMO_ML = 20000;

    /** Lo que hace tocar un vaso. */
    public enum Toque { ANADIR, QUITAR, NADA }

    private Agua() {
    }

    /** Vasos hasta el objetivo del plan, redondeando hacia arriba; 0 sin objetivo. */
    public static int vasosObjetivo(double litros, int vaso) {
        if (litros <= 0 || vaso <= 0) return 0;
        return (int) Math.ceil(Math.round(litros * 1000) / (double) vaso);
    }

    /** Vasos llenos, al vaso más cercano: el vaso puede cambiar a mitad de día. */
    public static int vasosLlenos(int ml, int vaso) {
        if (ml <= 0 || vaso <= 0) return 0;
        return (ml + vaso / 2) / vaso;
    }

    /**
     * Cuántos vasos se pintan: los del objetivo, o los bebidos si son más. Sin objetivo,
     * los bebidos y uno vacío para el siguiente.
     */
    public static int vasosAPintar(int llenos, int objetivo) {
        int n = objetivo > 0 ? Math.max(objetivo, llenos) : llenos + 1;
        return Math.min(MAXIMO_VASOS, n);
    }

    /**
     * Qué hace tocar el vaso {@code i} (desde 0) con {@code llenos} vasos llenos: el último
     * lleno lo quita, uno vacío añade uno, y otro lleno no hace nada.
     */
    @NonNull
    public static Toque toque(int i, int llenos) {
        if (i == llenos - 1) return Toque.QUITAR;
        if (i >= llenos) return Toque.ANADIR;
        return Toque.NADA;
    }

    /** Un vaso más, sin pasar del tope de la API. */
    public static int mas(int ml, int vaso) {
        return Math.min(MAXIMO_ML, Math.max(0, ml) + vaso);
    }

    /** Un vaso menos, sin bajar de 0. */
    public static int menos(int ml, int vaso) {
        return Math.max(0, ml - vaso);
    }

    /** ¿Es uno de los vasos que se pueden elegir? */
    public static boolean vasoValido(int ml) {
        for (int v : VASOS) if (v == ml) return true;
        return false;
    }

    /** El vaso guardado, o el de siempre si lo guardado no vale. */
    public static int vasoOPorDefecto(int ml) {
        return vasoValido(ml) ? ml : VASO_POR_DEFECTO;
    }

    /** Los litros de unos mililitros, con hasta dos decimales y sin ceros de más («1,25», «1,5», «2»). */
    @NonNull
    public static String litros(int ml, @NonNull Locale idioma) {
        return formato(idioma).format(ml / 1000.0);
    }

    /** Los litros del objetivo del plan, igual que {@link #litros(int, Locale)}. */
    @NonNull
    public static String litrosObjetivo(double litros, @NonNull Locale idioma) {
        return formato(idioma).format(litros);
    }

    private static NumberFormat formato(Locale idioma) {
        NumberFormat nf = NumberFormat.getNumberInstance(idioma);
        nf.setMinimumFractionDigits(0);
        nf.setMaximumFractionDigits(2);
        nf.setGroupingUsed(false);
        return nf;
    }
}
