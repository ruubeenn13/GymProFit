package es.pmdm.gymprofit.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

// ============================================================
// FilaRecord — lo que dice la fila de un récord, igual en la lista de Récords y en «Récords
// recientes» de Progreso (GP-225).
//
// El apoyo empieza en mayúscula («Hace 1 mes · antes 80 kg × 8»): la lista decía «hace 1 mes» y
// Progreso «Hace 1 mes». Para TalkBack, nombre, apoyo y marca, cada uno en su frase, y un récord
// por tiempo dice que es un tiempo («1:15 de tiempo»): el cronómetro solo se ve.
// ============================================================
public final class FilaRecord {

    private FilaRecord() { }

    /**
     * @param cuando    «hace 1 mes» (TiempoRelativo), o null.
     * @param anterior  la marca de antes, o null.
     * @param haceAntes el formato «%1$s · antes %2$s».
     * @return el apoyo, o null si no hay nada que decir.
     */
    @Nullable
    public static String apoyo(@Nullable String cuando, @Nullable String anterior, @NonNull String haceAntes,
                               @NonNull Locale idioma) {
        String c = cuando == null || cuando.isEmpty() ? null
                : cuando.substring(0, 1).toUpperCase(idioma) + cuando.substring(1);
        if (c == null) return anterior;
        return anterior == null ? c : String.format(idioma, haceAntes, c, anterior);
    }

    /**
     * @param deTiempo el formato «%1$s de tiempo» para un récord por tiempo.
     * @return lo que lee TalkBack de la fila.
     */
    @NonNull
    public static String dicho(@NonNull String nombre, @Nullable String apoyo, @NonNull String marca,
                               boolean porTiempo, @NonNull String deTiempo) {
        return FrasesA11y.unir(nombre, apoyo, porTiempo ? String.format(deTiempo, marca) : marca);
    }
}
