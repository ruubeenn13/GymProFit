package es.pmdm.gymprofit.ui.nutricion;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ReplacementSpan;
import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import es.pmdm.gymprofit.R;

// ============================================================
// MarcaPlato — la marca «Plato» junto al nombre de un plato propio (tablero 8, lote 1.6.6)
//
// Una píldora pequeña, con el fondo de las superficies elevadas y la letra de lo
// secundario, pegada al nombre como la insignia de «datos revisados» (Insignia): dentro
// del texto, para que con letra grande baje con la última palabra y no se quede sola en
// el borde de la fila. TalkBack no la lee aquí: la fila dice «plato» en su descripción.
// ============================================================
public final class MarcaPlato {

    private MarcaPlato() {
    }

    /** El nombre con la marca detrás si es un plato; si no, tal cual. */
    @NonNull
    public static CharSequence nombre(@NonNull Context ctx, @NonNull CharSequence nombre, boolean esPlato) {
        if (!esPlato) return nombre;
        SpannableStringBuilder sb = new SpannableStringBuilder(nombre).append(" ");
        int desde = sb.length();
        sb.append(ctx.getString(R.string.plato_marca));
        sb.setSpan(new Pildora(ctx), desde, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return sb;
    }

    /** La píldora: 12 sp en negrita, 8 dp a los lados y esquinas de 10 dp. */
    static final class Pildora extends ReplacementSpan {
        private final int fondo;
        private final int texto;
        private final float tamano;
        private final float lado;
        private final float radio;
        private final float separa;

        Pildora(@NonNull Context ctx) {
            float d = ctx.getResources().getDisplayMetrics().density;
            fondo = ContextCompat.getColor(ctx, R.color.gp_surface_2);
            TypedValue tv = new TypedValue();
            ctx.getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurfaceVariant, tv, true);
            texto = tv.data;
            tamano = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12, ctx.getResources().getDisplayMetrics());
            lado = 8 * d;
            radio = 10 * d;
            separa = 4 * d;
        }

        private Paint pintura(@NonNull Paint base) {
            Paint p = new Paint(base);
            p.setTextSize(tamano);
            p.setTypeface(Typeface.create(base.getTypeface(), Typeface.BOLD));
            return p;
        }

        @Override
        public int getSize(@NonNull Paint paint, CharSequence text, int start, int end, @Nullable Paint.FontMetricsInt fm) {
            return Math.round(separa + lado * 2 + pintura(paint).measureText(text, start, end));
        }

        @Override
        public void draw(@NonNull Canvas canvas, CharSequence text, int start, int end, float x, int top, int y,
                         int bottom, @NonNull Paint paint) {
            Paint p = pintura(paint);
            Paint.FontMetrics m = p.getFontMetrics();
            float ancho = p.measureText(text, start, end) + lado * 2;
            float alto = (m.descent - m.ascent) + 2 * (radio / 5);
            float centro = (top + bottom) / 2f;
            RectF caja = new RectF(x + separa, centro - alto / 2, x + separa + ancho, centro + alto / 2);
            Paint f = new Paint(Paint.ANTI_ALIAS_FLAG);
            f.setColor(fondo);
            canvas.drawRoundRect(caja, radio, radio, f);
            p.setColor(texto);
            float base = centro - (m.ascent + m.descent) / 2;
            canvas.drawText(text, start, end, x + separa + lado, base, p);
        }
    }
}
