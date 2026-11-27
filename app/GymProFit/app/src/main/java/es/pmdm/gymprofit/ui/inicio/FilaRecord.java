package es.pmdm.gymprofit.ui.inicio;

import android.content.Context;
import android.text.Layout;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import es.pmdm.gymprofit.R;

// ============================================================
// FilaRecord — la fila del último récord de Inicio (GP-285, lote 1b; view_record_inicio).
//
// El nombre del ejercicio, el detalle («Último récord · hace 6 días · antes 95 × 12») y la
// marca («100 kg × 12»). Ninguno se parte en dos líneas: con letra grande el detalle se
// quedaba en tres y la marca apretaba al nombre.
//
//   · Normal: la marca a la derecha del nombre, y el detalle debajo, en una línea, desde el
//     nombre hasta el borde (pasa por debajo de la marca).
//   · La marca baja debajo del texto si el detalle no cabe en esa línea o si la marca dejaría
//     al nombre con menos de media línea: letra 1,3, o un móvil estrecho. Entonces el detalle
//     empieza en el borde de la tarjeta, bajo el trofeo, que es todo el ancho que hay.
//   · Si ni así cabe (letra 1,3 en un móvil de menos de 400 dp), el detalle pierde su rótulo
//     y se queda en lo que solo él dice: «Hace 6 días · antes 95 × 12». El trofeo y la sección
//     ya dicen que es un récord, y TalkBack sigue leyendo la frase entera (la tarjeta lleva su
//     descripción).
//
// El nombre acaba en puntos suspensivos si no cabe; el detalle, solo si no cabe ni el corto.
//
// La decisión se toma al medir, con lo que mide cada texto en una línea, y solo se tocan los
// hijos cuando cambia (una vista que toca a sus hijos en cada medida pide otra medida, sin
// fin). Las cuentas, sin nada de Android, en marcaAbajo() y detalleCorto(): se prueban solas
// (FilaRecordTest).
// ============================================================
public class FilaRecord extends LinearLayout {

    private TextView nombre, detalle, marca, marcaAbajo;
    private View icono;
    private CharSequence detalleEntero = "", detalleSinRotulo = "";
    // null hasta la primera medida: la primera decisión siempre se aplica.
    @Nullable private Boolean abajo, corto;

    public FilaRecord(@NonNull Context context) {
        this(context, null);
    }

    public FilaRecord(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Si la marca tiene que bajar debajo del texto.
     *
     * @param detalle lo que mide el detalle en una línea, en px.
     * @param marca   lo que mide la marca con su hueco, en px.
     * @param columna el ancho que hay a la derecha del trofeo, en px.
     */
    public static boolean marcaAbajo(float detalle, float marca, float columna) {
        return detalle > columna || marca > columna / 2f;
    }

    /**
     * Si el detalle tiene que ir sin su rótulo: cuando no cabe en una línea ni con la marca
     * abajo, a todo el ancho de la fila.
     *
     * @param detalle lo que mide el detalle entero en una línea, en px.
     * @param ancho   el ancho de la fila por dentro, trofeo incluido, en px.
     */
    public static boolean detalleCorto(float detalle, float ancho) {
        return detalle > ancho;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        nombre = findViewById(R.id.tvRecordEjercicio);
        detalle = findViewById(R.id.tvRecordAntes);
        marca = findViewById(R.id.tvRecordMarca);
        marcaAbajo = findViewById(R.id.tvRecordMarcaAbajo);
        icono = findViewById(R.id.flRecordIcono);
    }

    /** La marca nueva («100 kg × 12»): va a la derecha del nombre o debajo del texto. */
    public void setMarca(@Nullable CharSequence texto) {
        marca.setText(texto);
        marcaAbajo.setText(texto);
    }

    /**
     * El detalle, en sus dos formas.
     *
     * @param entero     «Último récord · hace 6 días · antes 95 × 12».
     * @param sinRotulo  lo mismo sin el rótulo, para cuando el entero no cabe en una línea:
     *                   «Hace 6 días · antes 95 × 12». Si no hay forma más corta, el mismo.
     */
    public void setDetalle(@NonNull CharSequence entero, @NonNull CharSequence sinRotulo) {
        detalleEntero = entero;
        detalleSinRotulo = sinRotulo;
        corto = null;
        detalle.setText(entero);
    }

    /** Si la marca está debajo del texto (para pruebas). */
    public boolean marcaEstaAbajo() {
        return Boolean.TRUE.equals(abajo);
    }

    /** Si el detalle va sin su rótulo (para pruebas). */
    public boolean detalleEsCorto() {
        return Boolean.TRUE.equals(corto);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int ancho = MeasureSpec.getSize(widthMeasureSpec);
        if (MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.UNSPECIFIED && ancho > 0 && detalle != null) {
            MarginLayoutParams sangriaNombre = (MarginLayoutParams) nombre.getLayoutParams();
            int sangria = icono.getLayoutParams().width + sangriaNombre.getMarginStart();
            float dentro = ancho - getPaddingLeft() - getPaddingRight();
            float anchoMarca = Layout.getDesiredWidth(marca.getText(), marca.getPaint())
                    + ((MarginLayoutParams) marca.getLayoutParams()).getMarginStart();
            float anchoDetalle = Layout.getDesiredWidth(detalleEntero, detalle.getPaint());
            boolean baja = marcaAbajo(anchoDetalle, anchoMarca, dentro - sangria);
            boolean sinRotulo = baja && detalleCorto(anchoDetalle, dentro);
            if (abajo == null || baja != abajo) {
                abajo = baja;
                marca.setVisibility(baja ? GONE : VISIBLE);
                marcaAbajo.setVisibility(baja ? VISIBLE : GONE);
                MarginLayoutParams lp = (MarginLayoutParams) detalle.getLayoutParams();
                lp.setMarginStart(baja ? 0 : sangria);
                detalle.setLayoutParams(lp);
            }
            if (corto == null || sinRotulo != corto) {
                corto = sinRotulo;
                detalle.setText(sinRotulo ? detalleSinRotulo : detalleEntero);
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
