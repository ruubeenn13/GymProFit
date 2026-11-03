package es.pmdm.gymprofit.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

// ============================================================
// EjeFechas — el eje x de las gráficas con período en semanas va por fecha, no por orden (GP-228):
// el peso de «Tu cuerpo», Medidas y la progresión de la ficha del ejercicio.
//
// Por orden, dos pesajes separados un día salían igual de lejos que dos separados un mes. Ahora la
// x de cada punto es su día de calendario (días desde el 1-1-1970, en UTC para que un cambio de
// hora no descuadre la resta) y la etiqueta se saca de esa x con la fecha corta del idioma.
// Varios puntos del mismo día darían una línea vertical: se queda uno por día, el último (una
// medición corrige a la anterior) o el mayor (la mejor marca del día).
// Sin dependencias de Android, para probarla en la JVM.
// ============================================================
public final class EjeFechas {

    private static final long MS_DIA = 86_400_000L;
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    /** Qué punto se queda cuando hay varios el mismo día. */
    public enum DelDia {
        /** El de hora más tardía (a igual hora, el que viene después en la lista). */
        ULTIMO,
        /** El de valor más alto. */
        MAYOR
    }

    /** Un punto de la gráfica: su día como x y su valor. */
    public static final class Punto {
        /** El día de calendario (días desde el 1-1-1970). */
        public final float x;
        public final float y;
        /** La fecha ISO de la que sale. */
        public final String iso;

        Punto(float x, float y, String iso) {
            this.x = x;
            this.y = y;
            this.iso = iso;
        }
    }

    private EjeFechas() { }

    /**
     * El día de calendario de una fecha ISO («yyyy-MM-dd…»; lo que sigue se ignora).
     *
     * @return días desde el 1-1-1970, o null si la fecha no se entiende.
     */
    @Nullable
    public static Long dia(@Nullable String iso) {
        if (iso == null || iso.length() < 10) return null;
        try {
            int y = Integer.parseInt(iso.substring(0, 4));
            int m = Integer.parseInt(iso.substring(5, 7));
            int d = Integer.parseInt(iso.substring(8, 10));
            Calendar c = Calendar.getInstance(UTC);
            c.clear();
            c.setLenient(false);
            c.set(y, m - 1, d);
            return Math.floorDiv(c.getTimeInMillis(), MS_DIA);
        } catch (RuntimeException e) {   // número mal escrito o fecha imposible (13-45)
            return null;
        }
    }

    /**
     * Los puntos de la gráfica, uno por día y ordenados por fecha.
     *
     * @param fechas  la fecha ISO de cada valor (las que no se entienden se saltan).
     * @param valores los valores, en el mismo orden que las fechas.
     * @param regla   qué punto se queda si hay varios el mismo día.
     */
    @NonNull
    public static List<Punto> porDia(@NonNull List<String> fechas, @NonNull List<Float> valores,
                                     @NonNull DelDia regla) {
        Map<Long, Punto> porDia = new LinkedHashMap<>();
        for (int i = 0; i < fechas.size() && i < valores.size(); i++) {
            String iso = fechas.get(i);
            Long d = dia(iso);
            Float v = valores.get(i);
            if (d == null || v == null) continue;
            Punto nuevo = new Punto(d, v, iso);
            Punto hay = porDia.get(d);
            if (hay == null || gana(nuevo, hay, regla)) porDia.put(d, nuevo);
        }
        List<Punto> r = new ArrayList<>(porDia.values());
        Collections.sort(r, (a, b) -> Float.compare(a.x, b.x));
        return r;
    }

    private static boolean gana(Punto nuevo, Punto hay, DelDia regla) {
        if (regla == DelDia.MAYOR) return nuevo.y > hay.y;
        return nuevo.iso.compareTo(hay.iso) >= 0;   // ISO ordena como texto igual que el calendario
    }

    /** La fecha ISO («yyyy-MM-dd») del día {@code x} (se redondea al día más cercano). */
    @NonNull
    public static String iso(float x) {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        f.setTimeZone(UTC);
        return f.format(new java.util.Date(Math.round(x) * MS_DIA));
    }

    /**
     * La etiqueta del eje para el día {@code x}: la fecha corta del idioma («28 sept» / «Sep 28»).
     *
     * @param patron el patrón corto del idioma ({@code inicio_fecha_corta_patron}).
     */
    @NonNull
    public static String etiqueta(float x, @NonNull Locale idioma, @NonNull String patron) {
        SimpleDateFormat f = new SimpleDateFormat(patron, idioma);
        f.setTimeZone(UTC);
        return f.format(new java.util.Date(Math.round(x) * MS_DIA));
    }

    /**
     * Si el eje lleva etiqueta en {@code x}: con un solo punto, solo bajo él (sin el «ayer» y el
     * «mañana» que la gráfica añade a los lados para tener ancho); con más, en todas.
     */
    public static boolean llevaEtiqueta(float x, @NonNull List<Punto> puntos) {
        if (puntos.size() != 1) return true;
        return Math.round(x) == Math.round(puntos.get(0).x);
    }
}
