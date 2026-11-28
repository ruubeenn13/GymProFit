package es.pmdm.gymprofit.ui.cristal;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.color.MaterialColors;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.utils.Movimiento;

// ============================================================
// BarraProgresoCristal — una barra de progreso sobre cristal (GP-285).
//
// La pista sobre cristal es translúcida (un surco: gp_cristal_pista), y el indicador de
// Material la pinta por tramos que se pisan en las puntas: donde se pisan, el translúcido se
// suma y salen dos lentejas más oscuras (revisor de UI, 26-11-2026). Aquí la pista es un solo
// rectángulo redondeado y el relleno, otro encima.
//
// Es informativa y va siempre dentro de algo que ya dice el dato con palabras (un tile de «Tu
// día», la fila de una zona): no se anuncia a TalkBack (DEC-019: el nombre lo lleva su grupo).
// El progreso va de 0 a 100 (o, con setFraccion, de 0 a 1 con decimales). La pista sale del tema
// (colorSurfaceVariant) y el relleno, del naranja del tema hasta que se le dé otro, liso o en
// degradado de lado a lado.
// ============================================================
public class BarraProgresoCristal extends View {

    private final Paint pista = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint relleno = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF caja = new RectF();
    private final int grosor;
    private float progreso;
    private boolean conDegradado;
    @ColorInt private int degradadoDesde, degradadoHasta;
    @Nullable private ValueAnimator animador;

    public BarraProgresoCristal(@NonNull Context context) {
        this(context, null);
    }

    public BarraProgresoCristal(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        grosor = getResources().getDimensionPixelSize(R.dimen.tile_barra);
        pista.setColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurfaceVariant));
        relleno.setColor(MaterialColors.getColor(this, androidx.appcompat.R.attr.colorPrimary));
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    /** El color del relleno (el de la macro, el de la zona…). */
    public void setColorRelleno(@ColorInt int color) {
        conDegradado = false;
        relleno.setShader(null);
        relleno.setColor(color);
        invalidate();
    }

    /**
     * Fija el progreso.
     *
     * @param porcentaje de 0 a 100; lo que se salga, se recorta.
     * @param animar     si el tramo se llena desde donde estaba (momento 5 de DEC-039) o salta.
     *                   Con «Quitar animaciones» salta siempre.
     */
    public void setProgreso(int porcentaje, boolean animar) {
        float hasta = Math.max(0, Math.min(100, porcentaje)) / 100f;
        if (animador != null) animador.cancel();
        if (!animar || Movimiento.quieto(getContext())) {
            progreso = hasta;
            invalidate();
            return;
        }
        animador = ValueAnimator.ofFloat(progreso, hasta);
        animador.setDuration(Movimiento.BARRA);
        animador.setInterpolator(Movimiento.ESTANDAR);
        animador.addUpdateListener(a -> {
            progreso = (float) a.getAnimatedValue();
            invalidate();
        });
        animador.start();
    }

    /**
     * Fija el progreso sin animarlo, con decimales: para quien lo mueve fotograma a fotograma
     * (la barra del descanso, que se vacía en lineal: momento 26 de DEC-039).
     *
     * @param parte de 0 a 1; lo que se salga, se recorta.
     */
    public void setFraccion(float parte) {
        if (animador != null) animador.cancel();
        float hasta = Math.max(0f, Math.min(1f, parte));
        if (hasta == progreso) return;
        progreso = hasta;
        invalidate();
    }

    /**
     * El relleno en degradado de lado a lado de la barra (la del descanso: del acento claro al
     * naranja). Con los dos colores iguales es un color liso. El degradado va sobre la barra
     * entera y el relleno enseña su parte: se crea al cambiar de tamaño, nunca al dibujar.
     */
    public void setColoresRelleno(@ColorInt int desde, @ColorInt int hasta) {
        degradadoDesde = desde;
        degradadoHasta = hasta;
        conDegradado = desde != hasta;
        relleno.setColor(desde);
        montarDegradado();
        invalidate();
    }

    private void montarDegradado() {
        float izq = getPaddingLeft(), der = getWidth() - getPaddingRight();
        relleno.setShader(conDegradado && der > izq
                ? new LinearGradient(izq, 0, der, 0, degradadoDesde, degradadoHasta, Shader.TileMode.CLAMP)
                : null);
    }

    @Override
    protected void onSizeChanged(int w, int h, int antesW, int antesH) {
        super.onSizeChanged(w, h, antesW, antesH);
        montarDegradado();
    }

    /** El progreso de ahora, de 0 a 100. */
    public int getProgreso() {
        return Math.round(progreso * 100f);
    }

    @Override
    protected void onMeasure(int anchoSpec, int altoSpec) {
        setMeasuredDimension(getDefaultSize(getSuggestedMinimumWidth(), anchoSpec),
                resolveSize(grosor + getPaddingTop() + getPaddingBottom(), altoSpec));
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        float izq = getPaddingLeft(), der = getWidth() - getPaddingRight();
        float arriba = getPaddingTop() + (getHeight() - getPaddingTop() - getPaddingBottom() - grosor) / 2f;
        float radio = grosor / 2f;
        caja.set(izq, arriba, der, arriba + grosor);
        canvas.drawRoundRect(caja, radio, radio, pista);
        if (progreso <= 0f) return;
        // El relleno crece desde el principio de la lectura y nunca es más corto que su redondeo.
        float largo = Math.max(grosor, (der - izq) * progreso);
        if (getLayoutDirection() == LAYOUT_DIRECTION_RTL) caja.set(der - largo, arriba, der, arriba + grosor);
        else caja.set(izq, arriba, izq + largo, arriba + grosor);
        canvas.drawRoundRect(caja, radio, radio, relleno);
    }

    @Override
    protected void onDetachedFromWindow() {
        if (animador != null) animador.cancel();
        super.onDetachedFromWindow();
    }
}
