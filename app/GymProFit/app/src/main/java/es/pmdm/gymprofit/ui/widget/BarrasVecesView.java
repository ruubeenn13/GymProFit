package es.pmdm.gymprofit.ui.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.color.MaterialColors;

import java.util.ArrayList;
import java.util.List;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.model.sesion.VezRutina;
import es.pmdm.gymprofit.utils.BarrasVeces;
import es.pmdm.gymprofit.utils.Movimiento;

// ============================================================
// BarrasVecesView — las barras de «tus últimas veces» con una rutina (GP-232).
//
// Una barra por sesión de esa rutina, hasta 6, de la más antigua a esta, que va a la derecha.
// Empiezan en 0 (BarrasVeces): la gráfica no exagera. La de esta sesión va en el naranja de la
// marca; la que batió un récord, en dorado; las demás, en el color de borde (3:1 contra la
// tarjeta). Las esquinas, como las de BarrasKilosView: 8 dp arriba y 4 abajo, sin pasar de media
// barra. Las medidas las da el layout (barrasAncho, barrasHueco, barrasAlto): 10, 6 y 36 en
// tarjeta, 7, 4 y 28 en el carrusel de Inicio y 5, 3 y 24 en las filas.
//
// Es decorativa para TalkBack (DEC-019): lo que dice va en la frase de la tarjeta.
//
// Al entrar, una sola vez y si no están quitadas las animaciones: las barras crecen desde abajo
// una tras otra (Movimiento.BARRA, con el escalón de CASCADA_ESCALON) y la dorada brilla una vez
// al acabar de crecer (Movimiento.BRILLO). Quien la usa decide si toca animar (entrar), para que
// no se repita al desplazar.
// ============================================================
public class BarrasVecesView extends View {

    private final Paint pincel = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path trazo = new Path();
    private final RectF rect = new RectF();

    private final float ancho;
    private final float hueco;
    private final float alto;
    private final float minimo;
    private final float radioArriba;
    private final float radioAbajo;
    private final int colorEsta;
    private final int colorOro;
    private final int colorResto;

    private float[] alturas = new float[0];
    private BarrasVeces.Color[] colores = new BarrasVeces.Color[0];
    // De 0 a 1, lo crecida que está cada barra; y lo que brilla la dorada, de 0 a 1.
    private float[] crecida = new float[0];
    private float brillo;
    private final List<ValueAnimator> animadores = new ArrayList<>();

    public BarrasVecesView(@NonNull Context context) { this(context, null); }

    public BarrasVecesView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.BarrasVecesView);
        ancho = a.getDimension(R.styleable.BarrasVecesView_barrasAncho,
                getResources().getDimension(R.dimen.barras_tarjeta_ancho));
        hueco = a.getDimension(R.styleable.BarrasVecesView_barrasHueco,
                getResources().getDimension(R.dimen.barras_tarjeta_hueco));
        alto = a.getDimension(R.styleable.BarrasVecesView_barrasAlto,
                getResources().getDimension(R.dimen.barras_tarjeta_alto));
        a.recycle();
        minimo = getResources().getDimension(R.dimen.barras_minimo);
        radioArriba = Math.min(getResources().getDimension(R.dimen.espacio_8), ancho / 2f);
        radioAbajo = Math.min(getResources().getDimension(R.dimen.espacio_4), ancho / 2f);
        colorEsta = MaterialColors.getColor(this, androidx.appcompat.R.attr.colorPrimary);
        colorOro = ContextCompat.getColor(context, R.color.gp_gold);
        colorResto = ContextCompat.getColor(context, R.color.gp_stroke);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    /**
     * Fija las veces y repinta, ya en su estado final.
     *
     * @param veces de la más antigua a esta; se quedan las 6 últimas.
     */
    public void setVeces(@NonNull List<VezRutina> veces) {
        parar();
        List<VezRutina> v = BarrasVeces.recortar(veces);
        alturas = BarrasVeces.alturas(v);
        colores = BarrasVeces.colores(v);
        crecida = new float[v.size()];
        java.util.Arrays.fill(crecida, 1f);
        brillo = 0f;
        setVisibility(v.isEmpty() ? GONE : VISIBLE);
        requestLayout();
        invalidate();
    }

    /**
     * Las barras crecen desde abajo una tras otra y la dorada brilla una vez. Con las animaciones
     * quitadas no hace nada: ya están en su estado final.
     */
    public void entrar() {
        if (alturas.length == 0 || Movimiento.quieto(getContext())) return;
        parar();
        java.util.Arrays.fill(crecida, 0f);
        invalidate();
        int ultimaDorada = -1;
        for (int i = 0; i < colores.length; i++) if (colores[i] == BarrasVeces.Color.ORO) ultimaDorada = i;
        long finCrecer = 0;
        for (int i = 0; i < alturas.length; i++) {
            final int n = i;
            ValueAnimator crece = ValueAnimator.ofFloat(0f, 1f);
            crece.setDuration(Movimiento.BARRA);
            crece.setStartDelay(i * Movimiento.CASCADA_ESCALON);
            crece.setInterpolator(Movimiento.ESTANDAR);
            crece.addUpdateListener(an -> {
                crecida[n] = (float) an.getAnimatedValue();
                invalidate();
            });
            animadores.add(crece);
            crece.start();
            finCrecer = Math.max(finCrecer, i * Movimiento.CASCADA_ESCALON + Movimiento.BARRA);
        }
        if (ultimaDorada >= 0) {
            ValueAnimator brilla = ValueAnimator.ofFloat(0f, 1f);
            brilla.setDuration(Movimiento.BRILLO);
            brilla.setStartDelay(finCrecer);
            brilla.setInterpolator(Movimiento.LINEAL);
            brilla.addUpdateListener(an -> {
                float t = (float) an.getAnimatedValue();
                // Sube hasta el 40 % del tiempo y vuelve, como el lienzo.
                brillo = t < 0.4f ? t / 0.4f : (1f - t) / 0.6f;
                invalidate();
            });
            animadores.add(brilla);
            brilla.start();
        }
    }

    // Lo que se estaba moviendo acaba ya, en su estado final.
    private void parar() {
        for (ValueAnimator a : animadores) a.cancel();
        animadores.clear();
        java.util.Arrays.fill(crecida, 1f);
        brillo = 0f;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        parar();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int n = alturas.length;
        int w = n == 0 ? 0 : Math.round(n * ancho + (n - 1) * hueco) + getPaddingLeft() + getPaddingRight();
        int h = Math.round(alto) + getPaddingTop() + getPaddingBottom();
        setMeasuredDimension(resolveSize(w, widthMeasureSpec), resolveSize(h, heightMeasureSpec));
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        int n = alturas.length;
        if (n == 0) return;
        float base = getHeight() - getPaddingBottom();
        // Pegadas a la derecha: la de esta sesión siempre en el mismo sitio.
        float x = getWidth() - getPaddingRight() - (n * ancho + (n - 1) * hueco);
        float[] radios = {radioArriba, radioArriba, radioArriba, radioArriba, radioAbajo, radioAbajo, radioAbajo, radioAbajo};
        for (int i = 0; i < n; i++) {
            float h = Math.max(alturas[i] * alto, minimo) * crecida[i];
            if (h > 0.5f) {
                pincel.setColor(color(colores[i]));
                rect.set(x, base - h, x + ancho, base);
                trazo.reset();
                trazo.addRoundRect(rect, radios, Path.Direction.CW);
                canvas.drawPath(trazo, pincel);
            }
            x += ancho + hueco;
        }
    }

    private int color(BarrasVeces.Color c) {
        switch (c) {
            case ORO: return brillo > 0 ? masLuz(colorOro, brillo) : colorOro;
            case ESTA: return colorEsta;
            default: return colorResto;
        }
    }

    // El brillo del lienzo: el color multiplicado hasta por 1,45, sin pasar del blanco.
    private static int masLuz(int color, float k) {
        float f = 1f + 0.45f * k;
        return Color.argb(Color.alpha(color), Math.min(255, Math.round(Color.red(color) * f)),
                Math.min(255, Math.round(Color.green(color) * f)), Math.min(255, Math.round(Color.blue(color) * f)));
    }
}
