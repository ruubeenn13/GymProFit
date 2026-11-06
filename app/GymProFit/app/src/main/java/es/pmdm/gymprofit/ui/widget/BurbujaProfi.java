package es.pmdm.gymprofit.ui.widget;

import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import org.xmlpull.v1.XmlPullParser;

import es.pmdm.gymprofit.R;

// ============================================================
// BurbujaProfi — el bocadillo de Profi con su cola y su borde (GP-225, tablero de Inicio).
//
// Antes era un rectángulo con la esquina de arriba más cerrada: sin cola y sin borde, en claro
// el bocadillo blanco se perdía contra el fondo. Ahora: la cola a la izquierda, a media altura
// (apunta a la cara de Profi, que va centrada al lado), el cuerpo de radio 16 y el borde de la
// tarjeta (1 dp en claro, ninguno en oscuro: tarjeta_borde vale 0 en values-night).
//
// Se declara en res/drawable/bg_burbuja_profi.xml con <drawable class=…> (Android 7 o más, el
// mínimo de la app). La cola ocupa los primeros cola_burbuja dp del ancho: la vista lleva ese
// tanto más de relleno a la izquierda y ese tanto menos de margen.
// ============================================================
public class BurbujaProfi extends Drawable {

    private final Paint relleno = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borde = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path forma = new Path();
    private float cola, colaAlto, radio;

    public BurbujaProfi() {
        relleno.setStyle(Paint.Style.FILL);
        borde.setStyle(Paint.Style.STROKE);
    }

    @Override
    public void inflate(@NonNull Resources r, @NonNull XmlPullParser parser, @NonNull AttributeSet attrs,
                        @Nullable Resources.Theme theme) throws java.io.IOException,
            org.xmlpull.v1.XmlPullParserException {
        super.inflate(r, parser, attrs, theme);
        cola = r.getDimension(R.dimen.cola_burbuja);
        colaAlto = r.getDimension(R.dimen.cola_burbuja_alto);
        radio = r.getDimension(R.dimen.radio_miniatura);
        borde.setStrokeWidth(r.getDimension(R.dimen.tarjeta_borde));
        borde.setColor(ResourcesCompat.getColor(r, R.color.gp_tarjeta_borde, theme));
        relleno.setColor(ResourcesCompat.getColor(r, R.color.gp_surface, theme));
        if (theme != null) ponerTema(theme);
    }

    // El tema de la pantalla llega aquí (y no siempre a inflate): la superficie es colorSurface,
    // que con la app en oscuro no es la del sistema.
    @Override
    public boolean canApplyTheme() {
        return true;
    }

    @Override
    public void applyTheme(@NonNull Resources.Theme theme) {
        super.applyTheme(theme);
        ponerTema(theme);
    }

    private void ponerTema(@NonNull Resources.Theme theme) {
        TypedArray a = theme.obtainStyledAttributes(new int[]{ com.google.android.material.R.attr.colorSurface });
        if (a.hasValue(0)) relleno.setColor(a.getColor(0, relleno.getColor()));
        a.recycle();
        invalidateSelf();
    }

    @Override
    protected void onBoundsChange(@NonNull Rect b) {
        super.onBoundsChange(b);
        float medio = borde.getStrokeWidth() / 2f;
        RectF cuerpo = new RectF(b.left + cola, b.top + medio, b.right - medio, b.bottom - medio);
        forma.reset();
        forma.addRoundRect(cuerpo, radio, radio, Path.Direction.CW);
        Path pico = new Path();
        float cy = b.exactCenterY();
        // La base entra un poco en el cuerpo para que la unión no deje un corte.
        pico.moveTo(b.left + cola + radio / 2f, cy - colaAlto / 2f);
        pico.lineTo(b.left + medio, cy);
        pico.lineTo(b.left + cola + radio / 2f, cy + colaAlto / 2f);
        pico.close();
        forma.op(pico, Path.Op.UNION);
    }

    @Override
    public void draw(@NonNull Canvas c) {
        c.drawPath(forma, relleno);
        if (borde.getStrokeWidth() > 0) c.drawPath(forma, borde);
    }

    @Override
    public void getOutline(@NonNull Outline outline) {
        Rect b = getBounds();
        outline.setRoundRect((int) (b.left + cola), b.top, b.right, b.bottom, radio);
    }

    @Override public void setAlpha(int alpha) { relleno.setAlpha(alpha); borde.setAlpha(alpha); invalidateSelf(); }
    @Override public void setColorFilter(@Nullable ColorFilter f) { relleno.setColorFilter(f); borde.setColorFilter(f); invalidateSelf(); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
