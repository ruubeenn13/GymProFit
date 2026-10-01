package es.pmdm.gymprofit.ui.alta;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.text.DecimalFormatSymbols;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.utils.FechaUtils;
import es.pmdm.gymprofit.utils.Movimiento;

// ============================================================
// DemoBienvenida — la función, en marcha (GP-103, tablero 1 del lienzo).
//
// Un bucle de 8 s con los fotogramas del lienzo: se teclea «62,5» y «8» en la tercera
// serie, el check se marca con su onda, empieza el descanso con su barra, la tarjeta de
// la serie se aparta y sube el resumen de la sesión con un récord nuevo, que lo celebra
// (momento 12, sin vibrar: en bucle sería una vibración cada 8 segundos).
//
// Con «Quitar animaciones», el fotograma final y quieto: la serie apuntada, el descanso
// y el resumen con el récord a la vista. Para TalkBack es una imagen con su descripción.
// ============================================================
public class DemoBienvenida extends FrameLayout {

    /** La duración del bucle, la del lienzo. */
    static final long BUCLE = 8000;
    /** Cuándo sale el récord dentro del bucle (46 %). */
    private static final long RECORD = 3680;
    /** El fotograma que se enseña quieto: todo hecho y a la vista. */
    private static final long QUIETO = 4800;

    private final View serie, resumen, descanso, barraDescanso, fila3, onda, check, checkIcono, record, trofeo;
    private final TextView kg3, reps3;
    private final Drawable fondoFila3;
    private final String[] tecleoKg;
    private final float dp;
    @Nullable private ValueAnimator bucle;
    private long anterior;

    public DemoBienvenida(@NonNull Context context) {
        this(context, null);
    }

    public DemoBienvenida(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.view_demo_bienvenida, this, true);
        setClipChildren(false);
        dp = getResources().getDisplayMetrics().density;
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        setContentDescription(context.getString(R.string.demo_a11y));
        // Lo de dentro no se lee suelto: lo dice la descripción de la imagen.
        for (int i = 0; i < getChildCount(); i++) {
            getChildAt(i).setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        }

        serie = findViewById(R.id.demoSerie);
        resumen = findViewById(R.id.demoResumen);
        descanso = findViewById(R.id.demoDescanso);
        barraDescanso = findViewById(R.id.demoBarraDescanso);
        record = findViewById(R.id.demoRecord);
        trofeo = findViewById(R.id.demoTrofeo);
        rellenar(findViewById(R.id.demoFila1), 1, "60", "8");
        rellenar(findViewById(R.id.demoFila2), 2, "60", "8");
        fila3 = findViewById(R.id.demoFila3);
        rellenar(fila3, 3, "", "");
        ((TextView) fila3.findViewById(R.id.demoAnterior)).setText(context.getString(R.string.demo_anterior, 60, 7));
        kg3 = fila3.findViewById(R.id.demoKg);
        reps3 = fila3.findViewById(R.id.demoReps);
        onda = fila3.findViewById(R.id.demoOnda);
        check = fila3.findViewById(R.id.demoCheck);
        checkIcono = fila3.findViewById(R.id.demoCheckIcono);
        fondoFila3 = fila3.getBackground().mutate();
        // El check sin marcar es un aro gris, como en la sesión en vivo.
        GradientDrawable aro = new GradientDrawable();
        aro.setShape(GradientDrawable.OVAL);
        aro.setStroke(Math.round(2 * dp), color(com.google.android.material.R.attr.colorOutline));
        ((View) check.getParent()).setBackground(aro);

        char coma = DecimalFormatSymbols.getInstance(FechaUtils.localeDeLaApp(context)).getDecimalSeparator();
        tecleoKg = new String[]{"6", "62", "62" + coma, "62" + coma + "5"};
        barraDescanso.setPivotX(0f);
        aplicar(Movimiento.quieto(context) ? QUIETO : 0);
    }

    private void rellenar(View fila, int num, String kg, String reps) {
        ((TextView) fila.findViewById(R.id.demoNum)).setText(String.valueOf(num));
        ((TextView) fila.findViewById(R.id.demoAnterior)).setText(getContext().getString(R.string.demo_anterior, 60, 8));
        ((TextView) fila.findViewById(R.id.demoKg)).setText(kg);
        ((TextView) fila.findViewById(R.id.demoReps)).setText(reps);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (Movimiento.quieto(getContext())) {
            aplicar(QUIETO);
            return;
        }
        anterior = 0;
        bucle = ValueAnimator.ofFloat(0f, BUCLE);
        bucle.setDuration(BUCLE);
        bucle.setRepeatCount(ValueAnimator.INFINITE);
        bucle.setInterpolator(Movimiento.LINEAL);
        bucle.addUpdateListener(a -> {
            long t = (long) (float) a.getAnimatedValue();
            // El récord se celebra una vez por vuelta, al cruzar su momento.
            if (anterior < RECORD && t >= RECORD) {
                Movimiento.celebrarRecord((ViewGroup) record, trofeo,
                        ContextCompat.getColor(getContext(), R.color.gp_gold),
                        color(com.google.android.material.R.attr.colorOnPrimaryContainer), 0, false);
            }
            anterior = t;
            aplicar(t);
        });
        bucle.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (bucle != null) bucle.cancel();
        bucle = null;
        super.onDetachedFromWindow();
    }

    // Todo en el instante t (ms) del bucle, con los porcentajes del lienzo.
    private void aplicar(long t) {
        // vivo: la serie, a la vista hasta el 37 %; apartada del 44 al 90 %; vuelve al 97 %.
        float aparta = suave(t, 2960, 3520) - suave(t, 7200, 7760);
        serie.setAlpha(1f - 0.65f * aparta);
        serie.setTranslationY(-10 * dp * aparta);

        // Tecleo: 6, 2, la coma y 5; luego las reps. Se borran entre el 85 y el 89 %.
        long[] teclas = {400, 550, 680, 820};
        int escritos = 0;
        for (long k : teclas) if (t >= k) escritos++;
        boolean borrado = t >= 6900;
        kg3.setText(borrado || escritos == 0 ? "" : tecleoKg[escritos - 1]);
        reps3.setText(!borrado && t >= 1050 ? "8" : "");

        // fila: se tiñe del 19 al 22 % y vuelve del 90 al 96 %.
        float tinte = tramo(t, 1520, 1760) - tramo(t, 7200, 7680);
        fondoFila3.setAlpha(Math.round(255 * tinte));

        // chk: el check se rellena del 18 al 19,5 % y se vacía del 90 al 96 %.
        check.setAlpha(tramo(t, 1440, 1560) - tramo(t, 7200, 7680));
        // chkIcon: salta de 0,4 a 1,25 y a 1 entre el 18,5 y el 23 %.
        float icono = tramo(t, 1480, 1680);
        float asienta = tramo(t, 1680, 1840);
        float escala = icono < 1f ? 0.4f + 0.85f * icono : 1.25f - 0.25f * asienta;
        float sale = tramo(t, 7200, 7680);
        checkIcono.setScaleX(escala - 0.6f * sale);
        checkIcono.setScaleY(escala - 0.6f * sale);
        checkIcono.setAlpha(icono > 0 ? 1f - sale : 0f);
        // onda: sale del check entre el 17 y el 23 %.
        float o = tramo(t, 1400, 1840);
        onda.setScaleX(0.4f + 1.4f * o);
        onda.setScaleY(0.4f + 1.4f * o);
        onda.setAlpha(t >= 1360 && t < 1840 ? 0.6f * (1f - o) : 0f);

        // descanso: baja y aparece del 22 al 25 %; se va del 90 al 95 %.
        float d = tramo(t, 1760, 2000);
        float dFuera = tramo(t, 7200, 7600);
        descanso.setAlpha(d - dFuera);
        descanso.setTranslationY(-6 * dp * (1f - d));
        barraDescanso.setScaleX(1f - 0.2f * tramo(t, 2000, 2960));

        // resumen: sube con rebote del 38 al 46 %; baja del 90 al 96 %.
        float r = Movimiento.REBOTE.getInterpolation(tramo(t, 3040, 3680));
        float rFuera = tramo(t, 7200, 7680);
        float visible = r * (1f - rFuera);
        resumen.setAlpha(Math.min(1f, Math.max(0f, t < 3040 ? 0f : visible)));
        resumen.setTranslationY(40 * dp * (1f - visible));
        resumen.setScaleX(0.96f + 0.04f * visible);
        resumen.setScaleY(0.96f + 0.04f * visible);
    }

    // 0 antes de a, 1 después de b, lineal entre medias.
    private static float tramo(long t, long a, long b) {
        if (t <= a) return 0f;
        if (t >= b) return 1f;
        return (t - a) / (float) (b - a);
    }

    // Como tramo, con entrada y salida suaves (el ease-in-out del lienzo).
    private static float suave(long t, long a, long b) {
        float x = tramo(t, a, b);
        return x * x * (3 - 2 * x);
    }

    private int color(int attr) {
        TypedValue tv = new TypedValue();
        getContext().getTheme().resolveAttribute(attr, tv, true);
        return tv.data;
    }
}
