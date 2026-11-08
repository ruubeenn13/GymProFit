package es.pmdm.gymprofit.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.utils.TiempoRutinas;

// ============================================================
// BloquesRutinaView — los ejercicios de una rutina en bloques (GP-243, tablero Tiempo-1).
//
// Un bloque por ejercicio, todos del mismo ancho: lleno en naranja y con una raya por serie
// si se queda, vacío y con borde discontinuo si sale. Primero los que se quedan, en su
// orden, y luego los que salen (TiempoRutinas.bloques).
//
// Decorativa para TalkBack (DEC-019): lo que dice va en la frase de la tarjeta («3 de 7
// ejercicios. Los básicos, a 2 series.»), así que no se recorre suelta. Contrastes en
// DESIGN.md: el naranja (5,03:1 y 4,58:1) y el borde discontinuo (6,10:1 y 5,44:1), contra la
// superficie 2 de la tarjeta de la hoja.
// ============================================================
public class BloquesRutinaView extends View {

    private final Paint lleno = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borde = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint raya = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private final float hueco, radio, rayaAncho, rayaAlto, rayaHueco, alto;
    private List<TiempoRutinas.Bloque> bloques = new ArrayList<>();

    public BloquesRutinaView(@NonNull Context context) {
        this(context, null);
    }

    public BloquesRutinaView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        float d = getResources().getDisplayMetrics().density;
        hueco = getResources().getDimension(R.dimen.espacio_4);
        radio = getResources().getDimension(R.dimen.bloque_rutina_radio);
        alto = getResources().getDimension(R.dimen.bloque_rutina_alto);
        rayaAncho = 3 * d;
        rayaAlto = 12 * d;
        rayaHueco = 3 * d;
        lleno.setColor(ContextCompat.getColor(context, R.color.gp_primary));
        borde.setStyle(Paint.Style.STROKE);
        borde.setStrokeWidth(getResources().getDimension(R.dimen.bloque_rutina_borde));
        // El texto secundario y no gp_stroke: sobre la superficie 2 de la hoja, gp_stroke daba
        // 2,86:1 en oscuro y 2,77:1 en claro; este, 6,10:1 y 5,44:1.
        borde.setColor(ContextCompat.getColor(context, R.color.gp_text_secondary));
        borde.setPathEffect(new DashPathEffect(new float[]{4 * d, 3 * d}, 0));
        // Las rayas, sobre el naranja: el texto sobre la marca al 55 %, como el tablero.
        raya.setColor(ContextCompat.getColor(context, R.color.gp_on_primary));
        raya.setAlpha(140);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    /** Los bloques que pinta, en su orden. */
    public void setBloques(@NonNull List<TiempoRutinas.Bloque> bloques) {
        this.bloques = new ArrayList<>(bloques);
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int ancho = MeasureSpec.getSize(widthMeasureSpec);
        setMeasuredDimension(ancho, resolveSize(Math.round(alto), heightMeasureSpec));
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        int n = bloques.size();
        if (n == 0) return;
        float ancho = (getWidth() - getPaddingLeft() - getPaddingRight() - hueco * (n - 1)) / n;
        float x = getPaddingLeft();
        float media = borde.getStrokeWidth() / 2f;
        for (TiempoRutinas.Bloque b : bloques) {
            if (b.queda) {
                r.set(x, 0, x + ancho, getHeight());
                canvas.drawRoundRect(r, radio, radio, lleno);
                pintarRayas(canvas, x, ancho, b.series);
            } else {
                r.set(x + media, media, x + ancho - media, getHeight() - media);
                canvas.drawRoundRect(r, radio, radio, borde);
            }
            x += ancho + hueco;
        }
    }

    // Una raya por serie, centradas; si no caben en el bloque, las que quepan.
    private void pintarRayas(Canvas canvas, float x, float ancho, int series) {
        int caben = (int) Math.max(0, (ancho - 2 * rayaHueco + rayaHueco) / (rayaAncho + rayaHueco));
        int n = Math.min(series, caben);
        if (n <= 0) return;
        float total = n * rayaAncho + (n - 1) * rayaHueco;
        float ix = x + (ancho - total) / 2f;
        float iy = (getHeight() - rayaAlto) / 2f;
        for (int i = 0; i < n; i++) {
            r.set(ix, iy, ix + rayaAncho, iy + rayaAlto);
            canvas.drawRoundRect(r, rayaAncho / 2f, rayaAncho / 2f, raya);
            ix += rayaAncho + rayaHueco;
        }
    }
}
