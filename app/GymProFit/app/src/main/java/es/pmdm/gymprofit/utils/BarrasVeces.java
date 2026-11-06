package es.pmdm.gymprofit.utils;

import androidx.annotation.NonNull;

import java.util.List;

import es.pmdm.gymprofit.model.sesion.VezRutina;

// ============================================================
// BarrasVeces — alto y color de las barras de «tus últimas veces» (GP-232).
//
// Una barra por sesión de la rutina, hasta 6, de la más antigua a esta, que va a la derecha.
// Empiezan en 0: la más alta llena el alto y las demás, en proporción, así que una subida del
// 3 % se ve como lo que es. La de esta sesión (en una rutina, la última vez) va en naranja; la
// que batió un récord, en dorado, también si es esta; las demás, en el color de borde. Sin
// dependencias de Android, para probarla en la JVM.
// ============================================================
public final class BarrasVeces {

    /** Cuántas barras como mucho. */
    public static final int MAXIMO = 6;

    /** El color de una barra. */
    public enum Color { ESTA, ORO, RESTO }

    private BarrasVeces() { }

    /** Las últimas {@link #MAXIMO} veces, si llegan más. */
    @NonNull
    public static List<VezRutina> recortar(@NonNull List<VezRutina> veces) {
        return veces.size() > MAXIMO ? veces.subList(veces.size() - MAXIMO, veces.size()) : veces;
    }

    /** El alto de cada barra entre 0 y 1, desde 0 y contra la más alta. */
    @NonNull
    public static float[] alturas(@NonNull List<VezRutina> veces) {
        float[] a = new float[veces.size()];
        double max = 0;
        for (VezRutina v : veces) max = Math.max(max, v.getKilos());
        if (max <= 0) return a;
        for (int i = 0; i < a.length; i++) a[i] = (float) (Math.max(0, veces.get(i).getKilos()) / max);
        return a;
    }

    /** El color de cada barra: dorado si batió récord; si no, naranja la última y borde las demás. */
    @NonNull
    public static Color[] colores(@NonNull List<VezRutina> veces) {
        Color[] c = new Color[veces.size()];
        for (int i = 0; i < c.length; i++) {
            if (veces.get(i).isRecord()) c[i] = Color.ORO;
            else c[i] = i == c.length - 1 ? Color.ESTA : Color.RESTO;
        }
        return c;
    }
}
