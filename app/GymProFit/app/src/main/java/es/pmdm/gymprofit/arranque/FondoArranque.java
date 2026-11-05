package es.pmdm.gymprofit.arranque;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

// ============================================================
// FondoArranque — el fondo de la pantalla de carga, que se abre al salir (GP-223).
//
// Pinta el fondo oscuro entero menos un círculo centrado, de radio 0 mientras se carga.
// Al salir hacia Inicio, el radio crece hasta la esquina más lejana y deja ver Inicio, que
// ya está debajo. Un Path con relleno par-impar (el rectángulo y el círculo) sirve en
// todas las versiones; clipOutPath llega en Android 8.
// ============================================================
public class FondoArranque extends Drawable {

    private final Paint pintura = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path forma = new Path();
    /** Lo abierto, de 0 (cerrado) a 1 (el círculo llega a las esquinas). */
    private float apertura;

    public FondoArranque(@ColorInt int color) {
        pintura.setColor(color);
        forma.setFillType(Path.FillType.EVEN_ODD);
    }

    /** Cuánto se ha abierto el fondo, de 0 a 1. */
    public void setApertura(float apertura) {
        this.apertura = apertura;
        invalidateSelf();
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        Rect b = getBounds();
        if (apertura <= 0f) {
            canvas.drawRect(b, pintura);
            return;
        }
        float cx = b.exactCenterX(), cy = b.exactCenterY();
        float radio = (float) Math.hypot(b.width() / 2f, b.height() / 2f) * apertura;
        forma.reset();
        forma.addRect(b.left, b.top, b.right, b.bottom, Path.Direction.CW);
        forma.addCircle(cx, cy, radio, Path.Direction.CW);
        canvas.drawPath(forma, pintura);
    }

    @Override public void setAlpha(int alpha) { pintura.setAlpha(alpha); invalidateSelf(); }
    @Override public void setColorFilter(@Nullable ColorFilter cf) { pintura.setColorFilter(cf); invalidateSelf(); }
    @Override public int getOpacity() { return apertura > 0f ? PixelFormat.TRANSLUCENT : PixelFormat.OPAQUE; }
}
