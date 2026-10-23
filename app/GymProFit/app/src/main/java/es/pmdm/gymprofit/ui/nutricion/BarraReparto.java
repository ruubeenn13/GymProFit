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
// BarraReparto — el reparto de macros de una comida en una barra (GP-216, tablero 28)
//
// Tres tramos seguidos, proteína, carbohidratos y grasa, con un hueco de 2 dp entre ellos
// y las esquinas redondeadas, sobre una pista. Sustituye al anillo de la 1.6.2: la tarjeta
// protagonista lleva la cifra grande y esta barra debajo. Al entrar se dibuja de izquierda
// a derecha (BARRA); al quitar o deshacer, cada tramo pasa a su tamaño nuevo. Con «Quitar
// animaciones», en su sitio.
// Es dibujo: lo que dice lo dice la vista que lo contiene (DEC-019).
// ============================================================
public class BarraReparto extends View {

    private final Paint pista = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint[] tramos = new Paint[3];
    private final RectF caja = new RectF();
    private final float hueco;
    private final float[] partes = new float[3];
    // Cuánto de la barra está dibujado de la entrada (0..1).
    private float revelado = 1f;
    @Nullable private ValueAnimator animador;

    public BarraReparto(@NonNull Context context) {
        this(context, null);
    }

    public BarraReparto(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        hueco = 2 * getResources().getDisplayMetrics().density;
        pista.setColor(0x33FFFFFF);
        int[] colores = {R.color.gp_macro_proteinas, R.color.gp_macro_carbos, R.color.gp_macro_grasas};
        for (int i = 0; i < 3; i++) {
            tramos[i] = new Paint(Paint.ANTI_ALIAS_FLAG);
            tramos[i].setColor(ContextCompat.getColor(context, colores[i]));
        }
    }

    /**
     * Pone el reparto.
     *
     * @param reparto {proteína, carbohidratos, grasa} en % de las kcal, o null (solo la pista).
     * @param entrada true la primera vez: se dibuja de izquierda a derecha. false: pasa de lo
     *                que había a lo nuevo.
     */
    public void mostrar(@Nullable int[] reparto, boolean entrada) {
        float[] nuevas = new float[3];
        if (reparto != null) for (int i = 0; i < 3; i++) nuevas[i] = reparto[i] / 100f;
        if (animador != null) animador.cancel();
        boolean quieto = Movimiento.quieto(getContext());
        if (entrada) {
            System.arraycopy(nuevas, 0, partes, 0, 3);
            if (quieto) {
                revelado = 1f;
                invalidate();
                return;
            }
            revelado = 0f;
            animador = ValueAnimator.ofFloat(0f, 1f);
            animador.setStartDelay(150);
            animador.setDuration(Movimiento.BARRA);
            animador.setInterpolator(Movimiento.ESTANDAR);
            animador.addUpdateListener(a -> {
                revelado = (float) a.getAnimatedValue();
                invalidate();
            });
            animador.start();
            return;
        }
        float[] desde = partes.clone();
        if (quieto) {
            System.arraycopy(nuevas, 0, partes, 0, 3);
            invalidate();
            return;
        }
        animador = ValueAnimator.ofFloat(0f, 1f);
        animador.setDuration(Movimiento.BARRA);
        animador.setInterpolator(Movimiento.ESTANDAR);
        animador.addUpdateListener(a -> {
            float t = (float) a.getAnimatedValue();
            for (int i = 0; i < 3; i++) partes[i] = desde[i] + (nuevas[i] - desde[i]) * t;
            invalidate();
        });
        animador.start();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        float w = getWidth(), h = getHeight(), r = h / 2f;
        caja.set(0, 0, w, h);
        canvas.drawRoundRect(caja, r, r, pista);
        int conTramo = 0;
        for (float p : partes) if (p > 0f) conTramo++;
        float util = w - (conTramo > 1 ? hueco * (conTramo - 1) : 0f);
        float limite = w * revelado;
        float x = 0f;
        for (int i = 0; i < 3; i++) {
            if (partes[i] <= 0f) continue;
            float fin = x + util * partes[i];
            float dibujaFin = Math.min(fin, limite);
            if (dibujaFin > x) {
                // Extremos redondos solo en los de la barra; entre tramos, casi rectos.
                caja.set(x, 0, dibujaFin, h);
                canvas.drawRoundRect(caja, r, r, tramos[i]);
            }
            x = fin + hueco;
        }
    }
}
