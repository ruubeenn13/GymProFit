package es.pmdm.gymprofit.utils;

import android.view.View;

import androidx.annotation.NonNull;

// ============================================================
// AltoIgual — las vistas de una fila, a la altura de la más alta (GP-225).
//
// En Estadísticas de nutrición, con letra 1,3 «DÍAS REGISTRADOS» va en dos líneas y su tarjeta
// crecía sola, más alta que la de al lado. Un match_parent dentro de una fila wrap_content con
// pesos no sirve: LinearLayout las medía a 0 (se vio a 3 px en el emulador). Se miden, y a todas
// se les da de mínimo el alto de la mayor.
// ============================================================
public final class AltoIgual {

    private AltoIgual() { }

    /** Iguala las vistas cada vez que cambia el alto de alguna. */
    public static void fila(@NonNull View... vistas) {
        View.OnLayoutChangeListener l = (v, a, t, d, b, oa, ot, od, ob) -> {
            if (b - t != ob - ot) v.post(() -> igualar(vistas));
        };
        for (View v : vistas) v.addOnLayoutChangeListener(l);
    }

    private static void igualar(View[] vistas) {
        int mayor = 0;
        for (View v : vistas) {
            // Lo que mide sin el mínimo puesto: si no, nunca encogería.
            v.setMinimumHeight(0);
            v.measure(View.MeasureSpec.makeMeasureSpec(v.getWidth(), View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            mayor = Math.max(mayor, v.getMeasuredHeight());
        }
        for (View v : vistas) {
            if (v.getHeight() != mayor) {
                v.setMinimumHeight(mayor);
                v.requestLayout();
            } else {
                v.setMinimumHeight(mayor);
            }
        }
    }
}
