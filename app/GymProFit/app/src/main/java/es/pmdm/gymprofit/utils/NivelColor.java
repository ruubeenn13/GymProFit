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
}
