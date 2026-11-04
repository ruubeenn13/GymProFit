package es.pmdm.gymprofit.arranque;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

import es.pmdm.gymprofit.utils.Movimiento;

// ============================================================
// VistaG — la G de la pantalla de carga del arranque, con su brillo (GP-223).
//
// Es un ImageView con g_arranque, el último fotograma de la G que entra en el splash del
// sistema: debajo del splash, en el mismo sitio y al mismo tamaño. Lo único que dibuja a
// mano es el brillo: una franja de luz blanca al 40 %, recortada por la G (SRC_ATOP sobre
// una capa con la G), que la cruza en G_BRILLO con la curva estándar y vuelve a empezar
// cada G_BRILLO_CADA.
//
// Accesibilidad (DEC-019): es una imagen con nombre, «Cargando GymProFit», que la pantalla
// de carga enfoca al aparecer. El brillo no se anuncia: es decoración. Con «Quitar
// animaciones» no se enciende (lo decide PlanArranque) y la G se queda quieta.
// ============================================================
public class VistaG extends AppCompatImageView {

    /** Ancho de la franja respecto a la vista, e inclinación: lo que avanza abajo frente a arriba. */
    private static final float ANCHO = 0.22f;
    private static final float INCLINACION = 0.35f;

    private final Paint pintura = new Paint(Paint.ANTI_ALIAS_FLAG);
    /** La franja: blanca al 40 % en el centro (gp_arranque_brillo), transparente a los lados. */
    private final int luz;
    @Nullable private ValueAnimator brillo;
    /** Dónde va la franja, de 0 (fuera por la izquierda) a 1 (fuera por la derecha); -1 si no hay. */
    private float posicion = -1f;

    public VistaG(@NonNull Context context) { this(context, null); }

    public VistaG(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        luz = androidx.core.content.ContextCompat.getColor(context, es.pmdm.gymprofit.R.color.gp_arranque_brillo);
        pintura.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
    }

    /** Empieza el brillo en bucle. Si ya está, no hace nada. */
    public void empezarBrillo() {
        if (brillo != null) return;
        // Un solo animador lineal de un ciclo entero: el cruce ocupa los primeros G_BRILLO
        // con la estándar y el resto del ciclo la franja está fuera.
        brillo = ValueAnimator.ofFloat(0f, 1f);
        brillo.setDuration(Movimiento.G_BRILLO_CADA);
        brillo.setInterpolator(Movimiento.LINEAL);
        brillo.setRepeatCount(ValueAnimator.INFINITE);
        brillo.addUpdateListener(a -> {
            float t = a.getAnimatedFraction() * Movimiento.G_BRILLO_CADA;
            posicion = t < Movimiento.G_BRILLO
                    ? Movimiento.ESTANDAR.getInterpolation(t / Movimiento.G_BRILLO) : -1f;
            invalidate();
        });
        brillo.start();
    }

    /** Para el brillo y deja la G limpia. */
    public void pararBrillo() {
        if (brillo != null) brillo.cancel();
        brillo = null;
        posicion = -1f;
        invalidate();
    }

    @Override
    protected void onDetachedFromWindow() {
        pararBrillo();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        if (posicion < 0f) {
            super.onDraw(canvas);
            return;
        }
        int w = getWidth(), h = getHeight();
        int capa = canvas.saveLayer(0, 0, w, h, null);
        super.onDraw(canvas);
        // La franja entra por la izquierda y sale por la derecha: su centro va de −ancho a w + ancho.
        float ancho = w * ANCHO;
        float centro = -ancho - h * INCLINACION + posicion * (w + 2 * ancho + h * INCLINACION);
        pintura.setShader(new LinearGradient(centro - ancho, 0, centro + ancho, h * INCLINACION,
                new int[]{Color.TRANSPARENT, luz, Color.TRANSPARENT}, null, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w, h, pintura);
        canvas.restoreToCount(capa);
    }
}
