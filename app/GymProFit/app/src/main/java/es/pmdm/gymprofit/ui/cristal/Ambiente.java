package es.pmdm.gymprofit.ui.cristal;

import androidx.annotation.NonNull;

// ============================================================
// Ambiente — el fondo ambiental en números (GP-285, DEC-070).
//
// El fondo de las pantallas Liquid Glass es una base casi negra (casi blanca en claro) con
// tres brillos radiales difusos. Aquí está su geometría, que es lo que FondoAmbiental pinta, y
// la misma cuenta hecha a mano: colorEn() da el color de un punto y sobre() compone una capa
// translúcida encima, que es como ContrasteCristalTest mide el texto sobre «el punto más
// claro de fondo + brillo + cristal».
//
// Cada brillo es una elipse: su centro en fracción del ancho y del alto de la pantalla, sus
// radios en fracción del lado corto, y su color con el alfa del centro. El alfa baja en línea
// recta hasta 0 al 70 % del radio (el «transparent 70%» de la maqueta).
//
// No toca Android (los colores son enteros ARGB): se prueba sola.
// ============================================================
public final class Ambiente {

    private Ambiente() {}

    /** Parte del radio en la que el brillo ya es transparente. */
    public static final float FIN = 0.70f;

    /** Un brillo radial del fondo. */
    public static final class Brillo {
        /** Centro, en fracción del ancho y del alto. */
        public final float cx, cy;
        /** Radios de la elipse, en fracción del lado corto de la pantalla. */
        public final float rx, ry;
        /** Color ARGB en el centro. */
        public final int color;

        public Brillo(float cx, float cy, float rx, float ry, int color) {
            this.cx = cx;
            this.cy = cy;
            this.rx = rx;
            this.ry = ry;
            this.color = color;
        }
    }

    /**
     * La mezcla de Inicio: el primero arriba a la derecha, el segundo a la izquierda a media
     * altura y el tercero abajo a la derecha. Las medidas son las de la maqueta de 390 × 844
     * (elipses de 420 × 320, 360 × 300 y 380 × 320).
     */
    @NonNull
    public static Brillo[] inicio(int primero, int segundo, int tercero) {
        return new Brillo[]{
                new Brillo(0.90f, 0.08f, 420f / 390f, 320f / 390f, primero),
                new Brillo(0.00f, 0.42f, 360f / 390f, 300f / 390f, segundo),
                new Brillo(0.85f, 0.78f, 380f / 390f, 320f / 390f, tercero),
        };
    }

    /** El alfa (0 a 1) de un brillo en un punto de una pantalla de ese tamaño. */
    public static float alfaEn(@NonNull Brillo b, float x, float y, float ancho, float alto) {
        float lado = Math.min(ancho, alto);
        float dx = (x - b.cx * ancho) / (b.rx * lado);
        float dy = (y - b.cy * alto) / (b.ry * lado);
        float t = (float) Math.sqrt(dx * dx + dy * dy) / FIN;
        return t >= 1f ? 0f : (b.color >>> 24) / 255f * (1f - t);
    }

    /** El color opaco del fondo en un punto: la base con cada brillo compuesto encima, en orden. */
    public static int colorEn(float x, float y, float ancho, float alto, int base, @NonNull Brillo[] brillos) {
        int color = base | 0xFF000000;
        for (Brillo b : brillos) {
            int alfa = Math.round(alfaEn(b, x, y, ancho, alto) * 255f);
            color = sobre((alfa << 24) | (b.color & 0x00FFFFFF), color);
        }
        return color;
    }

    /**
     * Compone un color translúcido sobre uno opaco, como lo hace la pantalla (mezcla normal,
     * en sRGB sin linealizar). Devuelve el color opaco que se ve.
     */
    public static int sobre(int encima, int debajo) {
        float a = (encima >>> 24) / 255f;
        int r = Math.round(((encima >> 16) & 0xFF) * a + ((debajo >> 16) & 0xFF) * (1f - a));
        int g = Math.round(((encima >> 8) & 0xFF) * a + ((debajo >> 8) & 0xFF) * (1f - a));
        int b = Math.round((encima & 0xFF) * a + (debajo & 0xFF) * (1f - a));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
