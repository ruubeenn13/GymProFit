package es.pmdm.gymprofit.ui.cristal;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import es.pmdm.gymprofit.R;

// ============================================================
// VentanaCristal — la ventana de una pantalla Liquid Glass (GP-285, DEC-070).
//
// En una pantalla con fondo ambiental, el fondo llega bajo la barra de estado y el contenido
// bajo la de navegación: las dos barras del sistema son transparentes (desde Android 15 ya lo
// son; antes llevan el color del tema). El fondo de sus iconos lo ponen los bordes del sistema
// (BordeSistema), que van en el contenido.
//
// Lo usan la navegación principal (MainActivity) y la Sesión en vivo: una sola fuente.
// ============================================================
public final class VentanaCristal {

    private VentanaCristal() { }

    /**
     * Deja transparentes las barras del sistema de esa pantalla.
     *
     * <p>Desde Android 10 el sistema pintaba además su franja de contraste bajo los tres botones,
     * por encima de lo que la pantalla pinte ahí (en claro quedaba más clara que la pantalla):
     * se apaga. En Android 7 los botones de navegación son siempre blancos: en claro (el tema
     * que pide iconos oscuros) se quedan con un velo detrás para que se vean
     * ({@link BordesSistema#colorBarraNavegacion}).
     */
    @SuppressWarnings("deprecation")
    public static void barrasTransparentes(@NonNull Activity actividad) {
        Window ventana = actividad.getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ventana.setNavigationBarContrastEnforced(false);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) return;
        ventana.setStatusBarColor(Color.TRANSPARENT);
        ventana.setNavigationBarColor(BordesSistema.colorBarraNavegacion(Build.VERSION.SDK_INT,
                actividad.getResources().getBoolean(R.bool.gp_iconos_barra_oscuros),
                ContextCompat.getColor(actividad, R.color.gp_velo)));
    }
}
