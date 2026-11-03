package es.pmdm.gymprofit.utils;

import androidx.annotation.Nullable;

import java.util.Calendar;
import java.util.Locale;

// ============================================================
// KilosMovidos — «Kilos movidos» de Progreso (GP-216, lote 4, tablero 16): cuántas semanas pide
// cada período, desde qué lunes, cuánto lleva esta semana frente a la pasada y cada cuántas
// barras va una etiqueta.
//
// «4 sem.» y «8 sem.» cuentan la semana en curso. «Todo» va desde la semana de la primera
// sesión hasta hoy, con el tope de la API (104 semanas, dos años). El lunes lo calcula la app
// en su hora, como en Inicio: la API no sabe el huso del usuario.
//
// El avance compara la semana en curso con la pasada, que sí está completa: mientras no la
// supera dice «el 62 % de la pasada» (un jueves a medias no es una caída); al superarla,
// «+12 %». Sin kilos la semana pasada no hay con qué comparar y no se dice nada.
// Sin dependencias de Android, para probarla en la JVM.
// ============================================================
public final class KilosMovidos {

    /** El tope de semanas que da la API de una vez (dos años). */
    public static final int TOPE_SEMANAS = 104;

    /** Las semanas de cada período del selector, en su orden: 4 sem., 8 sem. y Todo (0). */
    public static final int[] PERIODOS = {4, 8, 0};

    /** El período que se elige al abrir: 8 semanas, como el tablero. */
    public static final int PERIODO_INICIAL = 1;

    /** Cómo va esta semana frente a la pasada. */
    public static final class Avance {
        /** true: ya la supera y {@link #porcentaje} es lo que sube; false: es el % de la pasada. */
        public final boolean supera;
        /** «+12» si supera; «62» (el 62 % de la pasada) si no. */
        public final int porcentaje;

        Avance(boolean supera, int porcentaje) {
            this.supera = supera;
            this.porcentaje = porcentaje;
        }
    }

    private KilosMovidos() { }

    /**
     * Las semanas que pide un período.
     *
     * @param periodo    semanas del período (4, 8) o 0 para «Todo».
     * @param primeraIso fecha ISO de la primera sesión (para «Todo»); null si no hay ninguna.
     * @param hoy        el día de hoy, «yyyy-MM-dd».
     * @return de 1 a {@link #TOPE_SEMANAS}.
     */
    public static int semanas(int periodo, @Nullable String primeraIso, String hoy) {
        if (periodo > 0) return Math.min(periodo, TOPE_SEMANAS);
        int dias = TiempoRelativo.diasEntre(lunesDe(primeraIso), lunesDe(hoy));
        if (dias < 0) return 1;
        return Math.max(1, Math.min(TOPE_SEMANAS, dias / 7 + 1));
    }

    /**
     * El lunes a las 00:00 de la primera de {@code semanas} semanas que acaban en la de hoy,
     * como lo pide la API («yyyy-MM-dd'T'00:00:00»).
     */
    public static String desdeIso(String hoy, int semanas) {
        Calendar c = lunes(hoy);
        if (c == null) c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_MONTH, -7 * (semanas - 1));
        return String.format(Locale.US, "%tF", c) + "T00:00:00";
    }

    /**
     * Esta semana frente a la pasada.
     *
     * @return null si la pasada no tuvo kilos (no hay con qué comparar).
     */
    @Nullable
    public static Avance avance(double esta, double pasada) {
        if (pasada <= 0) return null;
        if (esta > pasada) {
            // Lo que sube, hacia arriba: si supera, aunque sea por poco, no dice «+0 %».
            return new Avance(true, (int) Math.ceil((esta - pasada) / pasada * 100.0 - 1e-9));
        }
        // Lo que lleva, hacia abajo: mientras no la supera, nunca dice «el 100 %» antes de tiempo.
        return new Avance(false, (int) Math.floor(Math.max(0, esta) / pasada * 100.0 + 1e-9));
    }

    /**
     * Cada cuántas barras va una etiqueta para que no se pisen: 1 si caben todas.
     *
     * @param anchoCelda    lo que ocupa cada barra con su hueco.
     * @param anchoEtiqueta lo que ocupa la etiqueta más ancha con su aire.
     */
    public static int pasoEtiquetas(float anchoCelda, float anchoEtiqueta) {
        if (anchoCelda <= 0) return Integer.MAX_VALUE;
        return Math.max(1, (int) Math.ceil(anchoEtiqueta / anchoCelda - 1e-6));
    }

    /** Si la barra {@code i} de {@code n} lleva etiqueta: la última (esta semana) siempre, y hacia atrás cada {@code paso}. */
    public static boolean llevaEtiqueta(int i, int n, int paso) {
        return paso > 0 && (n - 1 - i) % paso == 0;
    }

    // El lunes de la semana de un día ISO, en ISO; null si no se entiende.
    @Nullable
    private static String lunesDe(@Nullable String iso) {
        Calendar c = lunes(iso);
        return c == null ? null : String.format(Locale.US, "%tF", c);
    }

    @Nullable
    private static Calendar lunes(@Nullable String iso) {
        if (iso == null || iso.length() < 10) return null;
        try {
            Calendar c = Calendar.getInstance();
            c.clear();
            c.set(Integer.parseInt(iso.substring(0, 4)), Integer.parseInt(iso.substring(5, 7)) - 1,
                    Integer.parseInt(iso.substring(8, 10)), 0, 0, 0);
            int dow = c.get(Calendar.DAY_OF_WEEK);           // domingo = 1 … sábado = 7
            c.add(Calendar.DAY_OF_MONTH, -((dow + 5) % 7));  // lunes = 0 días atrás
            return c;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
