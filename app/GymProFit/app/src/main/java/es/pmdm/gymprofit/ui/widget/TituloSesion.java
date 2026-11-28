package es.pmdm.gymprofit.ui.widget;

import android.content.Context;
import android.text.Layout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;

import java.text.Normalizer;
import java.util.Locale;

import es.pmdm.gymprofit.R;

// ============================================================
// TituloSesion — el título de la Sesión en vivo: la rutina y, debajo, su programa (GP-285,
// remate del lote 2; la regla de GP-288).
//
// Va suelto entre minimizar y «Terminar», que no se pueden cortar: con letra grande le queda
// poco ancho. A letra 2,0, «Cuerpo completo C» y «Cuerpo completo» ocupaban cuatro líneas
// («Cuerpo / completo C / Cuerpo / completo») y empujaban todo lo de debajo.
//
//   · El título (tvTituloSesion), como mucho en dos líneas y sin partir palabras: es un
//     TextoSinPartir con su tope de líneas, que encoge lo justo (nunca por debajo de su tamaño
//     a letra 1,0) para caber entero en ellas antes de recortar nada.
//   · El programa (tvSubtituloSesion), desde letra 1,5, solo se ve si dice algo que el título
//     no dice ya y cabe entero en una línea. Si repite el título («Cuerpo completo» bajo «Cuerpo
//     completo C») o no cabe, se oculta. Por debajo de 1,5 se ve siempre, como hasta ahora.
//   · TalkBack lo lee siempre: el grupo es una sola parada, un encabezado, con la rutina y el
//     programa, se vea este o no.
//
// La decisión se toma al medir, con el ancho que le dan, y solo se toca el hijo cuando cambia
// (una vista que toca a sus hijos en cada medida pide otra medida, sin fin). Las cuentas, sin
// nada de Android, en repite() y programaVisible(): se prueban solas (TituloSesionTest).
// ============================================================
public class TituloSesion extends LinearLayout {

    /** La letra desde la que el programa puede ocultarse. */
    public static final float LETRA_GRANDE = 1.5f;

    private TextoSinPartir titulo, programa;
    private boolean hayPrograma, repetido;
    // null hasta la primera medida: la primera decisión siempre se aplica.
    @Nullable private Boolean visto;
    private final TextPaint medidor = new TextPaint();

    public TituloSesion(@NonNull Context context) {
        this(context, null);
    }

    public TituloSesion(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setOrientation(VERTICAL);
        // Una sola parada de TalkBack, y es el encabezado de la pantalla.
        ViewCompat.setScreenReaderFocusable(this, true);
        ViewCompat.setAccessibilityHeading(this, true);
    }

    /**
     * Si el programa repite el título: con las mismas palabras, seguidas, sin contar mayúsculas,
     * tildes ni espacios de más («Cuerpo completo» en «Cuerpo completo C»).
     */
    public static boolean repite(@Nullable CharSequence titulo, @Nullable CharSequence programa) {
        String t = llano(titulo), p = llano(programa);
        if (p.isEmpty() || t.isEmpty()) return false;
        return (" " + t + " ").contains(" " + p + " ");
    }

    // En minúsculas, sin tildes y con un solo espacio entre palabras.
    private static String llano(@Nullable CharSequence texto) {
        if (texto == null) return "";
        String s = Normalizer.normalize(texto.toString(), Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return s.toLowerCase(Locale.ROOT).replaceAll("[\\s\\u00A0]+", " ").trim();
    }

    /**
     * Si el programa se ve bajo el título.
     *
     * @param escala  la escala de letra de la app.
     * @param repite  si repite el título ({@link #repite}).
     * @param cabe    si cabe entero en una línea, a su tamaño.
     */
    public static boolean programaVisible(float escala, boolean repite, boolean cabe) {
        if (escala < LETRA_GRANDE) return true;
        return !repite && cabe;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        titulo = findViewById(R.id.tvTituloSesion);
        titulo.encogerParaCaber(true);
        programa = findViewById(R.id.tvSubtituloSesion);
    }

    /**
     * La rutina y su programa.
     *
     * @param rutina    el nombre de la rutina (o «Entrenamiento libre»).
     * @param suPrograma el nombre del programa; null o vacío si la rutina no es de ninguno.
     */
    public void poner(@NonNull CharSequence rutina, @Nullable CharSequence suPrograma) {
        hayPrograma = suPrograma != null && suPrograma.length() > 0;
        repetido = repite(rutina, suPrograma);
        titulo.setText(rutina);
        programa.setText(hayPrograma ? suPrograma : "");
        setContentDescription(hayPrograma
                ? getContext().getString(R.string.envivo_titulo_a11y, rutina, suPrograma) : rutina);
        visto = null;
        requestLayout();
    }

    /** Si el programa está a la vista (para pruebas). */
    public boolean programaALaVista() {
        return Boolean.TRUE.equals(visto);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (programa != null) {
            boolean ver = hayPrograma;
            if (ver && MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.UNSPECIFIED) {
                int ancho = MeasureSpec.getSize(widthMeasureSpec) - getPaddingLeft() - getPaddingRight()
                        - programa.getCompoundPaddingLeft() - programa.getCompoundPaddingRight();
                // A su tamaño, sin encoger: si para caber tiene que encoger, no cabe.
                medidor.set(programa.getPaint());
                medidor.setTextSize(programa.tamanoPedido());
                boolean cabe = Layout.getDesiredWidth(programa.getTextoCompleto(), medidor) <= ancho;
                ver = programaVisible(getResources().getConfiguration().fontScale, repetido, cabe);
            }
            if (visto == null || ver != visto) {
                visto = ver;
                programa.setVisibility(ver ? VISIBLE : GONE);
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
