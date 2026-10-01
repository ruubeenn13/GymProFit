package es.pmdm.gymprofit.ui.alta;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import es.pmdm.gymprofit.utils.Movimiento;

// ============================================================
// CheckTrazado — el check de «Todo listo» (GP-103; GP-104, momento 9).
//
// Un círculo naranja con el check oscuro encima, como el SVG del lienzo (112 dp, trazo
// de 9 y extremos redondos). Al mostrarse, el círculo salta (420 ms con rebote) y el
// check se traza (420 ms con la curva estándar, 250 ms después). Con «Quitar
// animaciones», ya trazado.
//
// Es decoración: «Todo listo» lo dice el título, así que TalkBack no lo lee (DEC-019).
// ============================================================
public class CheckTrazado extends View {

    private final Paint circulo = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint trazo = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path check = new Path();
    private final Path parcial = new Path();
    private PathMeasure medida;
    private float progreso = 1f;

    public CheckTrazado(@NonNull Context context) {
        this(context, null);
    }

    public CheckTrazado(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        circulo.setColor(color(androidx.appcompat.R.attr.colorPrimary));
        trazo.setColor(color(com.google.android.material.R.attr.colorOnPrimary));
        trazo.setStyle(Paint.Style.STROKE);
        trazo.setStrokeCap(Paint.Cap.ROUND);
        trazo.setStrokeJoin(Paint.Join.ROUND);
    }

    /** Salta y traza el check. */
    public void mostrar() {
        setScaleX(0.4f);
        setScaleY(0.4f);
        setAlpha(0f);
        animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(Movimiento.TRAZO)
                .setInterpolator(Movimiento.REBOTE).start();
        progreso = 0f;
        invalidate();
        ValueAnimator a = ValueAnimator.ofFloat(0f, 1f);
        a.setStartDelay(250);
        a.setDuration(Movimiento.TRAZO);
        a.setInterpolator(Movimiento.ESTANDAR);
        a.addUpdateListener(an -> {
            progreso = (float) an.getAnimatedValue();
            invalidate();
        });
        a.start();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // Las coordenadas del lienzo, en una caja de 112.
        float e = Math.min(w, h) / 112f;
        check.reset();
        check.moveTo(34 * e, 57 * e);
        check.lineTo(50 * e, 73 * e);
        check.lineTo(80 * e, 41 * e);
        trazo.setStrokeWidth(9 * e);
        medida = new PathMeasure(check, false);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        float r = Math.min(getWidth(), getHeight()) / 2f;
        canvas.drawCircle(getWidth() / 2f, getHeight() / 2f, r, circulo);
        if (medida == null || progreso <= 0f) return;
        parcial.reset();
        medida.getSegment(0, medida.getLength() * progreso, parcial, true);
        canvas.drawPath(parcial, trazo);
    }

    private int color(int attr) {
        TypedValue tv = new TypedValue();
        getContext().getTheme().resolveAttribute(attr, tv, true);
        return tv.data;
    }
}
