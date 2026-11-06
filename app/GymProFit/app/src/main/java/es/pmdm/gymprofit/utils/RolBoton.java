package es.pmdm.gymprofit.utils;

import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

// ============================================================
// RolBoton — un contenedor pulsable que TalkBack anuncia como «botón» (GP-225, DEC-019).
//
// Los «+» de Añadir, de «Lo que te encaja» y de las ideas de Comida son un FrameLayout con el
// icono dentro: tenían nombre, pero TalkBack no decía que fuesen botones.
// ============================================================
public final class RolBoton {

    private RolBoton() { }

    /** El nombre de clase que se da a la vista: el de un botón. */
    public static final String CLASE = Button.class.getName();

    /** Le pone el rol de botón a la vista, sin tocar lo demás que diga. */
    public static void aplicar(@NonNull View v) {
        ViewCompat.setAccessibilityDelegate(v, new AccessibilityDelegateCompat() {
            @Override
            public void onInitializeAccessibilityNodeInfo(@NonNull View host, @NonNull AccessibilityNodeInfoCompat info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setClassName(CLASE);
            }
        });
    }
}
