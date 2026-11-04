package es.pmdm.gymprofit.ui.widget;

import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.widget.NestedScrollView;

// ============================================================
// PiezasSinCortar — GP-223: al abrirse el teclado, ninguna pieza de una pantalla con scroll
// queda a medias en sus bordes.
//
// El sistema baja lo justo para enseñar el campo con el foco, y en Entrar eso dejaba el
// título cortado bajo el aviso; bajando hasta el texto, quedaba cortado por abajo
// «¿Has olvidado tu contraseña?». Las piezas son los hijos directos del contenido del
// scroll: cada una se ve entera o queda fuera entera, con el campo con el foco entero a la
// vista, en la posición sin cortes más cercana a la del sistema. Si no hay ninguna, no se
// toca. Solo al encogerse la vista (el teclado): el scroll del usuario no se toca.
// ============================================================
public final class PiezasSinCortar {

    private PiezasSinCortar() { }

    /**
     * La posición del scroll en que ninguna pieza queda a medias y el foco se ve entero.
     * Medidas en píxeles, relativas al contenido del scroll.
     *
     * @param scroll     scroll actual
     * @param alto       alto visible de la vista con scroll
     * @param maximo     scroll máximo (alto del contenido menos el visible)
     * @param arriba     borde de arriba de cada pieza
     * @param abajo      borde de abajo de cada pieza, en el mismo orden
     * @param focoArriba borde de arriba del campo con el foco
     * @param focoAbajo  borde de abajo del campo con el foco
     * @return la posición sin cortes más cercana a la actual; la actual si ya no corta nada
     *         o si no hay ninguna
     */
    public static int destino(int scroll, int alto, int maximo, int[] arriba, int[] abajo,
                              int focoArriba, int focoAbajo) {
        if (valida(scroll, alto, maximo, arriba, abajo, focoArriba, focoAbajo)) return scroll;
        // Las candidatas: un borde de una pieza justo arriba o justo abajo de la vista.
        int mejor = scroll;
        int distancia = Integer.MAX_VALUE;
        for (int i = 0; i < arriba.length; i++) {
            for (int s : new int[]{arriba[i], abajo[i], arriba[i] - alto, abajo[i] - alto}) {
                if (!valida(s, alto, maximo, arriba, abajo, focoArriba, focoAbajo)) continue;
                int d = Math.abs(s - scroll);
                if (d < distancia) { mejor = s; distancia = d; }
            }
        }
        return mejor;
    }

    // Dentro del scroll, con el foco entero y ninguna pieza cruzando un borde.
    private static boolean valida(int s, int alto, int maximo, int[] arriba, int[] abajo,
                                  int focoArriba, int focoAbajo) {
        if (s < 0 || s > maximo) return false;
        if (focoArriba < s || focoAbajo > s + alto) return false;
        for (int i = 0; i < arriba.length; i++) {
            if (arriba[i] < s && s < abajo[i]) return false;
            if (arriba[i] < s + alto && s + alto < abajo[i]) return false;
        }
        return true;
    }

    /**
     * Engancha el ajuste a una vista con scroll: cada vez que encoge (se abre el teclado),
     * después de que el sistema haya llevado el foco a la vista. Las piezas son los hijos
     * visibles del contenido del scroll.
     *
     * @param scroll la vista con scroll; su único hijo es un ViewGroup con las piezas
     */
    public static void enganchar(NestedScrollView scroll) {
        scroll.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            if (b - t >= ob - ot) return;
            scroll.post(() -> ajustar(scroll));
        });
    }

    private static void ajustar(NestedScrollView scroll) {
        View foco = scroll.findFocus();
        if (foco == null || scroll.getChildCount() == 0) return;
        ViewGroup contenido = (ViewGroup) scroll.getChildAt(0);
        Rect r = new Rect();
        foco.getDrawingRect(r);
        contenido.offsetDescendantRectToMyCoords(foco, r);
        int n = 0;
        int[] arriba = new int[contenido.getChildCount()];
        int[] abajo = new int[contenido.getChildCount()];
        for (int i = 0; i < contenido.getChildCount(); i++) {
            View pieza = contenido.getChildAt(i);
            if (pieza.getVisibility() != View.VISIBLE) continue;
            arriba[n] = pieza.getTop();
            abajo[n] = pieza.getBottom();
            n++;
        }
        int alto = scroll.getHeight();
        int maximo = Math.max(0, contenido.getHeight() - alto);
        int y = destino(scroll.getScrollY(), alto, maximo, java.util.Arrays.copyOf(arriba, n),
                java.util.Arrays.copyOf(abajo, n), r.top, r.bottom);
        if (y != scroll.getScrollY()) scroll.smoothScrollTo(0, y);
    }
}
