package es.pmdm.gymprofit.ui.widget;

import android.view.View;

import androidx.core.widget.NestedScrollView;

// ============================================================
// TituloSinCortar — GP-223: al abrirse el teclado, el título de arriba de una pantalla con
// scroll no queda a medias.
//
// El sistema baja lo justo para enseñar el campo con el foco, y en Entrar eso deja el
// título cortado bajo el aviso. Si queda a medias, se sube para verlo entero o se baja
// hasta la pieza siguiente para dejarlo fuera entero, siempre que el campo siga entero a la
// vista. Solo al encogerse la vista (el teclado): el scroll del usuario no se toca.
// ============================================================
public final class TituloSinCortar {

    private TituloSinCortar() { }

    /**
     * El scroll en que el título no queda a medias y el foco sigue entero a la vista.
     * Todas las medidas, en píxeles y relativas al contenido de la vista con scroll.
     *
     * @param scroll          scroll actual
     * @param alto            alto visible de la vista con scroll
     * @param tituloArriba    borde de arriba del título
     * @param tituloAbajo     borde de abajo del título
     * @param siguienteArriba borde de arriba de la pieza que sigue al título
     * @param focoArriba      borde de arriba del campo con el foco
     * @param focoAbajo       borde de abajo del campo con el foco
     * @return el scroll al que ir; el actual si el título no está a medias o no hay arreglo
     */
    public static int destino(int scroll, int alto, int tituloArriba, int tituloAbajo,
                              int siguienteArriba, int focoArriba, int focoAbajo) {
        if (scroll <= tituloArriba || scroll >= tituloAbajo) return scroll;
        if (focoArriba >= tituloArriba && focoAbajo <= tituloArriba + alto) return tituloArriba;
        if (focoArriba >= siguienteArriba && focoAbajo <= siguienteArriba + alto) return siguienteArriba;
        return scroll;
    }

    /**
     * Engancha el ajuste a una vista con scroll: cada vez que encoge (se abre el teclado),
     * después de que el sistema haya llevado el foco a la vista.
     *
     * @param scroll    la vista con scroll
     * @param titulo    el título, hijo directo del contenido del scroll
     * @param siguiente la pieza que sigue al título, también hijo directo
     */
    public static void enganchar(NestedScrollView scroll, View titulo, View siguiente) {
        scroll.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            if (b - t >= ob - ot) return;
            scroll.post(() -> {
                View foco = scroll.findFocus();
                if (foco == null || scroll.getChildCount() == 0) return;
                android.graphics.Rect r2 = new android.graphics.Rect();
                foco.getDrawingRect(r2);
                ((android.view.ViewGroup) scroll.getChildAt(0)).offsetDescendantRectToMyCoords(foco, r2);
                int y = destino(scroll.getScrollY(), scroll.getHeight(), titulo.getTop(),
                        titulo.getBottom(), siguiente.getTop(), r2.top, r2.bottom);
                if (y != scroll.getScrollY()) scroll.smoothScrollTo(0, y);
            });
        });
    }
}
