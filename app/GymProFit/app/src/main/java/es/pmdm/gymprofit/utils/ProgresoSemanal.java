package es.pmdm.gymprofit.utils;

import androidx.annotation.Nullable;

// ============================================================
// ProgresoSemanal — «Tu progreso» de Inicio (GP-216, tablero 13): la altura de cada barra de
// kilos por semana y el «+12 % en 6 semanas».
//
// Los kilos de cada semana salen de GET /sesiones/usuario/{id}/volumen-semanal. La última
// semana es la que está en curso: se dibuja, pero no cuenta para el porcentaje, porque un
// jueves a medias parecería una caída. Sin dependencias de Android, para probarla en la JVM.
// ============================================================
public final class ProgresoSemanal {

    /** El cambio entre la primera semana con kilos y la última completa con kilos. */
    public static final class Variacion {
        /** Con signo: positivo si sube. */
        public final int porcentaje;
        /** Las semanas que abarca la comparación, ambas incluidas. */
        public final int semanas;

        Variacion(int porcentaje, int semanas) {
            this.porcentaje = porcentaje;
            this.semanas = semanas;
        }
    }

    private ProgresoSemanal() { }

    /** Alto de cada barra, de 0 a 1: la mayor, entera. Todo ceros, sin barras. */
    public static float[] alturas(double[] kilos) {
        double max = 0;
        for (double k : kilos) max = Math.max(max, k);
        float[] a = new float[kilos.length];
        if (max <= 0) return a;
        for (int i = 0; i < kilos.length; i++) a[i] = (float) (Math.max(0, kilos[i]) / max);
        return a;
    }

    /** Hay algo que dibujar: alguna semana con kilos. */
    public static boolean hayDatos(double[] kilos) {
        for (double k : kilos) if (k > 0) return true;
        return false;
    }

    /**
     * El cambio de los kilos entre semanas.
     *
     * @param kilos           los kilos de cada semana, de la más antigua a la más reciente.
     * @param ultimaEnCurso   la última es la semana en curso y no cuenta.
     * @return null si no hay dos semanas con kilos que comparar.
     */
    @Nullable
    public static Variacion variacion(double[] kilos, boolean ultimaEnCurso) {
        int n = ultimaEnCurso ? kilos.length - 1 : kilos.length;
        int primera = -1, ultima = -1;
        for (int i = 0; i < n; i++) {
            if (kilos[i] <= 0) continue;
            if (primera < 0) primera = i;
            ultima = i;
        }
        if (primera < 0 || ultima == primera) return null;
        double cambio = (kilos[ultima] - kilos[primera]) / kilos[primera] * 100.0;
        return new Variacion((int) Math.round(Math.abs(cambio)) * (cambio < 0 ? -1 : 1), ultima - primera + 1);
    }
}
