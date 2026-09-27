package es.pmdm.gymprofit.utils;

import android.content.Context;

import androidx.annotation.Nullable;

import java.util.Calendar;
import java.util.Locale;

import es.pmdm.gymprofit.R;

// ============================================================
// TiempoRelativo — «hoy», «ayer», «hace 4 días», «hace 2 semanas» (GP-105).
//
// Lo usan «Hoy toca», el último récord, la última medición de la hoja del «+» y los
// récords de Progreso. Se cuenta en días de calendario y no en horas: una sesión de
// anoche a las 23:00 es «ayer» aunque hayan pasado dos horas.
// ============================================================
public final class TiempoRelativo {

    private TiempoRelativo() { }

    /**
     * Días de calendario entre dos días ISO («yyyy-MM-dd…»; lo que sigue se ignora).
     *
     * @return {@code hoy - dia}, o -1 si alguna fecha no se entiende.
     */
    public static int diasEntre(@Nullable String dia, @Nullable String hoy) {
        long a = millisDelDia(dia), b = millisDelDia(hoy);
        if (a < 0 || b < 0) return -1;
        // Redondeo y no división exacta: un cambio de hora deja días de 23 o 25 horas.
        return (int) Math.round((b - a) / 86_400_000.0);
    }

    /** El día de hoy en ISO, «yyyy-MM-dd». */
    public static String hoy() {
        return String.format(Locale.US, "%tF", Calendar.getInstance());
    }

    /**
     * Texto relativo para una fecha ISO de la API.
     *
     * @return «hoy», «ayer», «hace N días/semanas/meses/años», o {@code null} si la
     *         fecha no se entiende o es futura.
     */
    @Nullable
    public static String texto(Context ctx, @Nullable String iso) {
        int dias = diasEntre(iso, hoy());
        if (dias < 0) return null;
        if (dias == 0) return ctx.getString(R.string.tiempo_hoy);
        if (dias == 1) return ctx.getString(R.string.tiempo_ayer);
        if (dias < 7) return plural(ctx, R.plurals.tiempo_hace_dias, dias);
        if (dias < 35) return plural(ctx, R.plurals.tiempo_hace_semanas, dias / 7);
        if (dias < 365) return plural(ctx, R.plurals.tiempo_hace_meses, Math.max(1, dias / 30));
        return plural(ctx, R.plurals.tiempo_hace_anios, dias / 365);
    }

    private static String plural(Context ctx, int id, int n) {
        return ctx.getResources().getQuantityString(id, n, n);
    }

    // Medianoche UTC del día: con UTC no hay cambios de hora que descuadren la resta.
    private static long millisDelDia(@Nullable String iso) {
        if (iso == null || iso.length() < 10) return -1;
        try {
            int y = Integer.parseInt(iso.substring(0, 4));
            int m = Integer.parseInt(iso.substring(5, 7));
            int d = Integer.parseInt(iso.substring(8, 10));
            Calendar c = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"));
            c.clear();
            c.set(y, m - 1, d);
            return c.getTimeInMillis();
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
