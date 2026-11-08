package es.pmdm.gymprofit.utils;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import es.pmdm.gymprofit.R;

// ============================================================
// MarcaSerie — cómo se ve y qué dice el botón de marcar una serie (GP-252).
//
// Hecha y sin hacer no se distinguen solo por el color (DEC-019): sin hacer es un aro sin
// relleno, como en Apuntar entreno (bg_check_pendiente); hecha, el círculo relleno con el check
// (bg_check, teñido por código). TalkBack dice el estado («Hecha» o «Sin hacer»). Lo usan la
// sesión en vivo (item_envivo_serie) y Apuntar entreno.
// ============================================================
public final class MarcaSerie {

    private MarcaSerie() { }

    /** El fondo del botón: el círculo relleno si está hecha; si no, el aro. */
    @DrawableRes
    public static int fondo(boolean hecha) {
        return hecha ? R.drawable.bg_check : R.drawable.bg_check_pendiente;
    }

    /** El estado que dice TalkBack. */
    @StringRes
    public static int estado(boolean hecha) {
        return hecha ? R.string.envivo_hecha : R.string.envivo_sin_hacer;
    }
}
