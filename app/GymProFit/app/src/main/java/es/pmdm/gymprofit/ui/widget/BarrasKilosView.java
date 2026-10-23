package es.pmdm.gymprofit.ui.widget;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.google.android.material.color.MaterialColors;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.utils.ProgresoSemanal;

// ============================================================
// BarrasKilosView — las barras de «Tu progreso» de Inicio (GP-216, tablero 13): los kilos movidos
// en cada una de las últimas semanas, con su etiqueta debajo.
//
// La semana en curso va en el naranja de la marca y las anteriores en el color de borde, que
// llega a 3:1 contra la tarjeta (un gráfico no depende solo del color: la cifra de cada semana
// va en la descripción). Es una vista dibujada a mano, así que nace con su accesibilidad
// (DEC-019): una sola descripción para TalkBack con lo que cuenta el gráfico, que pone quien
// la usa con {@link #setContentDescription(CharSequence)}.
// ============================================================
public class BarrasKilosView extends android.view.View {

    private final Paint pincelBarra = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pincelTexto = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path trazo = new Path();
    private final RectF rect = new RectF();

    private double[] kilos = new double[0];
    private String[] etiquetas = new String[0];
    private float[] alturas = new float[0];

    private final float altoBarras;
    private final float hueco;
    private final float radioArriba;
    private final float radioAbajo;
    private final float separacionEtiqueta;
    private final int colorPasadas;
    private final int colorActual;

    public BarrasKilosView(@NonNull Context context) { this(context, null); }

    public BarrasKilosView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        Resources r = getResources();
        altoBarras = 76 * r.getDisplayMetrics().density;
        hueco = r.getDimension(R.dimen.espacio_8) + r.getDimension(R.dimen.espacio_4) / 2f;
        radioArriba = r.getDimension(R.dimen.espacio_8);
        radioAbajo = r.getDimension(R.dimen.espacio_4);
        separacionEtiqueta = r.getDimension(R.dimen.espacio_8);
        colorPasadas = ContextCompat.getColor(context, R.color.gp_stroke);
        colorActual = MaterialColors.getColor(this, androidx.appcompat.R.attr.colorPrimary);
        pincelTexto.setTextAlign(Paint.Align.CENTER);
        pincelTexto.setTextSize(r.getDimension(R.dimen.text_eyebrow));
        pincelTexto.setColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurfaceVariant));
        Typeface barlow = ResourcesCompat.getFont(context, R.font.barlow);
        if (barlow != null) pincelTexto.setTypeface(barlow);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
    }

    /**
     * Fija las semanas y repinta.
     *
     * @param kilos     los kilos de cada semana, de la más antigua a la más reciente (la última, en curso).
     * @param etiquetas el texto de debajo de cada barra; mismo largo que {@code kilos}.
     */
    public void setSemanas(@NonNull double[] kilos, @NonNull String[] etiquetas) {
        this.kilos = kilos;
        this.etiquetas = etiquetas;
        this.alturas = ProgresoSemanal.alturas(kilos);
        requestLayout();
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Paint.FontMetrics fm = pincelTexto.getFontMetrics();
        int alto = Math.round(altoBarras + separacionEtiqueta + (fm.descent - fm.ascent));
        setMeasuredDimension(resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec), alto);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        int n = kilos.length;
        if (n == 0) return;
        float ancho = (getWidth() - getPaddingLeft() - getPaddingRight() - hueco * (n - 1)) / n;
        float x = getPaddingLeft();
        float baseTexto = altoBarras + separacionEtiqueta - pincelTexto.getFontMetrics().ascent;
        for (int i = 0; i < n; i++) {
            pincelBarra.setColor(i == n - 1 ? colorActual : colorPasadas);
            // Una semana sin entreno no dibuja barra, pero deja un filo de 2 dp para que se vea que existe.
            float alto = Math.max(alturas[i] * altoBarras, 2 * getResources().getDisplayMetrics().density);
            rect.set(x, altoBarras - alto, x + ancho, altoBarras);
            float[] radios = {radioArriba, radioArriba, radioArriba, radioArriba,
                    radioAbajo, radioAbajo, radioAbajo, radioAbajo};
            trazo.reset();
            trazo.addRoundRect(rect, radios, Path.Direction.CW);
            canvas.drawPath(trazo, pincelBarra);
            if (i < etiquetas.length) canvas.drawText(etiquetas[i], x + ancho / 2f, baseTexto, pincelTexto);
            x += ancho + hueco;
        }
    }
}
