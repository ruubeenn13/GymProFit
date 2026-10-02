package es.pmdm.gymprofit.ui.nutricion;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.utils.Movimiento;

// ============================================================
// AnilloComida — el anillo del resumen de una comida (decisión 16, tablero 2, lote 1.6.2)
//
// Reparte las kcal de la comida entre proteína, carbohidratos y grasa con un hueco de
// 3 dp entre tramos, sobre su pista, en un aro de 12 dp. Al entrar se dibuja una vez,
// tramo a tramo (500 ms estándar, a los 150, 250 y 350 ms, como el lienzo); al quitar o
// deshacer, cada tramo pasa a su tamaño nuevo con BARRA (momento 20). Con «Quitar
// animaciones», en su sitio.
// Es dibujo: lo que dice lo dice la vista que lo contiene (DEC-019).
// ============================================================
public class AnilloComida extends View {

    private static final long[] RETRASO_ENTRADA = {150, 250, 350};

    private final Paint pista = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint[] arcos = new Paint[3];
    private final RectF caja = new RectF();
    private final float grosor;
    private final float hueco;

    // Fracción de la vuelta de cada macro, y cuánto está dibujado de la entrada (0..1).
    private final float[] partes = new float[3];
    private final float[] dibujado = {1f, 1f, 1f};
    @Nullable private ValueAnimator cambio;

    public AnilloComida(@NonNull Context context) {
        this(context, null);
    }

    public AnilloComida(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        float d = getResources().getDisplayMetrics().density;
        grosor = 12 * d;
        hueco = 3 * d;
        pista.setStyle(Paint.Style.STROKE);
        pista.setStrokeWidth(grosor);
        pista.setColor(ContextCompat.getColor(context, R.color.gp_meter_track));
        int[] colores = {R.color.gp_macro_proteinas, R.color.gp_macro_carbos, R.color.gp_macro_grasas};
        for (int i = 0; i < 3; i++) {
            arcos[i] = new Paint(Paint.ANTI_ALIAS_FLAG);
            arcos[i].setStyle(Paint.Style.STROKE);
            arcos[i].setStrokeWidth(grosor);
            arcos[i].setColor(ContextCompat.getColor(context, colores[i]));
        }
    }

    /**
     * Pone el reparto.
     *
     * @param reparto  {proteína, carbohidratos, grasa} en % de las kcal, o null (sin macros:
     *                 solo la pista).
     * @param entrada  true la primera vez: se dibuja tramo a tramo. false: cada tramo pasa
     *                 de su tamaño de ahora al nuevo.
     */
    public void mostrar(@Nullable int[] reparto, boolean entrada) {
        float[] nuevas = new float[3];
        if (reparto != null) for (int i = 0; i < 3; i++) nuevas[i] = reparto[i] / 100f;
        if (cambio != null) cambio.cancel();
        boolean quieto = Movimiento.quieto(getContext());
        if (entrada) {
            System.arraycopy(nuevas, 0, partes, 0, 3);
            for (int i = 0; i < 3; i++) {
                if (quieto) {
                    dibujado[i] = 1f;
                    continue;
                }
                final int k = i;
                dibujado[k] = 0f;
                ValueAnimator a = ValueAnimator.ofFloat(0f, 1f);
                a.setStartDelay(RETRASO_ENTRADA[k]);
                a.setDuration(Movimiento.BARRA);
                a.setInterpolator(Movimiento.ESTANDAR);
                a.addUpdateListener(an -> {
                    dibujado[k] = (float) an.getAnimatedValue();
                    invalidate();
                });
                a.start();
            }
            invalidate();
            return;
        }
        float[] desde = partes.clone();
        if (quieto) {
            System.arraycopy(nuevas, 0, partes, 0, 3);
            invalidate();
            return;
        }
        cambio = ValueAnimator.ofFloat(0f, 1f);
        cambio.setDuration(Movimiento.BARRA);
        cambio.setInterpolator(Movimiento.ESTANDAR);
        cambio.addUpdateListener(an -> {
            float t = (float) an.getAnimatedValue();
            for (int i = 0; i < 3; i++) partes[i] = desde[i] + (nuevas[i] - desde[i]) * t;
            invalidate();
        });
        cambio.start();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        float cx = getWidth() / 2f, cy = getHeight() / 2f;
        float radio = Math.min(getWidth(), getHeight()) / 2f - grosor / 2f - getPaddingStart();
        caja.set(cx - radio, cy - radio, cx + radio, cy + radio);
        canvas.drawCircle(cx, cy, radio, pista);
        // El hueco entre tramos, en grados de esta circunferencia.
        float huecoGrados = (float) (hueco * 360 / (2 * Math.PI * radio));
        int conTramo = 0;
        for (float p : partes) if (p > 0f) conTramo++;
        float util = 360f - (conTramo > 1 ? huecoGrados * conTramo : 0f);
        float inicio = -90f;
        for (int i = 0; i < 3; i++) {
            if (partes[i] <= 0f) continue;
            float barrido = util * partes[i];
            float visible = barrido * dibujado[i];
            if (visible > 0f) canvas.drawArc(caja, inicio, visible, false, arcos[i]);
            inicio += barrido + (conTramo > 1 ? huecoGrados : 0f);
        }
    }
}
