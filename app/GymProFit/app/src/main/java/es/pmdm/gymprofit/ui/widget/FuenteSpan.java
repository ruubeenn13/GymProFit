package es.pmdm.gymprofit.ui.widget;

import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.style.MetricAffectingSpan;

import androidx.annotation.NonNull;

// ============================================================
// FuenteSpan — otra familia dentro de un mismo texto (GP-243): el «min» de los minutos en Barlow
// junto a una cifra condensada, y el «min» o «ahora» de una opción del selector. TypefaceSpan con
// un Typeface es de la API 28 y la app empieza en la 24 (lo mismo que hace TextosTarjeta con su
// «kg»).
// ============================================================
public final class FuenteSpan extends MetricAffectingSpan {

    private final Typeface tf;

    public FuenteSpan(@NonNull Typeface tf) {
        this.tf = tf;
    }

    @Override
    public void updateDrawState(@NonNull TextPaint p) {
        p.setTypeface(tf);
    }

    @Override
    public void updateMeasureState(@NonNull TextPaint p) {
        p.setTypeface(tf);
    }
}
