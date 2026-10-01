package es.pmdm.gymprofit.ui.alta;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.utils.AltaPasos;
import es.pmdm.gymprofit.utils.Movimiento;

// ============================================================
// SemanaEjemplo — la semana de ejemplo de «Días y tiempo» (GP-103): siete círculos de
// 30 dp, L a D, con los días que tocarían encendidos en naranja.
//
// Momento 6 de DEC-039: al cambiar los días, cada círculo cambia de color en 220 ms y
// crece de 0,86 a 1 con rebote en 280, uno tras otro cada 35 ms.
//
// Es una imagen para TalkBack, con la frase entera («Por ejemplo, lunes, miércoles y
// viernes»): las letras sueltas no dicen nada a quien no las ve.
// ============================================================
public class SemanaEjemplo extends LinearLayout {

    private final TextView[] dias = new TextView[7];
    private boolean[] encendidos = new boolean[7];

    public SemanaEjemplo(@NonNull Context context) {
        this(context, null);
    }

    public SemanaEjemplo(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        float d = getResources().getDisplayMetrics().density;
        String[] letras = getResources().getStringArray(R.array.alta_semana_letras);
        for (int i = 0; i < 7; i++) {
            TextView t = new TextView(context);
            t.setText(letras[i]);
            t.setGravity(Gravity.CENTER);
            t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            t.setTypeface(t.getTypeface(), Typeface.BOLD);
            t.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
            GradientDrawable fondo = new GradientDrawable();
            fondo.setShape(GradientDrawable.OVAL);
            t.setBackground(fondo);
            int lado = Math.round(30 * d);
            LayoutParams lp = new LayoutParams(lado, lado);
            if (i > 0) lp.setMarginStart(Math.round(6 * d));
            addView(t, lp);
            dias[i] = t;
        }
        pintar(false);
    }

    /**
     * Enciende los días del ejemplo para tantos días por semana.
     *
     * @param animar día a día (al elegir) o de golpe (al restaurar).
     * @param frase  lo que lee TalkBack.
     */
    public void mostrar(int numDias, boolean animar, @NonNull String frase) {
        encendidos = AltaPasos.semanaDeEjemplo(numDias);
        setContentDescription(frase);
        pintar(animar);
    }

    private void pintar(boolean animar) {
        int fondoOn = color(androidx.appcompat.R.attr.colorPrimary);
        int textoOn = color(com.google.android.material.R.attr.colorOnPrimary);
        int fondoOff = color(com.google.android.material.R.attr.colorSurface);
        int textoOff = color(com.google.android.material.R.attr.colorOutline);
        ArgbEvaluator mezcla = new ArgbEvaluator();
        for (int i = 0; i < 7; i++) {
            TextView t = dias[i];
            GradientDrawable fondo = (GradientDrawable) t.getBackground();
            boolean on = encendidos[i];
            int fondoFin = on ? fondoOn : fondoOff;
            int textoFin = on ? textoOn : textoOff;
            float escala = on ? 1f : 0.86f;
            if (!animar) {
                fondo.setColor(fondoFin);
                t.setTextColor(textoFin);
                t.setScaleX(escala);
                t.setScaleY(escala);
                t.setTag(fondoFin);
                continue;
            }
            int fondoIni = t.getTag() instanceof Integer ? (Integer) t.getTag() : fondoOff;
            int textoIni = t.getCurrentTextColor();
            t.setTag(fondoFin);
            long retraso = i * Movimiento.DIA_ESCALON;
            ValueAnimator a = ValueAnimator.ofFloat(0f, 1f);
            a.setDuration(220);
            a.setStartDelay(retraso);
            a.addUpdateListener(an -> {
                float f = (float) an.getAnimatedValue();
                fondo.setColor((int) mezcla.evaluate(f, fondoIni, fondoFin));
                t.setTextColor((int) mezcla.evaluate(f, textoIni, textoFin));
            });
            a.start();
            t.animate().scaleX(escala).scaleY(escala).setStartDelay(retraso).setDuration(Movimiento.DIA)
                    .setInterpolator(Movimiento.REBOTE).start();
        }
    }

    private int color(@AttrRes int attr) {
        TypedValue tv = new TypedValue();
        getContext().getTheme().resolveAttribute(attr, tv, true);
        return tv.data;
    }
}
