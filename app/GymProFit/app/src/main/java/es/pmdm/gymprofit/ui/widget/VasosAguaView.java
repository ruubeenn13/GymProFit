package es.pmdm.gymprofit.ui.widget;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import es.pmdm.gymprofit.R;

// ============================================================
// VasosAguaView — los vasos de la tarjeta de agua de Inicio (GP-216, tablero 13): uno por vaso
// del objetivo, llenos los bebidos y vacíos los que faltan, en filas que se parten solas.
//
// Es de solo lectura (tocar la tarjeta lleva a Nutrición, donde se añaden vasos), así que es
// una vista dibujada a mano que nace con su accesibilidad (DEC-019): quien la usa pone una
// descripción con lo que cuenta («6 de 9 vasos») y la vista no se anuncia por su cuenta. Lleno
// y vacío se distinguen por el color y por la altura del relleno, no solo por el color.
// ============================================================
public class VasosAguaView extends android.view.View {

    private final Paint pincel = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path trazo = new Path();
    private final RectF rect = new RectF();

    private final float anchoVaso;
    private final float altoVaso;
    private final float hueco;
    private final float radioArriba;
    private final float radioAbajo;
    private final int colorLleno;
    private final int colorVacio;

    private int total;
    private int llenos;

    public VasosAguaView(@NonNull Context context) { this(context, null); }

    public VasosAguaView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        Resources r = getResources();
        float d = r.getDisplayMetrics().density;
        anchoVaso = 14 * d;
        altoVaso = 20 * d;
        hueco = r.getDimension(R.dimen.espacio_4);
        radioArriba = r.getDimension(R.dimen.espacio_4) * 0.75f;
        radioAbajo = r.getDimension(R.dimen.espacio_4) * 1.25f;
        colorLleno = ContextCompat.getColor(context, R.color.gp_agua);
        colorVacio = ContextCompat.getColor(context, R.color.gp_meter_track);
    }

    /**
     * Fija los vasos y repinta.
     *
     * @param total  vasos a dibujar (los del objetivo, o los bebidos si son más).
     * @param llenos cuántos están llenos.
     */
    public void setVasos(int total, int llenos) {
        this.total = Math.max(0, total);
        this.llenos = Math.max(0, Math.min(llenos, this.total));
        requestLayout();
        invalidate();
    }

    private int porFila(int ancho) {
        return Math.max(1, (int) ((ancho + hueco) / (anchoVaso + hueco)));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int ancho = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        int filas = total == 0 ? 0 : (int) Math.ceil(total / (double) porFila(ancho - getPaddingLeft() - getPaddingRight()));
        int alto = filas == 0 ? 0 : Math.round(filas * altoVaso + (filas - 1) * hueco);
        setMeasuredDimension(ancho, alto);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        int disponible = getWidth() - getPaddingLeft() - getPaddingRight();
        int porFila = porFila(disponible);
        float[] radios = {radioArriba, radioArriba, radioArriba, radioArriba,
                radioAbajo, radioAbajo, radioAbajo, radioAbajo};
        for (int i = 0; i < total; i++) {
            float x = getPaddingLeft() + (i % porFila) * (anchoVaso + hueco);
            float y = (i / porFila) * (altoVaso + hueco);
            pincel.setColor(i < llenos ? colorLleno : colorVacio);
            rect.set(x, y, x + anchoVaso, y + altoVaso);
            trazo.reset();
            trazo.addRoundRect(rect, radios, Path.Direction.CW);
            canvas.drawPath(trazo, pincel);
        }
    }
}
