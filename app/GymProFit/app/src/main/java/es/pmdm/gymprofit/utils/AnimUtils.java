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

    // Transición de fundido entre las pestañas principales (Home/Rutinas/
    // Ejercicios/Nutrición/Perfil). La nueva pantalla aparece con un fundido
    // suave; la burbuja de la barra flotante viaja por encima.
    public static void transicionTab(Activity activity) {
        if (activity == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN,
                    R.anim.nav_fade_in, R.anim.nav_fade_out);
        } else {
            //noinspection deprecation
            activity.overridePendingTransition(R.anim.nav_fade_in, R.anim.nav_fade_out);
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
