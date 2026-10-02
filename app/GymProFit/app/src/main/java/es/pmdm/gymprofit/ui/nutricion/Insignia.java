package es.pmdm.gymprofit.ui.nutricion;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ImageSpan;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import es.pmdm.gymprofit.R;

// ============================================================
// Insignia — el nombre de un alimento con la insignia azul de «datos revisados» detrás
// (lote 1.6.1). Va dentro del texto, pegada a la última palabra, como en el lienzo: un
// icono aparte se quedaba en el borde derecho de la fila. TalkBack no lee la imagen; la
// fila ya dice «datos revisados» en su descripción.
// ============================================================
public final class Insignia {

    private Insignia() {
    }

    /**
     * @param nombre    el nombre.
     * @param revisado  si lleva la insignia.
     * @param tamanoDp  el lado de la insignia (18 en las filas, como en el lienzo).
     */
    @NonNull
    public static CharSequence nombre(@NonNull Context ctx, @NonNull String nombre, boolean revisado, int tamanoDp) {
        if (!revisado) return nombre;
        Drawable d = ContextCompat.getDrawable(ctx, R.drawable.ic_ms_verified_fill);
        if (d == null) return nombre;
        d = d.mutate();
        d.setTint(ContextCompat.getColor(ctx, R.color.gp_macro_proteinas));
        int lado = Math.round(tamanoDp * ctx.getResources().getDisplayMetrics().density);
        d.setBounds(0, 0, lado, lado);
        SpannableStringBuilder sb = new SpannableStringBuilder(nombre).append("  ");
        sb.setSpan(new ImageSpan(d, ImageSpan.ALIGN_CENTER), sb.length() - 1, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return sb;
    }
}
