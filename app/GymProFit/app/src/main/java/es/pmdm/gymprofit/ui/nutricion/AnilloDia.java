package es.pmdm.gymprofit.ui.nutricion;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.utils.Movimiento;

// ============================================================
// AnilloDia — el anillo de las kcal del día en Nutrición (lote 1.6.8, tableros 1 y 3)
//
// Un aro de 12 dp sobre su pista: en naranja, lo comido frente al objetivo (sin pasar de
// la vuelta entera); con el día completo, entero y en verde, sin mirar las calorías
// (decisión 10: sin clavarlas). Nunca rojo.
//
// Movimiento: al entrar se dibuja desde 0 con las cifras (momento 7, CIFRAS); al cambiar,
// pasa a su valor con BARRA; al completarse el día mientras se mira, se cierra y se pone
// verde (CIERRA, estándar) y suelta un halo verde (HALO, momento 21). Con «Quitar
// animaciones», todo en su sitio.
// Es dibujo: lo que dice lo dice la vista que lo contiene (DEC-019).
// ============================================================
public class AnilloDia extends View {

    private final Paint pista = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arco = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint halo = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF caja = new RectF();
    private final float grosor;
    private final int naranja;
    private final int verde;

    // Lo dibujado de la vuelta (0..1), el color del aro y el halo (0..1 de su vida; -1, sin).
    private float fraccion;
    private int color;
    private float vidaHalo = -1f;
    @Nullable private ValueAnimator cambio;
    @Nullable private ValueAnimator animHalo;

    public AnilloDia(@NonNull Context context) {
        this(context, null);
    }

    public AnilloDia(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        grosor = 12 * getResources().getDisplayMetrics().density;
        naranja = ContextCompat.getColor(context, R.color.gp_primary);
        verde = ContextCompat.getColor(context, R.color.gp_success);
        color = naranja;
        pista.setStyle(Paint.Style.STROKE);
        pista.setStrokeWidth(grosor);
        pista.setColor(ContextCompat.getColor(context, R.color.gp_meter_track));
        arco.setStyle(Paint.Style.STROKE);
        arco.setStrokeWidth(grosor);
        arco.setStrokeCap(Paint.Cap.ROUND);
        halo.setStyle(Paint.Style.FILL);
    }

    /**
     * Pone el anillo en su valor.
     *
     * @param nueva    parte de la vuelta, 0..1 (lo que pase de 1 se queda en 1).
     * @param completo si el día está completo: entero y verde.
     * @param modo     cómo llega: de golpe, contando desde 0 o pasando desde lo que había.
     */
    public void poner(float nueva, boolean completo, @NonNull Modo modo) {
        float destino = completo ? 1f : Math.max(0f, Math.min(1f, nueva));
        int colorDestino = completo ? verde : naranja;
        if (cambio != null) cambio.cancel();
        if (modo == Modo.QUIETO || Movimiento.quieto(getContext())) {
            fraccion = destino;
            color = colorDestino;
            invalidate();
            return;
        }
        float desde = modo == Modo.CONTAR ? 0f : fraccion;
        int colorDesde = color;
        long duracion = modo == Modo.CONTAR ? Movimiento.CIFRAS : modo == Modo.CERRAR ? Movimiento.CIERRA : Movimiento.BARRA;
        ValueAnimator a = ValueAnimator.ofFloat(0f, 1f);
        a.setDuration(duracion);
        // Contar va con la curva de las cifras, para que el aro y el número lleguen juntos.
        a.setInterpolator(modo == Modo.CONTAR ? Movimiento.LINEAL : Movimiento.ESTANDAR);
        ArgbEvaluator tinte = new ArgbEvaluator();
        a.addUpdateListener(an -> {
            float t = (float) an.getAnimatedValue();
            if (modo == Modo.CONTAR) t = (float) (1 - Math.pow(1 - t, 3));
            fraccion = desde + (destino - desde) * t;
            color = (int) tinte.evaluate(t, colorDesde, colorDestino);
            invalidate();
        });
        a.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationCancel(Animator animation) {
                fraccion = destino;
                color = colorDestino;
                invalidate();
            }
        });
        cambio = a;
        a.start();
    }

    /** El halo verde del día completo (momento 21), una vez. Con «Quitar animaciones», nada. */
    public void soltarHalo(long retraso) {
        if (Movimiento.quieto(getContext())) return;
        if (animHalo != null) animHalo.cancel();
        ValueAnimator a = ValueAnimator.ofFloat(0f, 1f);
        a.setDuration(Movimiento.HALO);
        a.setStartDelay(retraso);
        a.setInterpolator(Movimiento.ESTANDAR);
        a.addUpdateListener(an -> {
            vidaHalo = (float) an.getAnimatedValue();
            invalidate();
        });
        a.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                vidaHalo = -1f;
                invalidate();
            }
        });
        animHalo = a;
        a.start();
    }

    /** Cómo llega el anillo a su valor. */
    public enum Modo { QUIETO, CONTAR, PASAR, CERRAR }

    @Override
    protected void onDetachedFromWindow() {
        if (cambio != null) cambio.cancel();
        if (animHalo != null) animHalo.cancel();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        float lado = Math.min(getWidth(), getHeight());
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float radio = (lado - grosor) / 2f;

        if (vidaHalo >= 0f) {
            // Crece de 0,8 a 1,3 del aro y se apaga: el aura de DiaCompleto.dc.html.
            float escala = 0.8f + 0.5f * vidaHalo;
            float alfa = vidaHalo < 0.3f ? vidaHalo / 0.3f : 1f - (vidaHalo - 0.3f) / 0.7f;
            float r = (lado / 2f) * escala;
            int centro = (Math.round(0.30f * 255 * alfa) << 24) | (verde & 0x00FFFFFF);
            halo.setShader(new RadialGradient(cx, cy, Math.max(1f, r), centro, verde & 0x00FFFFFF, Shader.TileMode.CLAMP));
            canvas.drawCircle(cx, cy, r, halo);
        }

        caja.set(cx - radio, cy - radio, cx + radio, cy + radio);
        canvas.drawArc(caja, 0, 360, false, pista);
        if (fraccion <= 0f) return;
        arco.setColor(color);
        canvas.drawArc(caja, -90, 360 * fraccion, false, arco);
    }
}
