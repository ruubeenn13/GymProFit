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
        int marca   = attr(ctx, com.google.android.material.R.attr.colorPrimary, Color.parseColor("#FF6A00"));

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

    private static float dp(Context ctx, float v) {
        return v * ctx.getResources().getDisplayMetrics().density;
    }

    private static int attr(Context ctx, int attrRes, int fallback) {
        TypedValue tv = new TypedValue();
        if (ctx.getTheme().resolveAttribute(attrRes, tv, true)) return tv.data;
        return fallback;
    }
}
