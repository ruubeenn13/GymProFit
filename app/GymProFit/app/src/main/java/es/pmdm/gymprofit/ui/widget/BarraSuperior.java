package es.pmdm.gymprofit.ui.widget;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.widget.NestedScrollView;

// ============================================================
// BarraSuperior — el fondo que sale detrás del botón de volver al desplazar (GP-216, lote 2).
// En la ficha del ejercicio y los detalles de rutina y programa el botón de volver flota sobre el
// contenido; sin fondo, lo que pasa por debajo se leía cortado por el círculo («De qué va» salía
// «← qué va»). La barra tiene el color del fondo de la pantalla y solo se ve cuando se ha
// desplazado algo: arriba del todo no tapa la cabecera.
// ============================================================
public final class BarraSuperior {

    private BarraSuperior() { }

    /** Enseña la barra en cuanto el contenido se ha desplazado y la esconde al volver arriba. */
    public static void enlazar(@NonNull NestedScrollView scroll, @NonNull View barra) {
        scroll.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener)
                (v, x, y, antesX, antesY) -> barra.setVisibility(y > 0 ? View.VISIBLE : View.INVISIBLE));
    }
}
