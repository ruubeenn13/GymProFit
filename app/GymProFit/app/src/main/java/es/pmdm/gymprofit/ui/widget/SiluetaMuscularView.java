package es.pmdm.gymprofit.ui.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import es.pmdm.gymprofit.R;

// ============================================================
// SiluetaMuscularView — el cuerpo encendido de la pantalla de inicio.
//
// Dibuja dos siluetas a lo ancho, de frente y de espaldas, con los músculos
// trabajados en los últimos días teñidos del naranja de marca y el resto en gris.
// La intensidad del naranja sube con las series que ha recibido cada músculo, así
// que de un vistazo se ve no solo QUÉ se ha entrenado sino cuánto.
//
// Los 16 `ic_body_*` originales no servían para esto: cada uno es la silueta
// entera con UN músculo resaltado, y no hay forma de apilarlos. `scripts/
// extraer-musculos.py` los descompuso en dos cuerpos base más un vector por
// músculo, que es lo que esta vista apila y tiñe.
//
// Un usuario nuevo la ve entera en gris, y ese cuerpo vacío es la llamada a la
// acción: no hace falta explicar nada.
// ============================================================
public class SiluetaMuscularView extends View {

    // Proporción de los vectores de origen (viewport 724 x 1448).
    private static final float RELACION_ALTO_ANCHO = 2f;

    // Series a partir de las cuales un músculo se pinta a pleno color. Por encima no
    // sube más: la diferencia entre 12 y 30 series no cabe en un tinte.
    private static final int SERIES_PARA_MAXIMO = 12;

    // Suelo del tinte. Un músculo tocado una sola vez tiene que distinguirse del gris,
    // o la silueta miente por omisión.
    private static final float ALFA_MINIMO = 0.40f;

    // Músculos de la vista frontal y de la dorsal. La clave es la que manda la API.
    private static final String[] FRONTALES = {
            "cuello", "hombros", "pecho", "biceps", "antebrazos",
            "abdominales", "cuadriceps", "aductores"
    };
    private static final String[] DORSALES = {
            "trapecios", "dorsales", "triceps", "lumbares",
            "gluteos", "isquiotibiales", "gemelos"
    };

    private Drawable siluetaFrontal, siluetaDorsal;
    // Vectores de cada músculo, cacheados: se redibujan en cada scroll.
    private final Map<String, Drawable> musculos = new LinkedHashMap<>();

    // Series por músculo. Vacío = cuerpo entero en gris, que es el estado de partida.
    private Map<String, Integer> volumen = Collections.emptyMap();

    private int colorMarca;
    private int separacion;

    public SiluetaMuscularView(Context c) { this(c, null); }
    public SiluetaMuscularView(Context c, @Nullable AttributeSet a) { super(c, a); init(); }

    private void init() {
        colorMarca = attr(com.google.android.material.R.attr.colorPrimary);
        separacion = (int) (12 * getResources().getDisplayMetrics().density);

        siluetaFrontal = ContextCompat.getDrawable(getContext(), R.drawable.ic_silueta_frontal);
        siluetaDorsal  = ContextCompat.getDrawable(getContext(), R.drawable.ic_silueta_dorsal);

        cargarMusculos(FRONTALES);
        cargarMusculos(DORSALES);
    }

    // Resuelve por nombre los vectores ic_musculo_* y los guarda listos para teñir.
    private void cargarMusculos(String[] claves) {
        for (String clave : claves) {
            int res = getResources().getIdentifier(
                    "ic_musculo_" + clave, "drawable", getContext().getPackageName());
            if (res == 0) continue;                       // músculo sin vector: se ignora
            Drawable d = ContextCompat.getDrawable(getContext(), res);
            if (d != null) musculos.put(clave, DrawableCompat.wrap(d.mutate()));
        }
    }

    /**
     * Fija las series recibidas por cada músculo y repinta.
     *
     * @param volumen clave de músculo → series. Las claves desconocidas se ignoran,
     *                y los músculos ausentes se quedan en gris.
     */
    public void setVolumen(@Nullable Map<String, Integer> volumen) {
        this.volumen = volumen == null ? Collections.emptyMap() : volumen;
        invalidate();
    }

    /** Número de músculos distintos con al menos una serie. */
    public int getMusculosTrabajados() {
        int n = 0;
        for (Integer series : volumen.values()) {
            if (series != null && series > 0) n++;
        }
        return n;
    }

    // Dos cuerpos de proporción 1:2 uno al lado del otro: el alto sale del ancho.
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int ancho = MeasureSpec.getSize(widthMeasureSpec);
        int anchoCuerpo = (ancho - separacion) / 2;
        int alto = (int) (anchoCuerpo * RELACION_ALTO_ANCHO);
        setMeasuredDimension(ancho, alto);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        int anchoCuerpo = (getWidth() - separacion) / 2;
        int alto = getHeight();

        dibujarCuerpo(canvas, siluetaFrontal, FRONTALES, 0, anchoCuerpo, alto);
        dibujarCuerpo(canvas, siluetaDorsal, DORSALES, anchoCuerpo + separacion, anchoCuerpo, alto);
    }

    // Pinta una silueta y, encima, sus músculos teñidos según las series recibidas.
    private void dibujarCuerpo(Canvas canvas, Drawable silueta, String[] claves,
                               int izquierda, int ancho, int alto) {
        if (silueta != null) {
            silueta.setBounds(izquierda, 0, izquierda + ancho, alto);
            silueta.draw(canvas);
        }

        for (String clave : claves) {
            Integer series = volumen.get(clave);
            if (series == null || series <= 0) continue;   // sin trabajar: se queda gris

            Drawable musculo = musculos.get(clave);
            if (musculo == null) continue;

            DrawableCompat.setTintList(musculo, ColorStateList.valueOf(colorMarca));
            musculo.setAlpha(alfaPara(series));
            musculo.setBounds(izquierda, 0, izquierda + ancho, alto);
            musculo.draw(canvas);
        }
    }

    // De series a opacidad: crece deprisa al principio y se aplana al llegar al tope.
    private int alfaPara(int series) {
        float proporcion = Math.min(1f, series / (float) SERIES_PARA_MAXIMO);
        return Math.round(255 * (ALFA_MINIMO + (1f - ALFA_MINIMO) * proporcion));
    }

    private int attr(int attrRes) {
        TypedValue tv = new TypedValue();
        if (getContext().getTheme().resolveAttribute(attrRes, tv, true)) return tv.data;
        return Color.GRAY;
    }
}
