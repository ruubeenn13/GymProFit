package es.pmdm.gymprofit.ui.cristal;

import androidx.annotation.NonNull;

// ============================================================
// Ambiente — el fondo ambiental en números (GP-285, DEC-070).
//
// El fondo de las pantallas Liquid Glass es una base casi negra (clara y cálida en el tema
// claro) con brillos radiales difusos. Aquí está su geometría y la cuenta entera: mezclaEn()
// da el color exacto de un punto, pintar() lo lleva a píxeles y sobre() compone una capa
// translúcida encima, que es como ContrasteCristalTest mide el texto sobre «el punto más
// desfavorable de fondo + brillo + cristal». Lo que se pinta y lo que se mide salen de aquí.
//
// Cada brillo es una elipse: su centro en fracción del ancho y del alto de la pantalla, sus
// radios en fracción del lado corto, y su color con el alfa del centro. Se apaga al 70 % del
// radio (el «transparent 70%» de la maqueta), pero no en línea recta: con una curva plana en
// el centro y plana al apagarse. La recta acababa en seco y el ojo veía ahí el canto de una
// mancha (lote 1b).
//
// pintar() calcula el fondo a resolución completa y con tramado propio: un píxel de 8 bits no
// lleva decimales, y un degradado tan largo sobre casi negro, redondeado sin más, se ve a
// escalones. Los degradados de la GPU traman cada uno a la manera de su versión de Android
// (en Android 7, un damero con saltos de hasta 10 niveles entre vecinos); este es el mismo en
// todas: menos de un nivel, repartido para que la media de cada zona sea la exacta.
//
// No toca Android (los colores son enteros ARGB): se prueba sola (AmbienteTest).
// ============================================================
public final class Ambiente {

    private Ambiente() {}

    /** Parte del radio en la que el brillo ya es transparente. */
    public static final float FIN = 0.70f;

    // Cada cuántos píxeles se hace la cuenta exacta al pintar; entre medias se interpola. El
    // fondo cambia tan despacio que en 8 px la diferencia con la cuenta exacta no llega a una
    // centésima de nivel, y pintar una pantalla entera cuesta 64 veces menos raíces.
    private static final int PASO = 8;

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
     * altura, el tercero abajo a la derecha y el cuarto abajo a la izquierda. Las medidas de
     * los tres primeros son las de la maqueta de 390 × 844 (elipses de 420 × 320, 360 × 300 y
     * 380 × 320); el cuarto solo lo usa el tema claro (en oscuro es transparente).
     */
    @NonNull
    public static Brillo[] inicio(int primero, int segundo, int tercero, int cuarto) {
        return new Brillo[]{
                new Brillo(0.90f, 0.08f, 420f / 390f, 320f / 390f, primero),
                new Brillo(0.00f, 0.42f, 360f / 390f, 300f / 390f, segundo),
                new Brillo(0.85f, 0.78f, 380f / 390f, 320f / 390f, tercero),
                new Brillo(0.06f, 0.88f, 360f / 390f, 300f / 390f, cuarto),
        };
    }

    /**
     * La mezcla de la Sesión en vivo (lote 2): naranja en la esquina de arriba a la derecha,
     * dorado a la izquierda algo por debajo de la mitad y azul abajo a la derecha. Las medidas
     * son las de su maqueta de 390 × 844 (elipses de 380 × 300, 340 × 300 y 360 × 300).
     */
    @NonNull
    public static Brillo[] sesion(int naranja, int dorado, int azul) {
        return new Brillo[]{
                new Brillo(1.00f, 0.00f, 380f / 390f, 300f / 390f, naranja),
                new Brillo(0.00f, 0.60f, 340f / 390f, 300f / 390f, dorado),
                new Brillo(0.90f, 0.95f, 360f / 390f, 300f / 390f, azul),
        };
    }

    /** El alfa (0 a 1) de un brillo en un punto de una pantalla de ese tamaño. */
    public static float alfaEn(@NonNull Brillo b, float x, float y, float ancho, float alto) {
        float lado = Math.min(ancho, alto);
        float dx = (x - b.cx * ancho) / (b.rx * lado);
        float dy = (y - b.cy * alto) / (b.ry * lado);
        float t = (float) Math.sqrt(dx * dx + dy * dy) / FIN;
        if (t >= 1f) return 0f;
        // De 1 en el centro a 0 en el borde, sin esquinas: pendiente cero en los dos extremos.
        return (b.color >>> 24) / 255f * (1f - t * t * (3f - 2f * t));
    }

    /**
     * El color exacto del fondo en un punto, sin redondear: la base con cada brillo compuesto
     * encima, en orden (mezcla normal, en sRGB sin linealizar, como la pantalla).
     *
     * @param rgb donde se dejan rojo, verde y azul, de 0 a 255.
     */
    public static void mezclaEn(float x, float y, float ancho, float alto, int base, @NonNull Brillo[] brillos,
                                @NonNull float[] rgb) {
        rgb[0] = (base >> 16) & 0xFF;
        rgb[1] = (base >> 8) & 0xFF;
        rgb[2] = base & 0xFF;
        for (Brillo b : brillos) {
            if (b.color >>> 24 == 0) continue;
            float a = alfaEn(b, x, y, ancho, alto);
            if (a <= 0f) continue;
            rgb[0] += (((b.color >> 16) & 0xFF) - rgb[0]) * a;
            rgb[1] += (((b.color >> 8) & 0xFF) - rgb[1]) * a;
            rgb[2] += ((b.color & 0xFF) - rgb[2]) * a;
        }
    }

    /** El color opaco del fondo en un punto: el de {@link #mezclaEn}, redondeado. */
    public static int colorEn(float x, float y, float ancho, float alto, int base, @NonNull Brillo[] brillos) {
        float[] rgb = new float[3];
        mezclaEn(x, y, ancho, alto, base, brillos, rgb);
        return 0xFF000000 | (Math.round(rgb[0]) << 16) | (Math.round(rgb[1]) << 8) | Math.round(rgb[2]);
    }

    /**
     * Pinta unas filas del fondo, a resolución completa y con tramado.
     *
     * <p>Cada canal se redondea hacia abajo tras sumarle un umbral de 0 a 1 que cambia de píxel
     * a píxel (ruido de gradiente entrelazado): la media de cualquier zona es la exacta, y el
     * tramado nunca aparta un píxel más de un nivel de su valor.
     *
     * <p>El bucle de dentro va en enteros (coma fija de 16 bits) y sin llamadas: es una pantalla
     * entera de píxeles y se pinta en el hilo de la interfaz, una vez por tamaño.
     *
     * @param px    donde se pinta, ARGB opaco: {@code filas} filas de {@code ancho}, una tras otra.
     * @param ancho ancho del fondo, en px.
     * @param alto  alto del fondo entero, en px.
     * @param desde primera fila que se pinta.
     * @param filas cuántas.
     */
    public static void pintar(@NonNull int[] px, int ancho, int alto, int desde, int filas, int base,
                              @NonNull Brillo[] brillos) {
        int columnas = (ancho + PASO - 1) / PASO + 1;
        float[] arriba = new float[columnas * 3], abajo = new float[columnas * 3];
        int[] fila = new int[columnas * 3];
        int[] umbrales = UMBRALES;
        int celda = Integer.MIN_VALUE;
        for (int n = 0; n < filas; n++) {
            int y = desde + n, c = y / PASO;
            if (c != celda) {
                // Al pasar a la celda siguiente, su fila de arriba es la de abajo de la anterior.
                if (c == celda + 1) {
                    float[] t = arriba;
                    arriba = abajo;
                    abajo = t;
                } else {
                    filaExacta(arriba, columnas, c * PASO, ancho, alto, base, brillos);
                }
                filaExacta(abajo, columnas, (c + 1) * PASO, ancho, alto, base, brillos);
                celda = c;
            }
            float fy = (y - c * PASO) / (float) PASO;
            for (int i = 0; i < fila.length; i++) fila[i] = (int) ((arriba[i] + (abajo[i] - arriba[i]) * fy) * UNO);
            int p = n * ancho, filaUmbral = (y & (LADO_UMBRALES - 1)) * LADO_UMBRALES;
            for (int col = 0, x = 0; x < ancho; col++) {
                int i = col * 3, fin = Math.min(ancho, x + PASO);
                // De un punto exacto al siguiente, en PASO pasos iguales.
                int r = fila[i], g = fila[i + 1], b = fila[i + 2];
                int dr = (fila[i + 3] - r) / PASO, dg = (fila[i + 4] - g) / PASO, db = (fila[i + 5] - b) / PASO;
                for (; x < fin; x++) {
                    int u = filaUmbral + (x & (LADO_UMBRALES - 1));
                    // El mismo dibujo en los tres canales, desplazado: el grano no tiene color propio.
                    px[p + x] = 0xFF000000
                            | ((r + umbrales[u]) >> 16) << 16
                            | ((g + umbrales[(u + SALTO_VERDE) & (umbrales.length - 1)]) >> 16) << 8
                            | (b + umbrales[(u + SALTO_AZUL) & (umbrales.length - 1)]) >> 16;
                    r += dr;
                    g += dg;
                    b += db;
                }
            }
        }
    }

    // Coma fija: un nivel de color son 65.536 unidades.
    private static final int UNO = 1 << 16;
    // Los umbrales del tramado, en una baldosa de 128 × 128 que se repite, y lo que se desplaza
    // el dibujo en el verde y en el azul.
    private static final int LADO_UMBRALES = 128, SALTO_VERDE = 5003, SALTO_AZUL = 9001;
    private static final int[] UMBRALES = umbrales();

    // Ruido de gradiente entrelazado: reparte los umbrales de forma que vecinos y zonas pequeñas
    // los llevan todos, sin el dibujo regular de un tramado ordenado. De casi 0 a casi 1: no
    // toca ninguno de los dos para que un valor entero (el fondo base, lejos de todo brillo) no
    // se mueva un nivel por un error de redondeo.
    private static int[] umbrales() {
        int[] t = new int[LADO_UMBRALES * LADO_UMBRALES];
        for (int y = 0; y < LADO_UMBRALES; y++) {
            for (int x = 0; x < LADO_UMBRALES; x++) {
                double u = 52.9829189 * fraccion(0.06711056 * x + 0.00583715 * y);
                t[y * LADO_UMBRALES + x] = (int) ((0.0005 + fraccion(u) * 0.999) * UNO);
            }
        }
        return t;
    }

    // El color exacto de una fila, un punto cada PASO píxeles.
    private static void filaExacta(float[] destino, int columnas, int y, int ancho, int alto, int base,
                                   Brillo[] brillos) {
        float[] rgb = new float[3];
        for (int c = 0; c < columnas; c++) {
            mezclaEn(c * PASO, y, ancho, alto, base, brillos, rgb);
            destino[c * 3] = rgb[0];
            destino[c * 3 + 1] = rgb[1];
            destino[c * 3 + 2] = rgb[2];
        }
    }

    private static double fraccion(double v) {
        return v - Math.floor(v);
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
