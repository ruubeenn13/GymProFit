package es.pmdm.gymprofit.ui.widget;

import android.util.TypedValue;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.widget.TextViewCompat;

// ============================================================
// TextoQueEncoge — un rótulo que va siempre en una línea y, si no cabe, encoge (GP-285, lote 1b).
//
// «HOY TOCA · DÍA 1 DE 4» junto a las siluetas de «Hoy toca»: a letra 1,3 dejaba el «4» solo
// en una segunda línea. En vez de partirse, baja de tamaño hasta que cabe, y nunca por debajo
// del que tiene a letra 1,0 (el mínimo de la app).
//
// Se activa desde código y no con los atributos autoSize* del layout a propósito: el ajuste de
// AppCompat exige que el máximo sea mayor que el mínimo, y a letra 1,0 (o menor) los dos son el
// mismo. Puesto en el layout, la pantalla no llegaba a abrirse con la letra normal
// (IllegalArgumentException al inflar). Aquí, sin margen no se activa: el texto ya está en su
// mínimo y solo se le pide una línea.
// ============================================================
public final class TextoQueEncoge {

    private TextoQueEncoge() { }

    /**
     * Si hay margen para encoger: el tamaño de ahora pasa del mínimo en al menos un píxel.
     *
     * @param ahoraPx  tamaño de letra actual, en px enteros.
     * @param minimoPx tamaño mínimo, en px enteros.
     */
    public static boolean hayMargen(int ahoraPx, int minimoPx) {
        return minimoPx > 0 && ahoraPx > minimoPx;
    }

    /**
     * Deja el texto en una línea y, si tiene de dónde, encogiendo hasta el mínimo para caber.
     * El texto tiene que tener un ancho acotado (no «el de su contenido»).
     *
     * @param minimoPx el tamaño por debajo del que no baja, en px.
     */
    public static void enUnaLinea(@NonNull TextView texto, float minimoPx) {
        texto.setMaxLines(1);
        int ahora = (int) Math.floor(texto.getTextSize()), minimo = (int) Math.ceil(minimoPx);
        if (!hayMargen(ahora, minimo)) return;
        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(texto, minimo, ahora, 1, TypedValue.COMPLEX_UNIT_PX);
    }
}
