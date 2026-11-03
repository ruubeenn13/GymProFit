package es.pmdm.gymprofit.utils;

import android.content.Context;
import android.content.res.Configuration;

import androidx.appcompat.app.AppCompatDelegate;

// ============================================================
// ScaleUtils — escala de fuente GLOBAL de la app.
// Devuelve un contexto con un fontScale mayor para que TODO el texto se vea más
// grande de forma uniforme, incluidos los textSize hardcodeados en los layouts
// (fontScale multiplica la conversión sp→px). Se aplica en attachBaseContext de
// las Activities. Respeta además la escala del sistema (accesibilidad): se
// MULTIPLICA sobre la del usuario, no se reemplaza. Las etiquetas de la barra de
// navegación siguen a la del sistema solo hasta 1,3 (GP-128, BarraNavegacion).
// ============================================================
public final class ScaleUtils {

    private ScaleUtils() { }

    // Sin agrandado. Antes valía 1.18f, y era un parche que tapaba que nadie
    // controlaba los tamaños: agrandaba TODO el texto un 18 por ciento y dejaba
    // los márgenes donde estaban, así que un título de 34 sp se pintaba a 40
    // dentro de una tarjeta con el mismo padding. Y se multiplicaba por la escala
    // que el usuario tuviera en los ajustes del sistema: alguien con 1,3 acababa
    // en 1,53 y la cifra de 52 sp se iba a 80.
    //
    // Ahora los tamaños vienen bien de origen, desde la escala de dimens.xml, ya
    // rebasados para que lo que se ve en pantalla no cambie.
    //
    // La constante se queda en 1.0 en vez de borrarse porque wrap() lo llaman 35
    // Activities desde su attachBaseContext; se retira cuando se toquen.
    public static final float FONT_SCALE = 1.0f;

    // Envuelve un contexto aplicando SOLO la escala de fuente (override mínimo).
    // IMPORTANTE: no se copia toda la Configuration del base; si se copiara, se
    // CONGELARÍA el modo (claro/oscuro) y el idioma en el momento del wrap, y al
    // recrear la Activity por cambio de tema/idioma se quedaría "pillado" con el
    // anterior. Con un override mínimo, uiMode y locale siguen vivos desde el base.
    //
    // El claro u oscuro de la app sí va en el override, y solo él (GP-216, lote 4). Si no,
    // AppCompat lo pone cambiando a mano la configuración de los Resources de la pantalla
    // cuando ya coinciden con los del proceso, y lo pierde en cuanto el proceso recibe la
    // del sistema: al encender o apagar TalkBack, por ejemplo, lo que se pintaba después
    // salía con los colores del claro dentro de la pantalla oscura. Un override se vuelve
    // a aplicar encima de cada configuración nueva. No congela nada: cambiar el tema
    // recrea las pantallas, y cada una lo vuelve a leer aquí.
    public static Context wrap(Context base) {
        Configuration override = new Configuration();
        override.fontScale = base.getResources().getConfiguration().fontScale * FONT_SCALE;
        int tema = PreferencesManager.temaGuardado(base);
        if (tema == AppCompatDelegate.MODE_NIGHT_YES) override.uiMode = Configuration.UI_MODE_NIGHT_YES;
        else if (tema == AppCompatDelegate.MODE_NIGHT_NO) override.uiMode = Configuration.UI_MODE_NIGHT_NO;
        return base.createConfigurationContext(override);
    }
}
