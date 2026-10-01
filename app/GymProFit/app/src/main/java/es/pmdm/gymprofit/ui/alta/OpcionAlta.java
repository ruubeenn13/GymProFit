package es.pmdm.gymprofit.ui.alta;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.animation.AnimatorInflater;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.AttrRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.utils.Movimiento;

// ============================================================
// OpcionAlta — una opción de una pregunta del alta (GP-103), con su movimiento (GP-104).
//
// Tarjeta de radio 18 dp (16 en una hoja) con icono opcional, título, descripción y la
// marca redonda. Sin elegir: fondo de tarjeta y borde del tema, que en una opción que se
// pulsa llega a 3:1 (GP-063; el lienzo la pinta sin borde, y manda el token). Elegida:
// fondo naranja apagado, borde naranja de 2 dp, marca rellena con su check e icono
// relleno sobre naranja.
//
// Momento 1 de DEC-039: se hunde un 3 % al tocar, fondo y borde cambian en 150 ms, el
// check salta en 260 ms y el icono gira y rebota en 420; vibración ligera.
//
// Accesibilidad (DEC-019): TalkBack la lee como un botón de radio, con el título y la
// descripción en una sola frase y su estado marcado o no.
// ============================================================
public class OpcionAlta extends LinearLayout {

    private final View marcoIcono;
    private final ImageView icono;
    private final TextView titulo;
    private final TextView descripcion;
    private final View marca;
    private final ImageView check;

    private final GradientDrawable fondo = new GradientDrawable();
    private final GradientDrawable fondoIcono = new GradientDrawable();
    private final GradientDrawable fondoMarca = new GradientDrawable();

    @DrawableRes private int iconoContorno;
    @DrawableRes private int iconoRelleno;
    private boolean elegida;
    private boolean enHoja;
    @Nullable private ValueAnimator transicion;

    public OpcionAlta(@NonNull Context context) {
        this(context, null);
    }

    public OpcionAlta(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setClickable(true);
        setFocusable(true);
        LayoutInflater.from(context).inflate(R.layout.view_opcion_alta, this, true);
        marcoIcono = findViewById(R.id.marcoIconoOpcion);
        icono = findViewById(R.id.ivIconoOpcion);
        titulo = findViewById(R.id.tvTituloOpcion);
        descripcion = findViewById(R.id.tvDescripcionOpcion);
        marca = findViewById(R.id.marcaOpcion);
        check = findViewById(R.id.ivCheckOpcion);

        fondoIcono.setShape(GradientDrawable.OVAL);
        marcoIcono.setBackground(fondoIcono);
        fondoMarca.setShape(GradientDrawable.OVAL);
        marca.setBackground(fondoMarca);
        setBackground(fondo);
        setStateListAnimator(AnimatorInflater.loadStateListAnimator(context, R.animator.toque));
        medidas();
        pintar(false);

        ViewCompat.setAccessibilityDelegate(this, new AccessibilityDelegateCompat() {
            @Override
            public void onInitializeAccessibilityNodeInfo(@NonNull View host,
                                                          @NonNull AccessibilityNodeInfoCompat info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setClassName(RadioButton.class.getName());
                info.setCheckable(true);
                info.setChecked(elegida);
            }
        });
    }

    /**
     * Los textos y los iconos de la opción.
     *
     * @param tituloRes      el título.
     * @param descripcionRes la descripción, o 0 si no lleva.
     * @param contorno       el icono sin elegir, o 0 si la opción no lleva icono.
     * @param relleno        el icono elegido (relleno, GP-080).
     */
    public void setDatos(int tituloRes, int descripcionRes, @DrawableRes int contorno, @DrawableRes int relleno) {
        titulo.setText(tituloRes);
        if (descripcionRes != 0) {
            descripcion.setText(descripcionRes);
            descripcion.setVisibility(VISIBLE);
        } else {
            descripcion.setVisibility(GONE);
        }
        iconoContorno = contorno;
        iconoRelleno = relleno;
        marcoIcono.setVisibility(contorno != 0 ? VISIBLE : GONE);
        setContentDescription(descripcionRes != 0
                ? getContext().getString(R.string.alta_opcion_a11y, titulo.getText(), descripcion.getText())
                : titulo.getText());
        pintar(false);
    }

    /** La variante de las hojas: un poco más baja y sobre el fondo de la hoja. */
    public void setEnHoja(boolean enHoja) {
        this.enHoja = enHoja;
        medidas();
        pintar(false);
    }

    public boolean isElegida() {
        return elegida;
    }

    /**
     * Marca o desmarca la opción.
     *
     * @param animar con el momento 1 (al tocarla) o sin él (al restaurar el borrador).
     */
    public void setElegida(boolean elegida, boolean animar) {
        if (this.elegida == elegida && !animar) {
            pintar(false);
            return;
        }
        boolean cambia = this.elegida != elegida;
        this.elegida = elegida;
        pintar(animar && cambia);
        if (animar && cambia && elegida) {
            Movimiento.vibrar(this, Movimiento.Vibracion.LIGERA);
            Movimiento.saltar(check, Movimiento.CHECK);
            if (iconoContorno != 0) Movimiento.rebotarIcono(icono);
        }
        sendAccessibilityEvent(android.view.accessibility.AccessibilityEvent.TYPE_VIEW_SELECTED);
    }

    // Alturas, márgenes y radios: 76 dp y radio 18 en pantalla; 68 y 16 en la hoja.
    private void medidas() {
        float d = getResources().getDisplayMetrics().density;
        setMinimumHeight(Math.round((enHoja ? 68 : 76) * d));
        int lado = Math.round(14 * d);
        int arriba = Math.round((enHoja ? 10 : 12) * d);
        setPadding(lado, arriba, lado, arriba);
        fondo.setCornerRadius((enHoja ? 16 : 18) * d);
        titulo.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(
                enHoja ? R.dimen.text_fila : R.dimen.text_subtitle));
        int marcaLado = Math.round((enHoja ? 26 : 28) * d);
        marca.getLayoutParams().width = marcaLado;
        marca.getLayoutParams().height = marcaLado;
    }

    // Colores de un estado, con transición de 150 ms si se pide.
    private void pintar(boolean animar) {
        float d = getResources().getDisplayMetrics().density;
        int fondoA = color(enHoja ? android.R.attr.colorBackground : com.google.android.material.R.attr.colorSurface);
        int fondoB = color(com.google.android.material.R.attr.colorPrimaryContainer);
        int bordeA = color(com.google.android.material.R.attr.colorOutline);
        int bordeB = color(androidx.appcompat.R.attr.colorPrimary);
        int icoFondoA = color(com.google.android.material.R.attr.colorSurfaceVariant);
        int icoFondoB = bordeB;
        int icoA = color(com.google.android.material.R.attr.colorOnPrimaryContainer);
        int icoB = color(com.google.android.material.R.attr.colorOnPrimary);
        int grosor = Math.round(2 * d);

        icono.setImageResource(elegida && iconoRelleno != 0 ? iconoRelleno : iconoContorno);
        check.setVisibility(elegida ? VISIBLE : INVISIBLE);
        if (!elegida) {
            check.setScaleX(1f);
            check.setScaleY(1f);
            check.setAlpha(1f);
        }

        if (transicion != null) transicion.cancel();
        float hasta = elegida ? 1f : 0f;
        ArgbEvaluator mezcla = new ArgbEvaluator();
        Movimiento.Paso aplicar = t -> {
            fondo.setColor((int) mezcla.evaluate(t, fondoA, fondoB));
            fondo.setStroke(grosor, (int) mezcla.evaluate(t, bordeA, bordeB));
            fondoIcono.setColor((int) mezcla.evaluate(t, icoFondoA, icoFondoB));
            icono.setImageTintList(ColorStateList.valueOf((int) mezcla.evaluate(t, icoA, icoB)));
            fondoMarca.setColor((int) mezcla.evaluate(t, bordeB & 0x00FFFFFF, bordeB));
            fondoMarca.setStroke(grosor, (int) mezcla.evaluate(t, bordeA, bordeB));
        };
        if (!animar) {
            aplicar.en(hasta);
            return;
        }
        transicion = ValueAnimator.ofFloat(1f - hasta, hasta);
        transicion.setDuration(Movimiento.RELLENO);
        transicion.addUpdateListener(an -> aplicar.en((float) an.getAnimatedValue()));
        transicion.start();
    }

    private int color(@AttrRes int attr) {
        TypedValue tv = new TypedValue();
        getContext().getTheme().resolveAttribute(attr, tv, true);
        return tv.data;
    }
}
