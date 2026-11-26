package es.pmdm.gymprofit.ui.cristal;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.view.accessibility.AccessibilityManager;

import androidx.annotation.NonNull;

import es.pmdm.gymprofit.utils.PreferencesManager;

// ============================================================
// NivelCristal — qué hace el cristal vivo en este móvil (GP-285, DEC-070).
//
// El cristal vivo (la barra de abajo, la barra de la sesión, la hoja del «+») tiene contenido
// que pasa por debajo. Según lo que el móvil puede hacer:
//
//   1 · REFRACCION  Android 13 o más: desenfoca lo de detrás y lo refracta en los bordes.
//   2 · DESENFOQUE  Android 12 y 12L: solo desenfoca (no hay RuntimeShader).
//   3 · OPACO       Android 11 o menos, texto de alto contraste encendido o un móvil que no
//                   aguanta los otros dos: superficie translúcida casi opaca, sin desenfoque.
//
// Toda la decisión está aquí, en elegir(), que no toca Android y por eso se prueba sola
// (NivelCristalTest). actual() solo junta los tres datos. El cristal estático (tarjetas,
// chips) no pasa por aquí: es igual en todas las versiones.
// ============================================================
public enum NivelCristal {

    /** Desenfoque y refracción en los bordes. */
    REFRACCION(1),
    /** Solo desenfoque. */
    DESENFOQUE(2),
    /** Superficie casi opaca, sin desenfoque. */
    OPACO(3);

    /** El número del nivel, el de la documentación y el que se guarda en las preferencias. */
    public final int numero;

    NivelCristal(int numero) {
        this.numero = numero;
    }

    /**
     * El nivel para un móvil.
     *
     * @param api           versión de Android (Build.VERSION.SDK_INT).
     * @param altoContraste si el texto de alto contraste del sistema está encendido.
     * @param techo         el mejor nivel que el rendimiento medido permite (REFRACCION si
     *                      nunca ha habido que bajar).
     */
    @NonNull
    public static NivelCristal elegir(int api, boolean altoContraste, @NonNull NivelCristal techo) {
        if (altoContraste) return OPACO;
        NivelCristal porVersion = api >= Build.VERSION_CODES.TIRAMISU ? REFRACCION
                : api >= Build.VERSION_CODES.S ? DESENFOQUE : OPACO;
        // El techo solo baja: se queda el peor de los dos.
        return porVersion.numero >= techo.numero ? porVersion : techo;
    }

    /** El nivel siguiente hacia abajo; el opaco es el suelo. */
    @NonNull
    public NivelCristal unoMenos() {
        return this == REFRACCION ? DESENFOQUE : OPACO;
    }

    /** El nivel de un número guardado; uno que no existe no limita nada. */
    @NonNull
    public static NivelCristal deNumero(int numero) {
        for (NivelCristal n : values()) if (n.numero == numero) return n;
        return REFRACCION;
    }

    /** El nivel de este móvil ahora mismo. */
    @NonNull
    public static NivelCristal actual(@NonNull Context context) {
        NivelCristal techo = deNumero(new PreferencesManager(context).getCristalTecho());
        return elegir(Build.VERSION.SDK_INT, altoContraste(context), techo);
    }

    // El texto de alto contraste del sistema (Ajustes › Accesibilidad). Hasta Android 16 no hay
    // API pública: se lee el ajuste por su nombre, que es estable desde Android 5.
    private static boolean altoContraste(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= 36) {
            AccessibilityManager am = context.getSystemService(AccessibilityManager.class);
            if (am != null) return am.isHighContrastTextEnabled();
        }
        try {
            return Settings.Secure.getInt(context.getContentResolver(), "high_text_contrast_enabled", 0) == 1;
        } catch (SecurityException e) {
            // Un fabricante que no deje leer el ajuste: se sigue como si estuviera apagado. Es
            // seguro porque el texto pasa 4,5:1 en los tres niveles (ContrasteCristalTest); lo
            // único que se pierde es bajar al opaco por preferencia.
            return false;
        }
    }
}
