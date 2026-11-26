package es.pmdm.gymprofit.ui.widget;

/**
 * El texto de la barra de la sesión en curso cuando no cabe en una línea (GP-285).
 * <p>
 * La barra es una cápsula más estrecha que la pantalla: en inglés o con letra grande,
 * «Sesión en curso · Torso A · 23:14» no cabe. Dejar que el sistema parta dejaba el reloj solo
 * en la segunda línea o un «·» colgando al final de una o al principio de la otra.
 */
public final class TextoBarraSesion {

    /** El separador de las cadenas de la barra (envivo_barra…): espacio, punto medio, espacio. */
    private static final String SEPARADOR = " \u00B7 ";

    private TextoBarraSesion() { }

    /**
     * Parte el texto por su primer separador, que se quita: arriba «Sesión en curso» y debajo
     * «Torso A · 23:14». Sin separador, el texto vuelve como vino.
     */
    public static String enDosLineas(String texto) {
        int corte = texto.indexOf(SEPARADOR);
        if (corte < 0) return texto;
        return texto.substring(0, corte) + '\n' + texto.substring(corte + SEPARADOR.length());
    }
}
