package es.pmdm.gymprofit.utils;

import android.app.Activity;
import android.os.Build;

import es.pmdm.gymprofit.R;

// ============================================================
// AnimUtils — utilidades de animación de transición entre Activities.
// Centraliza la desactivación de la animación de transición (navegación inferior
// instantánea) usando la API correcta según la versión: overrideActivityTransition
// (API 34+) y overridePendingTransition (deprecada) por debajo. Así se evita el
// método deprecado repetido en cada Activity.
// ============================================================
public final class AnimUtils {

    private AnimUtils() {}

    // Aplica el deslizamiento lateral entre pantallas. En API 34+ el
    // windowAnimationStyle del tema es poco fiable, así que se registra la
    // transición por-Activity con overrideActivityTransition (entra desde la
    // derecha al abrir, sale hacia la derecha al cerrar). En API < 34 la animación
    // la sigue aportando el windowAnimationStyle del tema. Llamar en onCreate.
    public static void aplicarDeslizamiento(Activity activity) {
        if (activity == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN,
                    R.anim.slide_in_right, R.anim.slide_out_left);
            activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE,
                    R.anim.slide_in_left, R.anim.slide_out_right);
        }
    }

    // Presenta la Activity como MODAL: entra desde abajo al abrir y sale hacia abajo al
    // cerrar (el fondo se queda quieto). Para pantallas de crear/registrar. Llamar en
    // onCreate. En API < 34 lo aporta el windowAnimationStyle del tema (deslizamiento).
    public static void aplicarModal(Activity activity) {
        if (activity == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN,
                    R.anim.slide_in_up, 0);
            activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE,
                    0, R.anim.slide_out_down);
        }
    }

    // Quita la animación de entrada/salida de la Activity (transición instantánea).
    public static void sinAnimacion(Activity activity) {
        if (activity == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // API 34+: sustituto no deprecado de overridePendingTransition.
            activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN, 0, 0);
            activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE, 0, 0);
        } else {
            //noinspection deprecation
            activity.overridePendingTransition(0, 0);
        }
    }
}
