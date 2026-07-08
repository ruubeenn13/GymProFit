package es.pmdm.gymprofit.utils;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.TypedValue;

import androidx.core.content.res.ResourcesCompat;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import es.pmdm.gymprofit.R;

// ============================================================
// ChartStyler — estilo ATLÉTICO ÚNICO para TODAS las gráficas (MPAndroidChart)
// Punto único de configuración visual para que peso, nutrición y progresión de
// ejercicio se vean idénticas de estilo: fondo transparente, serie naranja de
// marca (colorPrimary), tipografía Barlow Condensed, sin rejilla ruidosa, ejes
// discretos, sin leyenda ni descripción, animación breve. Los colores se
// resuelven del TEMA (claro/oscuro) para respetar el modo activo.
// ============================================================
public final class ChartStyler {

    private ChartStyler() { }

    // Configura el "chrome" de un LineChart al estilo de la app.
    // xFormatter puede ser null (usa los valores crudos del eje X).
    public static void styleLine(LineChart chart, ValueFormatter xFormatter) {
        Context ctx = chart.getContext();
        int onSurface = attr(ctx, com.google.android.material.R.attr.colorOnSurface, Color.GRAY);
        int onVariant = attr(ctx, com.google.android.material.R.attr.colorOnSurfaceVariant, Color.GRAY);
        Typeface tf = fuente(ctx);

        chart.setBackgroundColor(Color.TRANSPARENT);
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.setDrawGridBackground(false);
        chart.setScaleYEnabled(false);            // solo zoom/scroll horizontal
        chart.setExtraBottomOffset(8f);
        chart.setNoDataText("");                  // el empty state lo pinta la pantalla
        chart.animateX(500);

        ejeX(chart.getXAxis(), onVariant, tf, xFormatter);
        ejeYIzq(chart.getAxisLeft(), onVariant, tf);
        chart.getAxisRight().setEnabled(false);
    }

    // Configura el "chrome" de un BarChart al estilo de la app.
    public static void styleBar(BarChart chart, ValueFormatter xFormatter) {
        Context ctx = chart.getContext();
        int onVariant = attr(ctx, com.google.android.material.R.attr.colorOnSurfaceVariant, Color.GRAY);
        Typeface tf = fuente(ctx);

        chart.setBackgroundColor(Color.TRANSPARENT);
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.setDrawGridBackground(false);
        chart.setDrawValueAboveBar(true);
        chart.setScaleEnabled(false);
        chart.setExtraBottomOffset(8f);
        chart.setNoDataText("");
        chart.animateY(500);

        ejeX(chart.getXAxis(), onVariant, tf, xFormatter);
        ejeYIzq(chart.getAxisLeft(), onVariant, tf);
        chart.getAxisRight().setEnabled(false);
    }

    // Aplica el color de marca (naranja) y el acabado suave a una serie de línea.
    public static void styleLineDataSet(LineDataSet ds, Context ctx) {
        int marca = attr(ctx, com.google.android.material.R.attr.colorPrimary, Color.parseColor("#FF6A00"));

        ds.setColor(marca);
        ds.setLineWidth(2.4f);
        ds.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER);   // curva suave
        ds.setDrawCircles(true);
        ds.setCircleColor(marca);
        ds.setCircleRadius(3.5f);
        ds.setDrawCircleHole(false);
        ds.setDrawValues(false);
        ds.setHighlightEnabled(true);
        ds.setHighLightColor(marca);
        ds.setDrawFilled(true);
        ds.setFillColor(marca);
        ds.setFillAlpha(38);                               // relleno naranja translúcido
    }

    // Aplica el color de marca a una serie de barras.
    public static void styleBarDataSet(BarDataSet ds, Context ctx) {
        int marca = attr(ctx, com.google.android.material.R.attr.colorPrimary, Color.parseColor("#FF6A00"));
        Typeface tf = fuente(ctx);

        ds.setColor(marca);
        ds.setDrawValues(true);
        ds.setValueTextColor(attr(ctx, com.google.android.material.R.attr.colorOnSurfaceVariant, Color.GRAY));
        ds.setValueTextSize(9f);
        ds.setValueTypeface(tf);
        ds.setHighlightEnabled(false);
    }

    // --- helpers privados ---

    private static void ejeX(XAxis x, int color, Typeface tf, ValueFormatter fmt) {
        x.setPosition(XAxis.XAxisPosition.BOTTOM);
        x.setDrawGridLines(false);
        x.setDrawAxisLine(false);
        x.setTextColor(color);
        x.setTypeface(tf);
        x.setTextSize(10f);
        x.setGranularity(1f);
        x.setLabelCount(4, false);
        if (fmt != null) x.setValueFormatter(fmt);
    }

    private static void ejeYIzq(YAxis y, int color, Typeface tf) {
        y.setDrawAxisLine(false);
        y.setDrawGridLines(true);
        y.setGridColor(conAlfa(color, 0x22));   // rejilla horizontal MUY sutil
        y.setGridLineWidth(0.8f);
        y.setTextColor(color);
        y.setTypeface(tf);
        y.setTextSize(10f);
        y.setLabelCount(4, false);
    }

    private static Typeface fuente(Context ctx) {
        Typeface tf = ResourcesCompat.getFont(ctx, R.font.barlow_condensed);
        return tf != null ? tf : Typeface.DEFAULT;
    }

    private static int attr(Context ctx, int attrRes, int fallback) {
        TypedValue tv = new TypedValue();
        if (ctx.getTheme().resolveAttribute(attrRes, tv, true)) return tv.data;
        return fallback;
    }

    private static int conAlfa(int rgb, int alfa) {
        return (rgb & 0x00FFFFFF) | (alfa << 24);
    }
}
