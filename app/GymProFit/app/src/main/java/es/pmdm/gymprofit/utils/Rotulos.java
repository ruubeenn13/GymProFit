package es.pmdm.gymprofit.utils;

import android.util.TypedValue;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.widget.TextViewCompat;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.ui.widget.BarraNavegacion;

// ============================================================
// Rotulos — los rótulos en mayúsculas de columnas y cifras a letra grande (GP-012).
//
// «SERIE», «ANTERIOR», «EJERCICIOS» en una columna estrecha, a letra 2,0, se partían
// por la mitad de la palabra. Siguen la regla de las etiquetas de la barra (GP-128):
// crecen con la letra del sistema hasta 1,3 y ahí se quedan; y si aun así no caben en
// su columna, se ajustan a una línea en vez de partirse. Son rótulos de la vista: el
// dato al que acompañan sí crece entero, y TalkBack lee la fila completa, no el rótulo.
// ============================================================
public final class Rotulos {

    private Rotulos() { }

    /** Aplica la regla a un rótulo con el tamaño de text_eyebrow. */
    public static void limitar(@NonNull TextView tv) {
        float sp = tv.getResources().getDimension(R.dimen.text_eyebrow)
                / tv.getResources().getDisplayMetrics().scaledDensity;
        float escala = BarraNavegacion.escalaEtiqueta(tv.getResources().getConfiguration().fontScale);
        float maxPx = sp * escala * tv.getResources().getDisplayMetrics().density;
        float minPx = 9 * tv.getResources().getDisplayMetrics().density;
        tv.setMaxLines(1);
        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(tv,
                Math.round(Math.min(minPx, maxPx)), Math.round(maxPx), 1, TypedValue.COMPLEX_UNIT_PX);
    }
}
