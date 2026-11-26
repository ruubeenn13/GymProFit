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
//   · Abajo, con gestos: el hueco del asa, macizo, y un degradado corto, solo hasta el borde
//     de la cápsula; no hay iconos que proteger, solo el asa, que no puede tener texto detrás.
//
// No dependen del nivel del cristal: son los mismos en los tres. Aquí solo hay cuentas, sin
// nada de Android, para poder probarlas (BordesSistemaTest); quien las usa es MainActivity.
// ============================================================
public final class BordesSistema {

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

    /**
     * Lo que del borde de abajo es macizo: el hueco del sistema entero, sean botones o el asa
     * de gestos. Con el degradado hasta el canto de la pantalla, el asa quedaba encima de texto
     * todavía legible y lo tachaba.
     */
    public static int macizoInferior(int hueco) {
        return hueco;
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
     * El alfa (0–255) de cada fila del borde, de la punta transparente (fila 0) a la maciza.
     *
     * <p>En la rampa sube con el cuadrado de la distancia: despacio junto al contenido y
     * deprisa junto a la barra (a media rampa, un cuarto). El tramo macizo es opaco entero.
     *
     * @param alto   alto del borde, en px.
     * @param macizo lo que de él es macizo, desde el canto de la pantalla, en px.
     */
    public static int[] alfas(int alto, int macizo) {
        int[] alfas = new int[Math.max(0, alto)];
        int rampa = alfas.length - Math.min(Math.max(0, macizo), alfas.length);
        for (int y = 0; y < alfas.length; y++) {
            float t = y >= rampa ? 1f : y / (float) rampa;
            alfas[y] = Math.round(255f * t * t);
        }
        return alfas;
    }
}
