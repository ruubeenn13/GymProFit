package es.pmdm.gymprofit.ui.inicio;

import android.content.Context;
import android.text.StaticLayout;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import es.pmdm.gymprofit.R;

// ============================================================
// CabeceraHoyToca — el título de «Hoy toca» con las siluetas al lado (GP-285, DEC-070).
//
// El subtítulo («7 ejercicios · unos 55 min · Espalda, Hombros y Brazos») va bajo el título,
// junto a las siluetas, mientras quepa ahí en dos líneas. En un móvil estrecho o con la letra
// grande necesitaba tres, en una columna que competía con las siluetas: entonces baja, a todo
// el ancho de la tarjeta, donde vuelve a caber en dos.
//
// Son dos textos, uno en cada sitio, y solo se ve uno: tvHoySub (el que el resto del código
// conoce y al que se le pone el texto) junto a las siluetas, y tvHoySubAncho debajo, que copia
// lo que diga el otro. Se decide en cada medida, como CamposSerie: girar el móvil, cambiar la
// letra o que lleguen las siluetas lo vuelve a decidir, sin parpadeo. Las líneas se cuentan
// aparte (un StaticLayout al ancho de la columna), sin enseñar y esconder las vistas en cada
// medida: eso pediría otra medida, y esa otra, sin fin.
// ============================================================
public class CabeceraHoyToca extends LinearLayout {

    /** Las líneas que el subtítulo puede ocupar junto a las siluetas. */
    static final int LINEAS_JUNTO = 2;

    public CabeceraHoyToca(@NonNull Context context) {
        super(context);
    }

    public CabeceraHoyToca(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Si el subtítulo va debajo de la fila, a todo el ancho.
     *
     * @param lineasJunto  las líneas que ocupa junto a las siluetas.
     * @param haySiluetas  si las siluetas están (sin ellas ya tiene todo el ancho).
     */
    static boolean vaDebajo(int lineasJunto, boolean haySiluetas) {
        return haySiluetas && lineasJunto > LINEAS_JUNTO;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        TextView junto = findViewById(R.id.tvHoySub);
        TextView ancho = findViewById(R.id.tvHoySubAncho);
        View siluetas = findViewById(R.id.siluetaHoy);
        if (junto == null || ancho == null || siluetas == null) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        // El ancho de la columna del título no depende de cuál de los dos textos se vea: con él
        // se cuenta cuántas líneas ocuparía el subtítulo ahí, sin tocar las vistas.
        int columna = ((View) junto.getParent()).getMeasuredWidth();
        boolean debajo = vaDebajo(lineas(junto, columna), siluetas.getVisibility() == VISIBLE);
        boolean cambia = debajo != (ancho.getVisibility() == VISIBLE);
        boolean otroTexto = debajo && !TextUtils.equals(ancho.getText(), junto.getText());
        // Solo se toca algo si cambia: tocarlo en cada medida pediría otra, y otra.
        if (!cambia && !otroTexto) return;
        if (otroTexto) ancho.setText(junto.getText());
        junto.setVisibility(debajo ? GONE : VISIBLE);
        ancho.setVisibility(debajo ? VISIBLE : GONE);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    // Las líneas que ese texto ocuparía a ese ancho, partido como lo parte su TextView.
    private CharSequence textoContado;
    private int anchoContado = -1, lineasContadas;

    private int lineas(TextView texto, int ancho) {
        CharSequence t = texto.getText();
        if (ancho <= 0 || TextUtils.isEmpty(t)) return 0;
        if (ancho == anchoContado && TextUtils.equals(t, textoContado)) return lineasContadas;
        lineasContadas = StaticLayout.Builder.obtain(t, 0, t.length(), texto.getPaint(), ancho)
                .setBreakStrategy(texto.getBreakStrategy())
                .setHyphenationFrequency(texto.getHyphenationFrequency())
                .setIncludePad(texto.getIncludeFontPadding())
                .build().getLineCount();
        textoContado = t.toString();
        anchoContado = ancho;
        return lineasContadas;
    }
}
