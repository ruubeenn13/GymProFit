package es.pmdm.gymprofit.utils;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;

import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;

import es.pmdm.gymprofit.R;

// ============================================================
// ChartMarker — tooltip flotante ÚNICO para todas las gráficas (MPAndroidChart)
// Al tocar un punto/barra muestra una burbuja (card oscura del tema + borde
// naranja de marca, tipografía Barlow Condensed) con el texto que produzca el
// Labeler de cada gráfica (p.ej. "76,5 kg\n08/07"). Se centra sobre el punto.
// ============================================================
public class ChartMarker extends MarkerView {

    // Genera el texto del tooltip para una entrada seleccionada (cada gráfica pone el suyo).
    public interface Labeler { String label(Entry e, Highlight h); }

    private final TextView tv;
    private final Labeler labeler;

    public ChartMarker(Context ctx, Labeler labeler) {
        super(ctx, R.layout.chart_marker);
        this.labeler = labeler;
        tv = findViewById(R.id.tvMarker);

        int surface = attr(ctx, com.google.android.material.R.attr.colorSurface, Color.DKGRAY);
        int marca   = attr(ctx, androidx.appcompat.R.attr.colorPrimary, Color.parseColor("#FF6A00"));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(surface);
        bg.setCornerRadius(dp(ctx, 10));
        bg.setStroke((int) dp(ctx, 1.5f), marca);
        tv.setBackground(bg);
        tv.setTextColor(attr(ctx, com.google.android.material.R.attr.colorOnSurface, Color.WHITE));

        Typeface font = ResourcesCompat.getFont(ctx, R.font.barlow_condensed);
        if (font != null) tv.setTypeface(font, Typeface.BOLD);
    }

    // Refresca el texto cada vez que se selecciona una entrada.
    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        tv.setText(labeler.label(e, highlight));
        super.refreshContent(e, highlight);
    }

    // Centra el tooltip horizontalmente sobre el punto y lo eleva por encima de él.
    @Override
    public MPPointF getOffset() {
        return new MPPointF(-(getWidth() / 2f), -getHeight() - dp(getContext(), 8));
    }

    /**
     * Pone el tooltip en la gráfica. Hay que darle la gráfica (setChartView): sin ella no sabe su
     * tamaño ni su área de datos y se salía por la derecha (revisor, GP-225).
     */
    public static ChartMarker poner(@androidx.annotation.NonNull com.github.mikephil.charting.charts.Chart<?> chart,
                                    @androidx.annotation.NonNull Labeler labeler) {
        ChartMarker m = new ChartMarker(chart.getContext(), labeler);
        m.setChartView(chart);
        chart.setMarker(m);
        return m;
    }

    // Sin esto, MPAndroidChart pega arriba la burbuja que no cabe encima del punto, y con el récord
    // (el punto más alto) la ponía justo encima de él, tapándolo; y por los lados tapaba las cifras
    // de los ejes o se salía de la gráfica (GP-225). Va dentro del área de datos.
    @Override
    public MPPointF getOffsetForDrawingAtPoint(float posX, float posY) {
        com.github.mikephil.charting.charts.Chart<?> c = getChartView();
        float izq = 0, arriba = 0, der = Float.MAX_VALUE, abajo = Float.MAX_VALUE;
        if (c != null) {
            com.github.mikephil.charting.utils.ViewPortHandler v = c.getViewPortHandler();
            izq = v.contentLeft();
            arriba = v.contentTop();
            der = v.contentRight();
            abajo = v.contentBottom();
        }
        float[] o = desplazamiento(posX, posY, getWidth(), getHeight(), izq, arriba, der, abajo, dp(getContext(), 8));
        return new MPPointF(o[0], o[1]);
    }

    /**
     * Dónde va la burbuja respecto al punto (x, y): centrada y encima, a {@code hueco}; si arriba
     * no cabe, debajo, también a {@code hueco}; y dentro del área de datos (sin tapar las cifras de
     * los ejes). Si el área es más estrecha que la burbuja, manda el lado izquierdo.
     *
     * @return {dx, dy} desde el punto hasta la esquina de arriba a la izquierda de la burbuja.
     */
    static float[] desplazamiento(float x, float y, float w, float h, float izq, float arriba, float der,
                                  float abajo, float hueco) {
        float ox = -w / 2f;
        if (x + ox + w > der) ox = der - w - x;
        if (x + ox < izq) ox = izq - x;
        float oy = -h - hueco;
        if (y + oy < arriba) {
            oy = hueco;
            // Debajo tampoco cabe entera: lo más abajo posible sin salir del área.
            if (y + oy + h > abajo) oy = Math.max(arriba - y, abajo - h - y);
        }
        return new float[]{ ox, oy };
    }

    private static float dp(Context ctx, float v) {
        return v * ctx.getResources().getDisplayMetrics().density;
    }

    private static int attr(Context ctx, int attrRes, int fallback) {
        TypedValue tv = new TypedValue();
        if (ctx.getTheme().resolveAttribute(attrRes, tv, true)) return tv.data;
        return fallback;
    }
}
