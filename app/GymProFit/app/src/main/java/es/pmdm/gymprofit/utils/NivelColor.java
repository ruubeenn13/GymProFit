package es.pmdm.gymprofit.utils;

import androidx.annotation.ColorRes;
import androidx.annotation.Nullable;

import es.pmdm.gymprofit.R;

// ============================================================
// NivelColor — el color del punto de nivel de un ejercicio (GP-216, tablero 29): verde para
// principiante, ámbar para intermedio y rojo para avanzado. Experto se pinta como avanzado,
// igual que se enseña su nombre (GP-103). El punto nunca va solo: el nombre del nivel va al lado.
// ============================================================
public final class NivelColor {

    private NivelColor() { }

    /** El color del nivel; el de intermedio si no se conoce, para no inventar ni verde ni rojo. */
    @ColorRes
    public static int de(@Nullable String nivel) {
        switch (NivelVisible.valor(nivel)) {
            case "PRINCIPIANTE": return R.color.gp_success;
            case "AVANZADO":     return R.color.gp_error;
            default:             return R.color.gp_nivel_intermedio;
        }
    }

    /**
     * El color del nivel sobre la tarjeta protagonista (gp_hero), que es oscura en los dos temas:
     * el del tema oscuro siempre (GP-232: el de claro, #99620E sobre #3A2110, daba 2,92).
     */
    @androidx.annotation.ColorInt
    public static int sobreHero(@androidx.annotation.NonNull android.content.Context ctx, @Nullable String nivel) {
        android.content.res.Configuration c = new android.content.res.Configuration(ctx.getResources().getConfiguration());
        c.uiMode = (c.uiMode & ~android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                | android.content.res.Configuration.UI_MODE_NIGHT_YES;
        return ctx.createConfigurationContext(c).getColor(de(nivel));
    }
}
