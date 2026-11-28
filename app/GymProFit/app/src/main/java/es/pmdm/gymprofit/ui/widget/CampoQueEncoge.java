package es.pmdm.gymprofit.ui.widget;

import android.content.Context;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatEditText;

// ============================================================
// CampoQueEncoge — un campo corto de una línea (los kilos, las repeticiones o el tiempo de una
// serie) cuyo contenido nunca se corta ni se parte (GP-288; revisor de UI, lote 2 de GP-285).
//
// Los campos de una serie son estrechos (una parte del ancho de la fila). Con letra 2,0 la
// pauta «12–15» salía cortada en «12–1», sin puntos suspensivos —se leía otro rango—, y un
// valor de cinco caracteres («102.5») se partía en dos líneas y hacía crecer la fila. Aquí, si
// lo escrito o la pista no caben en su línea, la letra encoge lo justo para que quepan, y nunca
// por debajo del tamaño que tiene a letra 1,0: la misma regla, y la misma cuenta, que
// TextoSinPartir.
//
// Se decide en onMeasure, con el ancho que le dan, y se vuelve a decidir con cada cambio de lo
// escrito: al borrar, la letra recupera su tamaño.
// ============================================================
public class CampoQueEncoge extends AppCompatEditText {

    // El tamaño del estilo, a la letra de ahora, y el de letra 1,0, en px.
    private float pedidoPx, minimoPx;
    private final TextPaint medidor = new TextPaint();

    public CampoQueEncoge(@NonNull Context context) {
        this(context, null);
    }

    public CampoQueEncoge(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, androidx.appcompat.R.attr.editTextStyle);
    }

    public CampoQueEncoge(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setMaxLines(1);
        setHorizontallyScrolling(false);
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
        // Con el ancho fijo, el campo rehace su línea sin medirse otra vez: aquí sí hace falta.
        if (pedidoPx > 0) requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (View.MeasureSpec.getMode(widthMeasureSpec) != View.MeasureSpec.UNSPECIFIED && pedidoPx > 0) {
            int ancho = View.MeasureSpec.getSize(widthMeasureSpec) - getCompoundPaddingLeft()
                    - getCompoundPaddingRight();
            medidor.set(getPaint());
            // Lo que se ve es lo escrito o, vacío, la pista; caben los dos y así no baila al
            // escribir el primer carácter.
            float tamano = Math.min(
                    TextoSinPartir.tamanoQueCabe(unaPieza(getText()), ancho, pedidoPx, minimoPx, medidor),
                    TextoSinPartir.tamanoQueCabe(unaPieza(getHint()), ancho, pedidoPx, minimoPx, medidor));
            if (tamano != getTextSize()) super.setTextSize(TypedValue.COMPLEX_UNIT_PX, tamano);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    // Todo en una línea: los espacios, si los hubiera, no parten (TextoSinPartir mide por palabras).
    @Nullable
    private static CharSequence unaPieza(@Nullable CharSequence texto) {
        return texto == null ? null : texto.toString().replace(' ', (char) 0xA0);
    }
}
