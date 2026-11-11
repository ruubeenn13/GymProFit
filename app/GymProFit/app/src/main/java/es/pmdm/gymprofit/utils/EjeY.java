package es.pmdm.gymprofit.utils;

import androidx.annotation.NonNull;

// ============================================================
// EjeY — el eje de valores de una gráfica de línea (Medidas, lote 12), en lógica pura.
//
// MPAndroidChart reparte cuatro etiquetas entre el mínimo y el máximo y salían pasos de 0,6 o
// 0,8 («13,8 / 14,4 / 15,0»), con la línea de 14,5 % entre dos etiquetas; con un solo punto,
// cada métrica salía con un eje distinto. Aquí el paso es redondo (1, 2 o 5 por una potencia de
// diez, desde 0,1) para que haya unas cuatro divisiones, y el eje empieza y acaba en múltiplos del
// paso. Una línea plana (o un solo punto) queda en medio, con medio punto a cada lado como
// mínimo: 14,5 % va de 14 a 15.
// ============================================================
public final class EjeY {

    // Lo más fino que se escribe en el eje: un decimal (GP-228).
    private static final float PASO_MINIMO = 0.1f;
    // Una línea plana se abre hasta este rango, para que quede en medio y con aire.
    private static final float RANGO_PLANO = 1f;

    /** Donde empieza el eje. */
    public final float inicio;
    /** Donde acaba. */
    public final float fin;
    /** La distancia entre dos etiquetas. */
    public final float paso;

    private EjeY(float inicio, float fin, float paso) {
        this.inicio = inicio;
        this.fin = fin;
        this.paso = paso;
    }

    /** Cuántas etiquetas lleva el eje, contando las dos puntas. */
    public int etiquetas() {
        return Math.round((fin - inicio) / paso) + 1;
    }

    /**
     * El eje para unos valores entre {@code min} y {@code max}.
     *
     * @param min el menor valor de la gráfica.
     * @param max el mayor (igual que {@code min} con un punto o una línea plana).
     */
    @NonNull
    public static EjeY de(float min, float max) {
        if (max < min) {
            float t = min;
            min = max;
            max = t;
        }
        if (max - min < RANGO_PLANO / 2f) {
            float centro = (min + max) / 2f;
            min = centro - RANGO_PLANO / 2f;
            max = centro + RANGO_PLANO / 2f;
        }
        float paso = redondo((max - min) / 4f);
        // Con milésimas de margen: 79,2 / 0,2 da 395,99… en coma flotante.
        float inicio = (float) (Math.floor(min / paso + 1e-3) * paso);
        float fin = (float) (Math.ceil(max / paso - 1e-3) * paso);
        if (fin - inicio < paso) fin = inicio + paso;
        return new EjeY(redondear(inicio), redondear(fin), paso);
    }

    // El primer paso redondo (1, 2 o 5 por una potencia de diez) que no es menor que el bruto.
    private static float redondo(float bruto) {
        double potencia = Math.pow(10, Math.floor(Math.log10(Math.max(bruto, PASO_MINIMO))));
        for (double m : new double[]{1, 2, 5, 10}) {
            if (m * potencia >= bruto - 1e-6) return redondear((float) Math.max(m * potencia, PASO_MINIMO));
        }
        return redondear((float) (10 * potencia));
    }

    // Quita el ruido de coma flotante a un decimal de más: 78.39999 → 78.4.
    private static float redondear(float v) {
        return Math.round(v * 100f) / 100f;
    }
}
