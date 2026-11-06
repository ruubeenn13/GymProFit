package es.pmdm.gymprofit.utils;

import android.text.Layout;
import android.text.StaticLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

// ============================================================
// AltoReservado — un texto que cambia con lo que se elige ocupa siempre el alto del más largo
// (GP-225).
//
// En Tus datos, la descripción de la actividad va encima del selector de experiencia: con letra
// 1,3, «Mucha» la parte en dos líneas y el selector bajaba 24 dp justo al tocarla. Se mide cada
// texto posible con el ancho y la letra del TextView y se le da de alto mínimo el mayor; se
// vuelve a medir si cambia el ancho (giro, ventana partida).
// ============================================================
public final class AltoReservado {

    private AltoReservado() { }

    /**
     * @param tv     el TextView que cambia de texto.
     * @param textos todos los textos que puede llevar.
     */
    public static void para(@NonNull TextView tv, @NonNull int[] textos) {
        // Fuera del layout: un setMinHeight dentro de él no pide otro y se perdía.
        tv.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            if (r - l != or - ol) tv.post(() -> medir(tv, textos));
        });
        tv.post(() -> medir(tv, textos));
    }

    private static void medir(TextView tv, int[] textos) {
        int ancho = tv.getWidth() - tv.getCompoundPaddingLeft() - tv.getCompoundPaddingRight();
        if (ancho <= 0) return;
        int mayor = 0;
        for (int id : textos) {
            CharSequence s = tv.getContext().getText(id);
            StaticLayout sl = StaticLayout.Builder.obtain(s, 0, s.length(), tv.getPaint(), ancho)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(tv.getLineSpacingExtra(), tv.getLineSpacingMultiplier())
                    .setIncludePad(tv.getIncludeFontPadding())
                    .setBreakStrategy(tv.getBreakStrategy())
                    .setHyphenationFrequency(tv.getHyphenationFrequency())
                    .build();
            mayor = Math.max(mayor, sl.getHeight());
        }
        int alto = mayor + tv.getCompoundPaddingTop() + tv.getCompoundPaddingBottom();
        if (tv.getMinHeight() != alto) tv.setMinHeight(alto);
    }
}
