package es.pmdm.gymprofit.ui.nutricion;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.utils.Movimiento;

// ============================================================
// MarcoEscaner — el marco del escáner con sus cuatro esquinas y la línea (lote 1.6.1)
//
// Dibuja las esquinas (36 dp, trazo de 4 dp, radio 16) y, mientras busca, una línea
// naranja que barre el marco de arriba abajo y vuelve (BARRIDO). Al leer un código, las
// esquinas pasan a verde y el marco encaja de 1,08 a 1 (ENCAJA): momento 17 de DEC-039.
// Con «Quitar animaciones» (Movimiento.quieto) la línea no se mueve: se queda quieta
// arriba, y el marco cambia de color sin encajar.
// Es decoración: TalkBack no la lee; el estado lo dice el aviso de encima.
// ============================================================
public class MarcoEscaner extends View {

    private final Paint esquinas = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linea = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path camino = new Path();
    private final RectF caja = new RectF();
    private final float dp;
    private final int blanco;
    private final int verde;

    private boolean leido;
    private float posLinea;
    @Nullable private ValueAnimator barrido;

    public MarcoEscaner(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        dp = getResources().getDisplayMetrics().density;
        blanco = ContextCompat.getColor(context, R.color.gp_escaner_texto);
        verde = ContextCompat.getColor(context, R.color.gp_escaner_leido);
        esquinas.setStyle(Paint.Style.STROKE);
        esquinas.setStrokeWidth(4 * dp);
        esquinas.setStrokeCap(Paint.Cap.ROUND);
        esquinas.setColor(blanco);
        linea.setStyle(Paint.Style.FILL);
        linea.setColor(ContextCompat.getColor(context, R.color.gp_primary));
        linea.setShadowLayer(10 * dp, 0, 0, ContextCompat.getColor(context, R.color.gp_primary));
        setLayerType(LAYER_TYPE_SOFTWARE, linea);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    /** Buscando: esquinas blancas y la línea que barre. */
    public void buscar() {
        leido = false;
        esquinas.setColor(blanco);
        empezarBarrido();
        invalidate();
    }

    /** Código leído: esquinas en verde, el marco encaja y la línea se va. */
    public void leer() {
        leido = true;
        pararBarrido();
        esquinas.setColor(verde);
        if (!Movimiento.quieto(getContext())) {
            setScaleX(1.08f);
            setScaleY(1.08f);
            animate().scaleX(1f).scaleY(1f).setDuration(Movimiento.ENCAJA).setInterpolator(Movimiento.ESTANDAR).start();
        }
        invalidate();
    }

    private void empezarBarrido() {
        pararBarrido();
        if (Movimiento.quieto(getContext())) {
            posLinea = 0f;
            return;
        }
        barrido = ValueAnimator.ofFloat(0f, 1f, 0f);
        barrido.setDuration(Movimiento.BARRIDO);
        barrido.setRepeatCount(ValueAnimator.INFINITE);
        barrido.setInterpolator(Movimiento.ESTANDAR);
        barrido.addUpdateListener(a -> {
            posLinea = (float) a.getAnimatedValue();
            invalidate();
        });
        barrido.start();
    }

    private void pararBarrido() {
        if (barrido != null) barrido.cancel();
        barrido = null;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!leido) empezarBarrido();
    }

    @Override
    protected void onDetachedFromWindow() {
        pararBarrido();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        float m = 2 * dp;
        caja.set(m, m, getWidth() - m, getHeight() - m);
        float largo = 36 * dp;
        float r = 16 * dp;
        camino.reset();
        // Arriba a la izquierda.
        camino.moveTo(caja.left, caja.top + largo);
        camino.lineTo(caja.left, caja.top + r);
        camino.quadTo(caja.left, caja.top, caja.left + r, caja.top);
        camino.lineTo(caja.left + largo, caja.top);
        // Arriba a la derecha.
        camino.moveTo(caja.right - largo, caja.top);
        camino.lineTo(caja.right - r, caja.top);
        camino.quadTo(caja.right, caja.top, caja.right, caja.top + r);
        camino.lineTo(caja.right, caja.top + largo);
        // Abajo a la derecha.
        camino.moveTo(caja.right, caja.bottom - largo);
        camino.lineTo(caja.right, caja.bottom - r);
        camino.quadTo(caja.right, caja.bottom, caja.right - r, caja.bottom);
        camino.lineTo(caja.right - largo, caja.bottom);
        // Abajo a la izquierda.
        camino.moveTo(caja.left + largo, caja.bottom);
        camino.lineTo(caja.left + r, caja.bottom);
        camino.quadTo(caja.left, caja.bottom, caja.left, caja.bottom - r);
        camino.lineTo(caja.left, caja.bottom - largo);
        canvas.drawPath(camino, esquinas);

        if (!leido) {
            float arriba = caja.top + 14 * dp;
            float abajo = caja.bottom - 16 * dp;
            float y = arriba + (abajo - arriba) * posLinea;
            canvas.drawRoundRect(caja.left + 16 * dp, y, caja.right - 16 * dp, y + 2 * dp, dp, dp, linea);
        }
    }
}
