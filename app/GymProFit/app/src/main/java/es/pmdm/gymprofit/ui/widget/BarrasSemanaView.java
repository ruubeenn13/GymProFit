package es.pmdm.gymprofit.ui.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import es.pmdm.gymprofit.utils.Movimiento;

// ============================================================
// BarrasSemanaView — las siete barras de «Tu semana de nutrición» (tablero 12, GP-210 paso 1).
//
// Una barra por día, de lunes a domingo, con una raya discontinua en el objetivo y, debajo,
// la letra del día. En la proteína, un trazo naranja bajo la letra marca el día que se
// entrenó; las barras que llegan al objetivo van en dorado (es de lo conseguido, DEC-018).
// Las barras crecen desde abajo, una tras otra (BARRA, escalón DIA_ESCALON); con «Quitar
// animaciones» salen ya hechas.
//
// Vista dibujada a mano (DEC-019): no es ningún control y no se enfoca por dentro, así que
// lleva UNA descripción para TalkBack con los siete valores; la pone quien la usa.
// Un día sin datos no pinta barra, solo su letra.
// ============================================================
public class BarrasSemanaView extends View {

    private static final int DIAS = 7;

    private final Paint barra = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint raya = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint texto = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint marca = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path camino = new Path();
    private final RectF rect = new RectF();

    private final float[] valores = new float[DIAS];
    private final boolean[] hay = new boolean[DIAS];
    private final boolean[] entreno = new boolean[DIAS];
    private final int[] colores = new int[DIAS];
    private final String[] letras = new String[DIAS];
    private float tope = 1f;
    private float objetivo = 0f;
    @Nullable private String rotuloObjetivo;
    private float radioArriba = 8f, radioAbajo = 4f;
    private float alturaBarras;
    private boolean conMarcaEntreno;
    private float progreso = 1f;
    @Nullable private ValueAnimator animador;

    @ColorInt private int colorObjetivo;
    @ColorInt private int colorTexto;
    @ColorInt private int colorEntreno;

    public BarrasSemanaView(Context context) { this(context, null); }

    public BarrasSemanaView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        colorObjetivo = tema(com.google.android.material.R.attr.colorOutline);
        colorTexto = tema(com.google.android.material.R.attr.colorOnSurfaceVariant);
        colorEntreno = tema(androidx.appcompat.R.attr.colorPrimary);
        raya.setStyle(Paint.Style.STROKE);
        raya.setStrokeWidth(dp(2));
        raya.setColor(colorObjetivo);
        raya.setPathEffect(new DashPathEffect(new float[]{dp(6), dp(5)}, 0));
        texto.setTextAlign(Paint.Align.CENTER);
        texto.setColor(colorTexto);
        marca.setColor(colorEntreno);
        for (int i = 0; i < DIAS; i++) letras[i] = "";
        // Tamaño de letra del sistema: se lee en sp para que crezca con la letra grande.
        texto.setTextSize(sp(14));
    }

    /**
     * Pone los siete días.
     *
     * @param valores  valor de cada día (kcal o gramos); se ignora el de los días sin datos.
     * @param hay      si el día tiene datos: sin ellos no hay barra.
     * @param colores  color de cada barra.
     * @param entreno  si se entrenó ese día (solo se pinta con {@link #conMarcaDeEntreno}); puede ser null.
     * @param letras   la inicial de cada día.
     * @param objetivo objetivo del día en la misma unidad que los valores; 0 si no hay.
     * @param rotulo   rótulo de la raya de objetivo («136 g»); null si no lleva.
     */
    public void poner(@NonNull float[] valores, @NonNull boolean[] hay, @NonNull @ColorInt int[] colores,
                      @Nullable boolean[] entreno, @NonNull String[] letras, float objetivo, @Nullable String rotulo) {
        float max = objetivo;
        for (int i = 0; i < DIAS; i++) {
            this.valores[i] = valores[i];
            this.hay[i] = hay[i];
            this.colores[i] = colores[i];
            this.entreno[i] = entreno != null && entreno[i];
            this.letras[i] = letras[i];
            if (hay[i]) max = Math.max(max, valores[i]);
        }
        this.objetivo = objetivo;
        this.rotuloObjetivo = rotulo;
        // La escala deja un poco de aire encima del objetivo y de la barra más alta.
        this.tope = Math.max(1f, max * 1.1f);
        invalidate();
    }

    /** Alto de la zona de barras en dp y radios de las esquinas (arriba y abajo) en dp. */
    public void formato(float altoBarrasDp, float radioArribaDp, float radioAbajoDp, boolean conMarcaDeEntreno) {
        this.alturaBarras = dp(altoBarrasDp);
        this.radioArriba = dp(radioArribaDp);
        this.radioAbajo = dp(radioAbajoDp);
        this.conMarcaEntreno = conMarcaDeEntreno;
        requestLayout();
    }

    /** Las barras crecen desde abajo, una tras otra; con las animaciones quitadas, ya hechas. */
    public void animarEntrada(long retraso) {
        if (animador != null) animador.cancel();
        if (Movimiento.quieto(getContext())) {
            progreso = 1f;
            invalidate();
            return;
        }
        progreso = 0f;
        long total = Movimiento.BARRA + (DIAS - 1) * Movimiento.DIA_ESCALON;
        animador = ValueAnimator.ofFloat(0f, 1f);
        animador.setStartDelay(retraso);
        animador.setDuration(total);
        animador.setInterpolator(Movimiento.LINEAL);
        animador.addUpdateListener(a -> {
            progreso = (float) a.getAnimatedValue();
            invalidate();
        });
        animador.start();
    }

    // El progreso de la barra i: arranca con su escalón y dura BARRA, con la curva estándar.
    private float progresoDe(int i) {
        float total = Movimiento.BARRA + (DIAS - 1) * Movimiento.DIA_ESCALON;
        float t = (progreso * total - i * Movimiento.DIA_ESCALON) / Movimiento.BARRA;
        t = Math.max(0f, Math.min(1f, t));
        return Movimiento.ESTANDAR.getInterpolation(t);
    }

    @Override
    protected void onMeasure(int anchoSpec, int altoSpec) {
        float letra = texto.getFontMetrics().descent - texto.getFontMetrics().ascent;
        float abajo = dp(8) + letra + (conMarcaEntreno ? dp(7) : 0);
        float arriba = dp(4);
        int alto = (int) Math.ceil(arriba + alturaBarras + abajo);
        setMeasuredDimension(resolveSize(getSuggestedMinimumWidth(), anchoSpec), resolveSize(alto, altoSpec));
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        float ancho = getWidth();
        float hueco = dp(10);
        float anchoBarra = (ancho - hueco * (DIAS - 1)) / DIAS;
        float arriba = dp(4);
        float base = arriba + alturaBarras;

        if (objetivo > 0) {
            float y = base - alturaBarras * (objetivo / tope);
            canvas.drawLine(0, y, ancho, y, raya);
            if (rotuloObjetivo != null) {
                Paint p = new Paint(texto);
                p.setTextAlign(Paint.Align.RIGHT);
                p.setTextSize(sp(12));
                canvas.drawText(rotuloObjetivo, ancho, y - dp(8), p);
            }
        }

        float letra = -texto.getFontMetrics().ascent;
        for (int i = 0; i < DIAS; i++) {
            float x = i * (anchoBarra + hueco);
            if (hay[i]) {
                float alto = Math.max(dp(2), alturaBarras * (valores[i] / tope)) * progresoDe(i);
                rect.set(x, base - alto, x + anchoBarra, base);
                barra.setColor(colores[i]);
                camino.reset();
                float[] radios = {radioArriba, radioArriba, radioArriba, radioArriba,
                        radioAbajo, radioAbajo, radioAbajo, radioAbajo};
                camino.addRoundRect(rect, radios, Path.Direction.CW);
                canvas.drawPath(camino, barra);
            }
            canvas.drawText(letras[i], x + anchoBarra / 2, base + dp(8) + letra, texto);
            if (conMarcaEntreno && entreno[i]) {
                float yMarca = base + dp(8) + letra + dp(6);
                rect.set(x + anchoBarra / 2 - dp(8), yMarca, x + anchoBarra / 2 + dp(8), yMarca + dp(3));
                canvas.drawRoundRect(rect, dp(2), dp(2), marca);
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (animador != null) animador.cancel();
        super.onDetachedFromWindow();
    }

    private float dp(float v) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }

    private float sp(float v) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v, getResources().getDisplayMetrics());
    }

    @ColorInt
    private int tema(int atributo) {
        TypedValue tv = new TypedValue();
        getContext().getTheme().resolveAttribute(atributo, tv, true);
        return tv.data;
    }
}
