package es.pmdm.gymprofit.utils;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import es.pmdm.gymprofit.R;

// ============================================================
// Caras — «¿Qué tal fue?» de Apuntar entreno (GP-241, punto 3): cinco caras con nombre que
// valen de 1 a 5, la valoración de la sesión (GP-070). Sin tocar ninguna, la valoración es
// NULL y no se manda (GP-077): un valor por defecto falsearía las medias.
//
// Tocar la marcada la quita: con el dedo, un toque por error se deshace igual que se hizo.
// ============================================================
public final class Caras {

    /** Cuántas caras hay; la valoración va de 1 a {@code NUMERO}, como el @Max de la API. */
    public static final int NUMERO = Valoracion.MAXIMO;

    private static final int[] NOMBRES = {R.string.cara_fatal, R.string.cara_flojo, R.string.cara_normal,
            R.string.cara_bien, R.string.cara_genial};

    private static final int[] ICONOS = {R.drawable.ic_ms_sentiment_very_dissatisfied,
            R.drawable.ic_ms_sentiment_dissatisfied, R.drawable.ic_ms_sentiment_neutral,
            R.drawable.ic_ms_sentiment_satisfied, R.drawable.ic_ms_sentiment_very_satisfied};

    private Caras() { }

    /** Si es una valoración que se puede mandar: de 1 a 5. */
    public static boolean valida(@Nullable Integer valor) {
        return valor != null && valor >= 1 && valor <= NUMERO;
    }

    /** El nombre de una valoración («Bien» para el 4), o 0 si no hay ninguna. */
    @StringRes
    public static int nombre(@Nullable Integer valor) {
        return valida(valor) ? NOMBRES[valor - 1] : 0;
    }

    /** El icono de la cara de una valoración, de 1 a 5. */
    @DrawableRes
    public static int icono(int valor) {
        return ICONOS[valor - 1];
    }

    /**
     * La valoración tras tocar una cara.
     *
     * @param actual  la que había, o null.
     * @param tocada  la cara tocada, de 1 a 5.
     * @return la tocada; o null si se tocó la que ya estaba marcada.
     */
    @Nullable
    public static Integer alTocar(@Nullable Integer actual, int tocada) {
        if (!valida(tocada)) return valida(actual) ? actual : null;
        return actual != null && actual == tocada ? null : tocada;
    }
}
