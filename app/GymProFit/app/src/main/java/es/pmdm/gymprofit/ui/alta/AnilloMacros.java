package es.pmdm.gymprofit.ui.alta;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.utils.Movimiento;

// ============================================================
// AnilloMacros — el anillo de «Tu plan»: las calorías del día repartidas por macros
// (GP-103, tablero 8 del lienzo).
//
// Un aro de 12 dp sobre su pista, con tres arcos —proteína, carbohidratos y grasa— del
// tamaño de las calorías que aporta cada uno (4, 4 y 9 kcal por gramo), en sus colores
// de siempre. Momento 7 de DEC-039: los arcos se dibujan uno tras otro (320, 480 y 320
// ms con la curva estándar) mientras las cifras cuentan, y al final sale un halo.
//
// Es dibujo: lo que dice lo dice la vista que lo contiene, con «2397 kilocalorías al
// día», así que este no es importante para TalkBack (DEC-019).
// ============================================================
public class AnilloMacros extends View {

    private final Paint pista = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint[] arcos = new Paint[3];
    private final Paint halo = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF caja = new RectF();

    // Fracción de la vuelta de cada macro y cuánto de ella está dibujado (0..1).
    private final float[] partes = new float[3];
    private final float[] dibujado = new float[3];
    private float alfaHalo;
    private float escalaHalo = 1f;

    public AnilloMacros(@NonNull Context context) {
        this(context, null);
    }

    public AnilloMacros(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        float grosor = 12 * getResources().getDisplayMetrics().density;
        TypedValue tv = new TypedValue();
        context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorSurfaceVariant, tv, true);
        pista.setStyle(Paint.Style.STROKE);
        pista.setStrokeWidth(grosor);
        pista.setColor(tv.data);
        int[] colores = {R.color.gp_macro_proteinas, R.color.gp_macro_carbos, R.color.gp_macro_grasas};
        for (int i = 0; i < 3; i++) {
            arcos[i] = new Paint(Paint.ANTI_ALIAS_FLAG);
            arcos[i].setStyle(Paint.Style.STROKE);
            arcos[i].setStrokeWidth(grosor);
            arcos[i].setColor(ContextCompat.getColor(context, colores[i]));
        }
    }

    /**
     * Reparte el anillo y lo dibuja.
     *
     * @param proteinas     gramos de proteína.
     * @param carbohidratos gramos de carbohidratos.
     * @param grasas        gramos de grasa.
     * @param animar        con el momento 7, o dibujado del todo.
     */
    public void mostrar(int proteinas, int carbohidratos, int grasas, boolean animar) {
        float p = proteinas * 4f, c = carbohidratos * 4f, g = grasas * 9f;
        float total = p + c + g;
        if (total <= 0) total = 1;
        partes[0] = p / total;
        partes[1] = c / total;
        partes[2] = g / total;
        if (!animar || Movimiento.quieto(getContext())) {
            dibujado[0] = dibujado[1] = dibujado[2] = 1f;
            invalidate();
            return;
        }
        long[] retraso = {200, 520, 1000};
        long[] duracion = {320, 480, 320};
        for (int i = 0; i < 3; i++) {
            final int k = i;
            dibujado[k] = 0f;
            ValueAnimator a = ValueAnimator.ofFloat(0f, 1f);
            a.setStartDelay(retraso[k]);
            a.setDuration(duracion[k]);
            a.setInterpolator(Movimiento.ESTANDAR);
            a.addUpdateListener(an -> {
                dibujado[k] = (float) an.getAnimatedValue();
                invalidate();
            });
            a.start();
        }
        ValueAnimator h = ValueAnimator.ofFloat(0f, 1f);
        h.setStartDelay(1250);
        h.setDuration(Movimiento.HALO);
        h.addUpdateListener(an -> {
            float t = (float) an.getAnimatedValue();
            escalaHalo = 0.8f + 0.5f * t;
            alfaHalo = t < 0.3f ? t / 0.3f : (1f - t) / 0.7f;
            invalidate();
        });
        h.start();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        float d = getResources().getDisplayMetrics().density;
        float cx = getWidth() / 2f, cy = getHeight() / 2f;
        float radio = Math.min(getWidth(), getHeight()) / 2f - 10 * d;

        if (alfaHalo > 0f) {
            float r = (radio + 22 * d) * escalaHalo;
            int naranja = ContextCompat.getColor(getContext(), R.color.gp_primary);
            halo.setShader(new RadialGradient(cx, cy, r,
                    new int[]{(naranja & 0x00FFFFFF) | 0x52000000, naranja & 0x00FFFFFF},
                    new float[]{0f, 0.7f}, Shader.TileMode.CLAMP));
            halo.setAlpha(Math.round(255 * alfaHalo));
            canvas.drawCircle(cx, cy, r, halo);
        }

        caja.set(cx - radio, cy - radio, cx + radio, cy + radio);
        canvas.drawCircle(cx, cy, radio, pista);
        // Empieza arriba, en sentido horario, con medio grado de hueco entre macros.
        float inicio = -90f;
        float hueco = 0.6f;
        for (int i = 0; i < 3; i++) {
            float barrido = 360f * partes[i];
            float visible = Math.max(0f, (barrido - hueco) * dibujado[i]);
            if (visible > 0f) canvas.drawArc(caja, inicio, visible, false, arcos[i]);
            inicio += barrido;
        }
    }
}
