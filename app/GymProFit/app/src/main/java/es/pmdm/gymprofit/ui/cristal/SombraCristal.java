package es.pmdm.gymprofit.ui.cristal;

import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

// ============================================================
// SombraCristal — la sombra suave del cristal estático (GP-285, lote 1b; solo el tema claro).
//
// El cristal es translúcido: una sombra debajo se vería a través y lo ensuciaría. Esta solo
// existe por fuera de la forma, como la de CSS (box-shadow). La técnica es la de la sombra del
// cristal vivo (944a604): una silueta difuminada en una imagen de un solo canal, teñida al
// pintar. Con dos diferencias, porque aquí el cristal sí deja ver lo de debajo en todas las
// versiones de Android:
//
//   · El hueco de la forma va ya vaciado en la imagen. No se recorta al dibujar: antes de
//     Android 8 el recorte «todo menos la forma» parte de la caja de la vista y dejaba la
//     sombra cortada en recto.
//   · Es una imagen pequeña, a tamaño real, que se pinta en ocho trozos alrededor de la
//     tarjeta (las cuatro esquinas tal cual y los cuatro lados estirados): una por radio de
//     esquina, compartida por todas las tarjetas, sin depender del tamaño de cada una.
//
// Quien la pinta dibuja fuera de su caja: su padre no puede recortar a sus hijos.
// ============================================================
public final class SombraCristal {

    // Una imagen por medidas: las comparten todas las tarjetas del mismo radio.
    private static final Map<String, SombraCristal> HECHAS = new HashMap<>();
    // Lado del tramo recto que se estira, en px.
    private static final int RECTO = 2;

    private final Bitmap imagen;
    // Lo que la sombra sobresale de la forma por cada lado, y el radio de la esquina, en px.
    private final int margen, esquina;
    private final Paint pintura = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Rect origen = new Rect();
    private final RectF destino = new RectF();

    /**
     * La sombra de una forma redondeada con esas medidas (en px).
     *
     * @param radio      radio de las esquinas de la forma.
     * @param difuminado el «blur» de CSS: dos veces la desviación del difuminado.
     * @param baja       lo que la sombra cae hacia abajo.
     */
    @NonNull
    public static SombraCristal de(float radio, float difuminado, float baja) {
        String clave = Math.round(radio) + "/" + Math.round(difuminado) + "/" + Math.round(baja);
        SombraCristal s = HECHAS.get(clave);
        if (s == null) {
            s = new SombraCristal(Math.round(radio), difuminado, baja);
            HECHAS.put(clave, s);
        }
        return s;
    }

    /** Lo que una sombra de esas medidas sobresale de su forma, en px: tres desviaciones y la caída. */
    public static int alcance(float difuminado, float baja) {
        return (int) Math.ceil(difuminado * 1.5f + Math.abs(baja));
    }

    private SombraCristal(int radio, float difuminado, float baja) {
        esquina = Math.max(1, radio);
        margen = alcance(difuminado, baja);
        int nucleo = esquina * 2 + RECTO, lado = nucleo + margen * 2;
        imagen = Bitmap.createBitmap(lado, lado, Bitmap.Config.ALPHA_8);
        Canvas c = new Canvas(imagen);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        // BlurMaskFilter pide un «radio», del que saca σ = radio · 0,57735 + 0,5; CSS da 2σ.
        float sigma = difuminado / 2f;
        p.setMaskFilter(new BlurMaskFilter(Math.max(0.1f, (sigma - 0.5f) / 0.57735f), BlurMaskFilter.Blur.NORMAL));
        c.drawRoundRect(margen, margen + baja, margen + nucleo, margen + baja + nucleo, esquina, esquina, p);
        // El hueco: la forma en su sitio (sin la caída), vaciada. Bajo el cristal no hay sombra.
        Paint hueco = new Paint(Paint.ANTI_ALIAS_FLAG);
        hueco.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        c.drawRoundRect(margen, margen, margen + nucleo, margen + nucleo, esquina, esquina, hueco);
    }

    /**
     * Pinta la sombra alrededor de una forma de ese tamaño con su esquina superior izquierda
     * en el origen del lienzo.
     *
     * @param color el color de la sombra, con su alfa.
     */
    public void pintar(@NonNull Canvas canvas, float ancho, float alto, @ColorInt int color) {
        if (color >>> 24 == 0 || ancho <= 0 || alto <= 0) return;
        pintura.setColor(color);
        // Una forma más pequeña que dos esquinas: se encoge la esquina de destino.
        float ex = Math.min(esquina, ancho / 2f), ey = Math.min(esquina, alto / 2f);
        int a = margen + esquina, b = a + RECTO, fin = imagen.getWidth();
        int[] cortes = {0, a, b, fin};
        float[] xs = {-margen, ex, ancho - ex, ancho + margen};
        float[] ys = {-margen, ey, alto - ey, alto + margen};
        for (int fila = 0; fila < 3; fila++) {
            for (int col = 0; col < 3; col++) {
                // El centro es el hueco: no hay nada que pintar.
                if (fila == 1 && col == 1) continue;
                if (xs[col + 1] <= xs[col] || ys[fila + 1] <= ys[fila]) continue;
                origen.set(cortes[col], cortes[fila], cortes[col + 1], cortes[fila + 1]);
                destino.set(xs[col], ys[fila], xs[col + 1], ys[fila + 1]);
                canvas.drawBitmap(imagen, origen, destino, pintura);
            }
        }
    }
}
