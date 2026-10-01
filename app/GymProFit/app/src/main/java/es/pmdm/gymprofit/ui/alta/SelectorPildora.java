package es.pmdm.gymprofit.ui.alta;

import android.animation.AnimatorInflater;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.utils.Movimiento;

// ============================================================
// SelectorPildora — una fila de opciones con una píldora naranja que viaja a la elegida
// (GP-103, «Días y tiempo»; GP-104, momento 6).
//
// Opciones de contorno, todas del mismo ancho y 8 dp de hueco; la elegida pierde el
// borde y su texto pasa a oscuro sobre la píldora, que llega con rebote en 320 ms. Sin
// elegir nada, la píldora no se ve.
//
// Cada opción es una vista de texto, no un dibujo: TalkBack la lee como un botón de
// radio, con su nombre completo («3 días») y si está marcada (DEC-019). A letra grande
// las opciones crecen en alto y la píldora con ellas.
// ============================================================
public class SelectorPildora extends FrameLayout {

    /** Se avisa al elegir una opción con el dedo. */
    public interface AlElegir { void elegida(int indice); }

    private final View pildora;
    private final LinearLayout fila;
    private TextView[] opciones = new TextView[0];
    private int elegida = -1;
    private boolean condensada;
    @Nullable private AlElegir alElegir;

    public SelectorPildora(@NonNull Context context) {
        this(context, null);
    }

    public SelectorPildora(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        GradientDrawable fondo = new GradientDrawable();
        fondo.setColor(color(androidx.appcompat.R.attr.colorPrimary));
        pildora = new View(context);
        pildora.setBackground(fondo);
        pildora.setVisibility(INVISIBLE);
        pildora.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        addView(pildora, new LayoutParams(0, 0));

        fila = new LinearLayout(context);
        fila.setOrientation(LinearLayout.HORIZONTAL);
        fila.setBaselineAligned(false);
        addView(fila, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        fila.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> colocarPildora(false));
    }

    /**
     * Las opciones.
     *
     * @param textos      lo que se ve («2», «45 min»).
     * @param nombres     lo que lee TalkBack («2 días»).
     * @param altoDp      alto mínimo de cada opción.
     * @param condensada  cifra grande en Barlow Condensed (días) o texto normal (minutos).
     */
    public void setOpciones(@NonNull String[] textos, @NonNull String[] nombres, int altoDp, boolean condensada) {
        this.condensada = condensada;
        fila.removeAllViews();
        opciones = new TextView[textos.length];
        float d = getResources().getDisplayMetrics().density;
        for (int i = 0; i < textos.length; i++) {
            TextView o = new TextView(getContext());
            o.setText(textos[i]);
            o.setContentDescription(nombres[i]);
            o.setGravity(Gravity.CENTER);
            o.setMinHeight(Math.round(altoDp * d));
            o.setPadding(Math.round(4 * d), Math.round(6 * d), Math.round(4 * d), Math.round(6 * d));
            o.setClickable(true);
            o.setFocusable(true);
            o.setStateListAnimator(AnimatorInflater.loadStateListAnimator(getContext(), R.animator.toque));
            if (condensada) {
                o.setTypeface(ResourcesCompat.getFont(getContext(), R.font.barlow_condensed), Typeface.BOLD);
                o.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
            } else {
                // A letra grande, «75+ min» se partía por la mitad: una línea, y el texto
                // encoge hasta 13 sp, el mínimo de la app (GP-089), antes de partirse.
                o.setMaxLines(1);
                androidx.core.widget.TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(o,
                        13, Math.max(13, Math.round(getResources().getDimension(R.dimen.text_body)
                                / getResources().getDisplayMetrics().scaledDensity)), 1, TypedValue.COMPLEX_UNIT_SP);
            }
            GradientDrawable borde = new GradientDrawable();
            borde.setCornerRadius(altoDp * d / 2f);
            o.setBackground(borde);
            final int indice = i;
            o.setOnClickListener(v -> {
                if (indice == elegida) return;
                Movimiento.vibrar(v, Movimiento.Vibracion.LIGERA);
                setElegida(indice, true);
                if (alElegir != null) alElegir.elegida(indice);
            });
            ViewCompat.setAccessibilityDelegate(o, new AccessibilityDelegateCompat() {
                @Override
                public void onInitializeAccessibilityNodeInfo(@NonNull View host,
                                                              @NonNull AccessibilityNodeInfoCompat info) {
                    super.onInitializeAccessibilityNodeInfo(host, info);
                    info.setClassName(RadioButton.class.getName());
                    info.setCheckable(true);
                    info.setChecked(indice == elegida);
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
            if (i > 0) lp.setMarginStart(Math.round(8 * d));
            fila.addView(o, lp);
            opciones[i] = o;
        }
        pintar(false);
    }

    public void setAlElegir(@Nullable AlElegir alElegir) {
        this.alElegir = alElegir;
    }

    /** La elegida, o -1. */
    public int getElegida() {
        return elegida;
    }

    /**
     * Elige una opción.
     *
     * @param animar con la píldora viajando (al tocar) o en su sitio (al restaurar).
     */
    public void setElegida(int indice, boolean animar) {
        boolean primera = elegida < 0;
        elegida = indice;
        pintar(animar);
        // La primera vez no hay de dónde viajar: aparece en su sitio.
        colocarPildora(animar && !primera);
        for (TextView o : opciones) o.sendAccessibilityEvent(android.view.accessibility.AccessibilityEvent.TYPE_VIEW_SELECTED);
    }

    private void colocarPildora(boolean animar) {
        if (elegida < 0 || elegida >= opciones.length) {
            pildora.setVisibility(INVISIBLE);
            return;
        }
        TextView o = opciones[elegida];
        if (o.getWidth() == 0) return;
        LayoutParams lp = (LayoutParams) pildora.getLayoutParams();
        if (lp.width != o.getWidth() || lp.height != o.getHeight()) {
            lp.width = o.getWidth();
            lp.height = o.getHeight();
            ((GradientDrawable) pildora.getBackground()).setCornerRadius(Math.min(o.getWidth(), o.getHeight()) / 2f);
            pildora.setLayoutParams(lp);
        }
        float x = getLayoutDirection() == LAYOUT_DIRECTION_RTL
                ? -(fila.getWidth() - o.getRight()) : o.getLeft();
        pildora.setVisibility(VISIBLE);
        if (animar) {
            pildora.animate().translationX(x).setDuration(Movimiento.PILDORA)
                    .setInterpolator(Movimiento.REBOTE).start();
        } else {
            pildora.animate().cancel();
            pildora.setTranslationX(x);
        }
    }

    // Borde y texto de cada opción: con contorno sin elegir, sin borde y oscuro elegida.
    private void pintar(boolean animar) {
        float d = getResources().getDisplayMetrics().density;
        int contorno = color(com.google.android.material.R.attr.colorOutline);
        int texto = color(com.google.android.material.R.attr.colorOnSurface);
        int textoElegido = color(com.google.android.material.R.attr.colorOnPrimary);
        ArgbEvaluator mezcla = new ArgbEvaluator();
        for (int i = 0; i < opciones.length; i++) {
            TextView o = opciones[i];
            boolean si = i == elegida;
            GradientDrawable borde = (GradientDrawable) o.getBackground();
            int bordeFinal = si ? (contorno & 0x00FFFFFF) : contorno;
            int textoFinal = si ? textoElegido : texto;
            if (!condensada) o.setTypeface(o.getTypeface(), si ? Typeface.BOLD : Typeface.NORMAL);
            if (!animar) {
                borde.setStroke(Math.round(d), bordeFinal);
                o.setTextColor(textoFinal);
                continue;
            }
            int bordeIni = si ? contorno : (contorno & 0x00FFFFFF);
            int textoIni = o.getCurrentTextColor();
            ValueAnimator a = ValueAnimator.ofFloat(0f, 1f);
            a.setDuration(200);
            a.addUpdateListener(an -> {
                float t = (float) an.getAnimatedValue();
                borde.setStroke(Math.round(d), (int) mezcla.evaluate(t, bordeIni, bordeFinal));
                o.setTextColor((int) mezcla.evaluate(t, textoIni, textoFinal));
            });
            a.start();
        }
    }

    private int color(@AttrRes int attr) {
        TypedValue tv = new TypedValue();
        getContext().getTheme().resolveAttribute(attr, tv, true);
        return tv.data;
    }
}
