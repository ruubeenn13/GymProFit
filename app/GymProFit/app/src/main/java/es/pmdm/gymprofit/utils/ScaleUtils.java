package es.pmdm.gymprofit.utils;

import android.content.Context;
import android.content.res.Configuration;

// ============================================================
// ScaleUtils — escala de fuente GLOBAL de la app.
// Devuelve un contexto con un fontScale mayor para que TODO el texto se vea más
// grande de forma uniforme, incluidos los textSize hardcodeados en los layouts
// (fontScale multiplica la conversión sp→px). Se aplica en attachBaseContext de
// las Activities. Respeta además la escala del sistema (accesibilidad): se
// MULTIPLICA sobre la del usuario, no se reemplaza. La barra de navegación queda
// excluida porque sus etiquetas se miden en dp (no en sp).
// ============================================================
public final class ScaleUtils {

    private ScaleUtils() { }

    // Factor de agrandado de toda la app (18% más grande).
    public static final float FONT_SCALE = 1.18f;

    // Envuelve un contexto aplicando la escala de fuente global (sobre la del sistema).
    public static Context wrap(Context base) {
        Configuration cfg = new Configuration(base.getResources().getConfiguration());
        cfg.fontScale = cfg.fontScale * FONT_SCALE;
        return base.createConfigurationContext(cfg);
    }
}
