package es.pmdm.gymprofit.utils;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import es.pmdm.gymprofit.R;

// ============================================================
// MarcaSerie — cómo se ve y qué dice el botón de marcar una serie (GP-252).
//
// Hecha y sin hacer no se distinguen solo por el color (DEC-019): sin hacer es un aro sin
// relleno, como en Apuntar entreno (bg_check_pendiente; la primera sin hacer de la sesión en vivo,
// en el color principal: bg_check_toca); hecha, el círculo relleno con borde y el check
// (bg_check_hecha). Ninguno se tiñe por código: el tinte de fondo pinta el óvalo entero y
// convertía el aro en un círculo relleno. TalkBack dice el estado («Hecha» o «Sin hacer») y el
// botón se nombra por lo que hace. Lo usan la sesión en vivo (item_envivo_serie) y Apuntar entreno.
// La sesión en vivo es Liquid Glass y usa los dibujos de cristal (fondoCristal).
// ============================================================
public final class MarcaSerie {

    private MarcaSerie() { }

    /** El fondo del botón: el círculo relleno si está hecha; si no, el aro (del color principal si toca). */
    @DrawableRes
    public static int fondo(boolean hecha, boolean toca) {
        if (hecha) return R.drawable.bg_check_hecha;
        return toca ? R.drawable.bg_check_toca : R.drawable.bg_check_pendiente;
    }

    /**
     * El fondo del botón sobre cristal (la Sesión en vivo, GP-285): las mismas formas con los
     * colores medidos ahí. La serie de un récord, el círculo dorado, que lleva dentro el trofeo.
     */
    @DrawableRes
    public static int fondoCristal(boolean hecha, boolean toca, boolean record) {
        if (record) return R.drawable.bg_check_cristal_record;
        if (hecha) return R.drawable.bg_check_cristal_hecha;
        return toca ? R.drawable.bg_check_cristal_toca : R.drawable.bg_check_cristal_pendiente;
    }

    /** El estado que dice TalkBack. */
    @StringRes
    public static int estado(boolean hecha) {
        return hecha ? R.string.envivo_hecha : R.string.envivo_sin_hacer;
    }

    /** El nombre del botón, con el número de la serie: «Apuntar la serie 2» o «Serie 2 apuntada». */
    @StringRes
    public static int nombre(boolean hecha) {
        return hecha ? R.string.apuntar_serie_apuntada_a11y : R.string.apuntar_apuntar_serie_a11y;
    }
}
