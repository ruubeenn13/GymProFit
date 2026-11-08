package es.pmdm.gymprofit.utils;

import androidx.annotation.NonNull;

import java.util.List;

// ============================================================
// DuracionRutina — cuánto dura una rutina, para calcularlo en vivo al crearla (GP-244, punto 9).
//
// Copia de ReglasPrograma.centesimas y redondear, en la API: la duración se calcula, no se
// teclea, y la app la necesita mientras se eligen ejercicios. Las dos se prueban contra la
// misma tabla (api/gymprofit-api/src/test/resources/programas/duracion_rutina_casos.csv), así
// que si una cambia y la otra no, falla un test.
//
// La fórmula del catálogo: 8 min de calentamiento, 1 min por cada cambio de ejercicio y, por
// serie, el trabajo (3,5 s por repetición a mitad del rango, o los segundos; el doble si es
// por lado) más su descanso, salvo tras la última serie de la rutina. Se redondea a los 5 min
// más cercanos, la mitad hacia arriba. En centésimas de segundo, con enteros: el redondeo es
// exacto.
// ============================================================
public final class DuracionRutina {

    private static final long CALENTAMIENTO_CS = 8 * 60 * 100;
    private static final long CAMBIO_CS = 60 * 100;

    private DuracionRutina() { }

    /** Lo que cuenta de un ejercicio para la duración. */
    public static final class Pieza {
        final int series, min, max, descanso;
        final boolean segundos, porLado;

        /**
         * @param segundos si la serie se mide en segundos y no en repeticiones.
         * @param porLado  si va por pierna, brazo o lado (cuenta el doble).
         * @param descanso segundos de descanso tras cada serie.
         */
        public Pieza(int series, int min, int max, boolean segundos, boolean porLado, int descanso) {
            this.series = series;
            this.min = min;
            this.max = max;
            this.segundos = segundos;
            this.porLado = porLado;
            this.descanso = descanso;
        }

        /** La pauta con la que entra un ejercicio al crear una rutina: 3 series de 8 a 12 y 90 s. */
        @NonNull
        public static Pieza porDefecto() {
            return new Pieza(3, 8, 12, false, false, 90);
        }
    }

    /** Duración sin redondear, en centésimas de segundo. */
    public static long centesimas(@NonNull List<Pieza> rutina) {
        long total = CALENTAMIENTO_CS + CAMBIO_CS * Math.max(0, rutina.size() - 1);
        int seriesTotales = 0;
        for (Pieza p : rutina) seriesTotales += p.series;
        int hechas = 0;
        for (Pieza e : rutina) {
            long trabajo = (long) (e.min + e.max) * (e.segundos ? 50 : 175);
            if (e.porLado) trabajo *= 2;
            for (int s = 0; s < e.series; s++) {
                total += trabajo;
                if (++hechas < seriesTotales) total += e.descanso * 100L;
            }
        }
        return total;
    }

    /** A los 5 min más cercanos, la mitad hacia arriba. */
    public static int redondear(long centesimas) {
        return (int) ((centesimas + 15_000) / 30_000) * 5;
    }

    /** La duración redondeada, en minutos. */
    public static int minutos(@NonNull List<Pieza> rutina) {
        return redondear(centesimas(rutina));
    }
}
