package es.pmdm.gymprofit.ui.widget;

import android.content.Context;
import android.os.Build;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
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
// Con un tope de líneas (android:maxLines), lo que no cabe se quita por palabras enteras y se
// acaba en «…»: la elipsis cae siempre tras una palabra completa, nunca a media («mancue…»).
// getText() da entonces lo que se ve; el texto entero sigue en getTextoCompleto() y es lo que lee
// TalkBack. Con encogerParaCaber(), antes de quitar nada encoge (con el mismo suelo) hasta que
// el texto entero cabe en sus líneas: el título de la Sesión en vivo, que a letra 2,0 en un
// móvil estrecho se quedaba en «Cuerpo…».
//
// Se decide en onMeasure, con el ancho que le dan, y se vuelve a decidir con cada texto nuevo:
// un texto más corto recupera el tamaño pedido. Se mide lo que se pinta: un rótulo en mayúsculas
// (los de las columnas de una serie, GP-286) cuenta con sus mayúsculas.
// ============================================================
public class TextoSinPartir extends AppCompatTextView {

    // El tamaño del estilo, a la letra de ahora, y el de letra 1,0, en px.
    private float pedidoPx, minimoPx;
    private final TextPaint medidor = new TextPaint();
    // Los textos que van al mismo tamaño que este (él incluido), o null.
    @Nullable private TextoSinPartir[] grupo;
    // El texto que se pidió, entero: getText() puede ir recortado con «…» si hay tope de líneas.
    // Sin valor inicial a propósito: el constructor de TextView ya pone el texto del layout.
    private CharSequence completo;
    // Se está poniendo el texto recortado: no es un texto nuevo.
    private boolean recortando;
    // La descripción para TalkBack la puso el recorte (el texto entero), no quien usa la vista.
    private boolean descripcionDelRecorte;
    // Un tope que le pone su fila (FilaRotulos), en px; 0, ninguno. Y el tamaño que eligió él
    // solo, sin ese tope, la última vez que se midió.
    private float tope, propio;
    // Con tope de líneas: encoge hasta caber entero en ellas antes de recortar.
    private boolean hastaCaber;

    /**
     * Pone varios textos hermanos, del mismo ancho y estilo, a un solo tamaño: el del que más
     * tiene que encoger. Un texto nuevo en cualquiera vuelve a medir a todos.
     */
    public static void igualar(@NonNull TextoSinPartir... textos) {
        for (TextoSinPartir t : textos) t.grupo = textos;
    }

    /**
     * Un tope para su tamaño, además del suyo: el de otro texto de su fila que ha tenido que
     * encoger más (FilaRotulos, con columnas de distinto ancho). Nunca baja del tamaño a letra
     * 1,0. Cero lo quita.
     *
     * @param px el tamaño máximo, en px.
     */
    public void limitarA(float px) {
        if (px == tope) return;
        tope = px;
        forceLayout();
    }

    /** El tamaño que eligió por su cuenta en su última medida (sin el tope de su fila), en px. */
    public float tamanoPropio() {
        return propio > 0f ? propio : getTextSize();
    }

    /**
     * Con un tope de líneas (android:maxLines), si el texto entero no cabe en ellas, encoge lo
     * justo para que quepa, nunca por debajo de su tamaño a letra 1,0; solo si ni así cabe se
     * recorta por palabras con «…».
     */
    public void encogerParaCaber(boolean si) {
        if (si == hastaCaber) return;
        hastaCaber = si;
        requestLayout();
    }

    /** El tamaño que pide su estilo a la letra de ahora, sin encoger, en px. */
    public float tamanoPedido() {
        return pedidoPx;
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
    public void setText(CharSequence text, BufferType type) {
        if (!recortando) completo = text == null ? "" : text;
        super.setText(text, type);
    }

    /** El texto que se pidió, entero, aunque en pantalla vaya recortado con «…». */
    @NonNull
    public CharSequence getTextoCompleto() {
        return completo == null ? "" : completo;
    }

    // El texto entero tal como se pinta: un rótulo en mayúsculas (textAllCaps) es más ancho que
    // lo que se le dio, y lo que tiene que caber es lo pintado (GP-286).
    @NonNull
    private CharSequence comoSePinta() {
        CharSequence texto = getTextoCompleto();
        android.text.method.TransformationMethod m = getTransformationMethod();
        CharSequence pintado = m == null ? texto : m.getTransformation(texto, this);
        return pintado == null ? texto : pintado;
    }

    @Override
    protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter);
        // Con el ancho fijo, TextView rehace su línea sin medirse otra vez: aquí sí hace falta.
        if (pedidoPx > 0 && !recortando) {
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
            float tamano = tamanoQueCabe(comoSePinta(), ancho, pedidoPx, minimoPx, medidor);
            // En un grupo, todos al del que más tiene que encoger (los textos hermanos tienen
            // el mismo ancho): que «Agua» no salga más grande que «Calorías» a su lado.
            if (grupo != null) {
                for (TextoSinPartir otro : grupo) {
                    if (otro == this) continue;
                    tamano = Math.min(tamano, tamanoQueCabe(otro.comoSePinta(), ancho, pedidoPx,
                            minimoPx, medidor));
                }
            }
            if (hastaCaber) tamano = tamanoQueCabeEnLineas(comoSePinta(), ancho, getMaxLines(), tamano, minimoPx);
            propio = tamano;
            if (tope > 0f) tamano = Math.max(minimoPx, Math.min(tamano, tope));
            if (tamano != getTextSize()) super.setTextSize(TypedValue.COMPLEX_UNIT_PX, tamano);
            recortar(ancho, tamano);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    // El mayor tamaño, entre el mínimo y el dado, al que el texto entero cabe en ese número de
    // líneas partiendo solo por los espacios; si ni al mínimo cabe, el mínimo (y se recortará).
    private float tamanoQueCabeEnLineas(@NonNull CharSequence texto, int ancho, int lineas, float desde,
                                        float minimo) {
        if (lineas <= 0 || lineas == Integer.MAX_VALUE || ancho <= 0 || texto.length() == 0) return desde;
        int estrategia = Build.VERSION.SDK_INT >= 23 ? getBreakStrategy() : 0;
        float tamano = desde;
        // De píxel en píxel, como tamanoQueCabe: pocos pasos y solo cuando no cabe.
        while (true) {
            medidor.setTextSize(tamano);
            if (tamano <= minimo || colocar(texto, ancho, medidor, estrategia).getLineCount() <= lineas) return tamano;
            tamano = Math.max(minimo, tamano - 1f);
        }
    }

    // Con tope de líneas, deja a la vista las palabras enteras que caben y «…»; sin tope, o si
    // cabe, el texto entero.
    private void recortar(int ancho, float tamano) {
        int tope = getMaxLines();
        if (tope <= 0 || tope == Integer.MAX_VALUE || ancho <= 0) return;
        medidor.set(getPaint());
        medidor.setTextSize(tamano);
        int estrategia = Build.VERSION.SDK_INT >= 23 ? getBreakStrategy() : 0;
        CharSequence visible = hastaPalabraCompleta(getTextoCompleto(), ancho, tope, medidor, estrategia);
        boolean recortado = !TextUtils.equals(visible, getTextoCompleto());
        if (!TextUtils.equals(visible, getText())) {
            recortando = true;
            setText(visible);
            recortando = false;
        }
        // TalkBack lee el nombre entero, no el recortado.
        if (recortado) {
            setContentDescription(getTextoCompleto());
            descripcionDelRecorte = true;
        } else if (descripcionDelRecorte) {
            setContentDescription(null);
            descripcionDelRecorte = false;
        }
    }

    /**
     * El texto, o, si no cabe en ese número de líneas, sus primeras palabras enteras seguidas de
     * «…»: tantas como quepan. Si ni la primera palabra cabe, el texto tal cual (lo cortará
     * TextView: no hay palabra completa que dejar).
     *
     * @param ancho      ancho disponible, en px.
     * @param lineas     tope de líneas.
     * @param medidor    pincel con la fuente y el tamaño del texto.
     * @param estrategia la de salto de línea del TextView (getBreakStrategy), para partir igual.
     */
    @VisibleForTesting
    static CharSequence hastaPalabraCompleta(@NonNull CharSequence texto, int ancho, int lineas,
                                             @NonNull TextPaint medidor, int estrategia) {
        if (texto.length() == 0) return texto;
        StaticLayout entero = colocar(texto, ancho, medidor, estrategia);
        if (entero.getLineCount() <= lineas) return texto;
        String s = texto.toString();
        // Donde acaba la última línea que se ve; de ahí hacia atrás, palabra a palabra, hasta
        // que lo que queda más «…» quepa.
        int fin = entero.getLineEnd(lineas - 1);
        while (fin > 0) {
            // Solo vale un corte entre palabras: justo antes de un espacio o justo después.
            if (fin < s.length() && !Character.isWhitespace(s.charAt(fin))
                    && !Character.isWhitespace(s.charAt(fin - 1))) {
                fin--;
                continue;
            }
            int hasta = fin;
            // Fuera los espacios y la puntuación que quedarían colgando antes de «…».
            while (hasta > 0 && (Character.isWhitespace(s.charAt(hasta - 1))
                    || ",;:-–(".indexOf(s.charAt(hasta - 1)) >= 0)) hasta--;
            if (hasta == 0) break;
            String candidato = s.substring(0, hasta) + "…";
            if (colocar(candidato, ancho, medidor, estrategia).getLineCount() <= lineas) return candidato;
            fin = hasta - 1;
        }
        return texto;
    }

    // Las líneas del texto como las pondrá el TextView: mismo pincel, mismo ancho, sin guiones.
    private static StaticLayout colocar(CharSequence texto, int ancho, TextPaint medidor, int estrategia) {
        if (Build.VERSION.SDK_INT >= 23) {
            return StaticLayout.Builder.obtain(texto, 0, texto.length(), medidor, ancho)
                    .setBreakStrategy(estrategia)
                    .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE)
                    .build();
        }
        return new StaticLayout(texto, medidor, ancho, Layout.Alignment.ALIGN_NORMAL, 1f, 0f, true);
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
