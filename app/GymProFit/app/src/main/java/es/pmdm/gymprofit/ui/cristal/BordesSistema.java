package es.pmdm.gymprofit.ui.cristal;

// ============================================================
// BordesSistema — dónde acaba el contenido frente a las barras del sistema (GP-285, DEC-070).
//
// La barra flotante deja pasar el contenido por debajo, y con él llegaba hasta los iconos del
// sistema: las cifras de «Tu día» bajo ◁ ● ■ y, arriba, cortado en seco bajo el reloj. Dos
// bordes (BordeSistema) lo desvanecen antes: el fondo base, macizo donde están los iconos, y
// de ahí a transparente.
//
//   · Arriba: la barra de estado entera, maciza, y un tramo de degradado por debajo.
//   · Abajo, con botones: la franja de los botones, maciza, y el degradado hasta media
//     cápsula de la barra flotante (el contenido se sigue viendo pasar tras ella).
//   · Abajo, con gestos: corto, del borde de la pantalla al de la cápsula y sin tramo macizo;
//     no hay iconos que proteger, solo el asa.
//
// No dependen del nivel del cristal: son los mismos en los tres. Aquí solo hay cuentas, sin
// nada de Android, para poder probarlas (BordesSistemaTest); quien las usa es MainActivity.
// ============================================================
public final class BordesSistema {

    // Tramos del degradado: el alfa sube con el cuadrado de la distancia (despacio junto al
    // contenido, deprisa junto a los iconos), y un degradado lineal de dos colores no lo da.
    private static final int TRAMOS = 6;

    // Android 10: de aquí en adelante hay navegación por gestos y contraste forzado del sistema.
    private static final int API_GESTOS = 29;
    // Android 8: de aquí en adelante los botones de navegación pueden ser oscuros.
    private static final int API_BOTONES_OSCUROS = 26;

    private BordesSistema() { }

    /**
     * Si abajo hay botones del sistema (◁ ● ■) y no la barra de gestos.
     *
     * @param api             versión de Android.
     * @param huecoNavegacion alto de la barra de navegación del sistema, en px.
     * @param huecoTocable    lo que de ese hueco es tocable (0 con gestos), en px; antes de
     *                        Android 10 no se mira, porque no hay gestos.
     */
    public static boolean hayBotones(int api, int huecoNavegacion, int huecoTocable) {
        if (huecoNavegacion <= 0) return false;
        return api < API_GESTOS || huecoTocable > 0;
    }

    /** Alto del borde de arriba: la barra de estado y el tramo en que el contenido se desvanece. */
    public static int altoSuperior(int barraEstado, int desvanecido) {
        return barraEstado + desvanecido;
    }

    /**
     * Alto del borde de abajo, desde el borde de la pantalla.
     *
     * @param hueco       alto de la barra de navegación del sistema.
     * @param botones     si son botones ({@link #hayBotones}).
     * @param margenBarra de la cápsula al hueco del sistema (barra_margen_abajo).
     * @param altoBarra   alto de la cápsula (barra_alto).
     */
    public static int altoInferior(int hueco, boolean botones, int margenBarra, int altoBarra) {
        return hueco + margenBarra + (botones ? altoBarra / 2 : 0);
    }

    /** Lo que del borde de abajo es macizo: con botones, su franja entera; con gestos, nada. */
    public static int macizoInferior(int hueco, boolean botones) {
        return botones ? hueco : 0;
    }

    /**
     * Color de la barra de navegación de la ventana. Transparente: su fondo es el tramo macizo
     * del borde, que va en el contenido y por eso el velo del «+» lo cubre igual que al resto.
     * La excepción es Android 7 en claro, donde los botones son siempre blancos y necesitan
     * algo oscuro detrás.
     *
     * @param iconosOscuros si el tema pide iconos oscuros (el claro).
     * @param velo          el color de ese fondo oscuro.
     */
    public static int colorBarraNavegacion(int api, boolean iconosOscuros, int velo) {
        return api < API_BOTONES_OSCUROS && iconosOscuros ? velo : 0;
    }

    /**
     * Colores del degradado, de la punta transparente a la maciza.
     *
     * @param base   el fondo base, opaco.
     * @param macizo parte del alto que es maciza, de 0 a 1.
     */
    public static int[] colores(int base, float macizo) {
        int[] c = new int[TRAMOS + (macizo > 0f ? 2 : 1)];
        int rgb = base & 0x00FFFFFF;
        for (int i = 0; i <= TRAMOS; i++) {
            float t = i / (float) TRAMOS;
            c[i] = (Math.round(255f * t * t) << 24) | rgb;
        }
        if (macizo > 0f) c[TRAMOS + 1] = 0xFF000000 | rgb;
        return c;
    }

    /** Las posiciones de esos colores, de 0 a 1. */
    public static float[] paradas(float macizo) {
        float[] p = new float[TRAMOS + (macizo > 0f ? 2 : 1)];
        for (int i = 0; i <= TRAMOS; i++) p[i] = (i / (float) TRAMOS) * (1f - macizo);
        if (macizo > 0f) p[TRAMOS + 1] = 1f;
        return p;
    }
}
