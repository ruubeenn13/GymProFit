package es.pmdm.gymprofit.ui.alta;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.ProgressBar;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.utils.AltaPasos;
import es.pmdm.gymprofit.utils.Movimiento;

// ============================================================
// BarraCapitulos — la barra del cuestionario: cuatro tramos, uno por capítulo (GP-103).
//
// Tramos de 6 dp con 6 dp de hueco y radio 3. Los capítulos cerrados, llenos; el de
// ahora, con la parte que le toca a la pregunta (AltaPasos.Paso.fraccion); los que
// faltan, vacíos.
//
// Momento 5 de DEC-039: el tramo se llena en 500 ms con la curva estándar, 150 ms
// después de entrar, y si con eso se cierra el capítulo, destella 700 ms.
//
// Vista dibujada a mano, así que nace con su accesibilidad (DEC-019): TalkBack la lee
// como una barra de progreso, con su porcentaje y «Capítulo 2 de 4, sobre ti».
// ============================================================
public class BarraCapitulos extends View {

    private final Paint pista = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint lleno = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint destello = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    private int capitulo = 1;
    private float fraccion;
    private float alfaDestello;
    private int porcentaje;
    private String texto = "";

    public BarraCapitulos(@NonNull Context context) {
        this(context, null);
    }

    public BarraCapitulos(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        pista.setColor(color(com.google.android.material.R.attr.colorSurfaceVariant));
        lleno.setColor(color(androidx.appcompat.R.attr.colorPrimary));
        destello.setColor(color(androidx.appcompat.R.attr.colorPrimary));
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        setContentDescription(context.getString(R.string.alta_progreso_a11y));

        ViewCompat.setAccessibilityDelegate(this, new AccessibilityDelegateCompat() {
            @Override
            public void onInitializeAccessibilityNodeInfo(@NonNull View host,
                                                          @NonNull AccessibilityNodeInfoCompat info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setClassName(ProgressBar.class.getName());
                info.setRangeInfo(AccessibilityNodeInfoCompat.RangeInfoCompat.obtain(
                        AccessibilityNodeInfoCompat.RangeInfoCompat.RANGE_TYPE_PERCENT, 0, 100, porcentaje));
                info.setStateDescription(texto);
            }
        });
    }

    /**
     * Pone la barra en una pregunta.
     *
     * @param paso     la pregunta de ahora.
     * @param anterior la de antes, para llenar desde donde estaba; null al entrar sin animar.
     * @param nombre   el nombre del capítulo, para TalkBack («tu objetivo»).
     */
    public void mostrar(@NonNull AltaPasos.Paso paso, @Nullable AltaPasos.Paso anterior, @NonNull String nombre) {
        capitulo = paso.capitulo;
        porcentaje = paso.porcentaje();
        texto = getContext().getString(R.string.alta_capitulo_a11y, paso.capitulo, AltaPasos.CAPITULOS, nombre);
        ViewCompat.setStateDescription(this, texto);

        boolean mismoCapitulo = anterior != null && anterior.capitulo == paso.capitulo;
        float desde = anterior == null ? paso.fraccion
                : mismoCapitulo ? anterior.fraccion
                : anterior.capitulo < paso.capitulo ? 0f : 1f;
        fraccion = desde;
        alfaDestello = 0f;
        invalidate();
        if (anterior == null || desde == paso.fraccion) {
            fraccion = paso.fraccion;
            invalidate();
            return;
        }
        ValueAnimator a = ValueAnimator.ofFloat(desde, paso.fraccion);
        a.setDuration(Movimiento.BARRA);
        a.setStartDelay(150);
        a.setInterpolator(Movimiento.ESTANDAR);
        a.addUpdateListener(an -> {
            fraccion = (float) an.getAnimatedValue();
            invalidate();
        });
        a.start();
        // Se cierra el capítulo al avanzar: destella.
        if (paso.fraccion >= 1f && desde < 1f && !Movimiento.quieto(getContext())) {
            ValueAnimator d = ValueAnimator.ofFloat(0f, 1f);
            d.setDuration(Movimiento.DESTELLO);
            d.setStartDelay(650);
            d.addUpdateListener(an -> {
                float t = (float) an.getAnimatedValue();
                alfaDestello = t < 0.4f ? t / 0.4f : (1f - t) / 0.6f;
                invalidate();
            });
            d.start();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int alto = Math.round(18 * getResources().getDisplayMetrics().density);
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), resolveSize(alto, heightMeasureSpec));
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        float d = getResources().getDisplayMetrics().density;
        float hueco = 6 * d;
        float alto = 6 * d;
        float radio = 3 * d;
        float margen = 6 * d;
        float ancho = (getWidth() - 2 * margen - hueco * (AltaPasos.CAPITULOS - 1)) / AltaPasos.CAPITULOS;
        float arriba = (getHeight() - alto) / 2f;
        boolean rtl = getLayoutDirection() == LAYOUT_DIRECTION_RTL;

        for (int i = 0; i < AltaPasos.CAPITULOS; i++) {
            int indice = rtl ? AltaPasos.CAPITULOS - 1 - i : i;
            float izq = margen + indice * (ancho + hueco);
            rect.set(izq, arriba, izq + ancho, arriba + alto);
            int cap = i + 1;
            if (cap == capitulo && alfaDestello > 0f) {
                destello.setAlpha(Math.round(110 * alfaDestello));
                float r = 3 * d * alfaDestello;
                canvas.drawRoundRect(rect.left - r, rect.top - r, rect.right + r, rect.bottom + r,
                        radio + r, radio + r, destello);
            }
            canvas.drawRoundRect(rect, radio, radio, pista);
            float f = cap < capitulo ? 1f : cap == capitulo ? fraccion : 0f;
            if (f <= 0f) continue;
            if (rtl) rect.left = rect.right - ancho * f;
            else rect.right = rect.left + ancho * f;
            canvas.drawRoundRect(rect, radio, radio, lleno);
        }
    }

    private int color(@AttrRes int attr) {
        TypedValue tv = new TypedValue();
        getContext().getTheme().resolveAttribute(attr, tv, true);
        return tv.data;
    }
}
