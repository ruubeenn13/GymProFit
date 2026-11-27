package es.pmdm.gymprofit.ui.widget;

import android.content.Context;
import android.os.Build;
import android.text.Layout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.widget.AppCompatTextView;

// ============================================================
// TextoSinPartir — un texto que nunca parte una palabra (GP-288; DEC-019).
//
// Los tiles de «Tu día» de Inicio son estrechos (un tercio del ancho): con letra grande una
// palabra sola no cabía y Android la partía donde se le acababa la línea («Calori / as»,
// «Protei / na»), o cortaba la cifra. Aquí, si la palabra más ancha no cabe en una línea, el
// texto encoge lo justo para que quepa, y nunca por debajo del tamaño que tiene a letra 1,0 (el
// mínimo de la app, como TextoQueEncoge). Con varias palabras sigue pasando a otra línea, pero
// solo por los espacios: sin guiones y sin cortar. Un espacio duro (U+00A0, el de «188 g») une.
//
// Se decide en onMeasure, con el ancho que le dan, y se vuelve a decidir con cada texto nuevo:
// un texto más corto recupera el tamaño pedido.
// ============================================================
public class TextoSinPartir extends AppCompatTextView {

    // El tamaño del estilo, a la letra de ahora, y el de letra 1,0, en px.
    private float pedidoPx, minimoPx;
    private final TextPaint medidor = new TextPaint();
    // Los textos que van al mismo tamaño que este (él incluido), o null.
    @Nullable private TextoSinPartir[] grupo;

    /**
     * Pone varios textos hermanos, del mismo ancho y estilo, a un solo tamaño: el del que más
     * tiene que encoger. Un texto nuevo en cualquiera vuelve a medir a todos.
     */
    public static void igualar(@NonNull TextoSinPartir... textos) {
        for (TextoSinPartir t : textos) t.grupo = textos;
    }

    public TextoSinPartir(@NonNull Context context) {
        this(context, null);
    }

    public TextoSinPartir(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, android.R.attr.textViewStyle);
    }

    public TextoSinPartir(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        // Sin guiones: también es partir una palabra (y Android 6 a 9 los pone por defecto).
        if (Build.VERSION.SDK_INT >= 23) setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE);
        recordarTamano();
    }

    @Override
    public void setTextAppearance(int resId) {
        super.setTextAppearance(resId);
        recordarTamano();
    }

    @Override
    public void setTextSize(int unit, float size) {
        super.setTextSize(unit, size);
        recordarTamano();
    }

    // El tamaño que pide el estilo a la letra de ahora y su suelo, el de letra 1,0.
    private void recordarTamano() {
        pedidoPx = getTextSize();
        float escala = getResources().getConfiguration().fontScale;
        minimoPx = escala > 1f ? pedidoPx / escala : pedidoPx;
    }

    @Override
    protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter);
        // Con el ancho fijo, TextView rehace su línea sin medirse otra vez: aquí sí hace falta.
        if (pedidoPx > 0) {
            if (grupo == null) requestLayout();
            else for (TextoSinPartir t : grupo) t.requestLayout();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (View.MeasureSpec.getMode(widthMeasureSpec) != View.MeasureSpec.UNSPECIFIED && pedidoPx > 0) {
            int ancho = View.MeasureSpec.getSize(widthMeasureSpec) - getCompoundPaddingLeft()
                    - getCompoundPaddingRight();
            medidor.set(getPaint());
            float tamano = tamanoQueCabe(getText(), ancho, pedidoPx, minimoPx, medidor);
            // En un grupo, todos al del que más tiene que encoger (los textos hermanos tienen
            // el mismo ancho): que «Agua» no salga más grande que «Calorías» a su lado.
            if (grupo != null) {
                for (TextoSinPartir otro : grupo) {
                    if (otro == this) continue;
                    tamano = Math.min(tamano, tamanoQueCabe(otro.getText(), ancho, pedidoPx, minimoPx, medidor));
                }
            }
            if (tamano != getTextSize()) super.setTextSize(TypedValue.COMPLEX_UNIT_PX, tamano);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    /**
     * El mayor tamaño, entre el mínimo y el pedido, al que la palabra más ancha del texto cabe en
     * el ancho dado. Si ni al mínimo cabe, el mínimo.
     *
     * @param ancho    ancho disponible para el texto, en px.
     * @param pedidoPx tamaño pedido, en px.
     * @param minimoPx tamaño por debajo del que no baja, en px.
     * @param medidor  pincel con la fuente del texto (se le cambia el tamaño).
     */
    @VisibleForTesting
    static float tamanoQueCabe(@Nullable CharSequence texto, int ancho, float pedidoPx, float minimoPx,
                               @NonNull TextPaint medidor) {
        if (texto == null || texto.length() == 0 || ancho <= 0 || minimoPx >= pedidoPx) return pedidoPx;
        String[] palabras = texto.toString().split("[\\s&&[^\\u00A0]]+");
        float tamano = pedidoPx;
        // De píxel en píxel: pocos pasos (de letra 2,0 a 1,0 son unos 30 en el peor tile).
        while (tamano > minimoPx) {
            medidor.setTextSize(tamano);
            float masAncha = 0f;
            for (String p : palabras) masAncha = Math.max(masAncha, medidor.measureText(p));
            if (masAncha <= ancho) return tamano;
            tamano = Math.max(minimoPx, tamano - 1f);
        }
        return minimoPx;
    }
}
