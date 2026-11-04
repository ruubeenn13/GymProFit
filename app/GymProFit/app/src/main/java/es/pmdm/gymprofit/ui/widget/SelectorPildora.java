package es.pmdm.gymprofit.ui.widget;

import android.animation.AnimatorInflater;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.utils.Movimiento;

// ============================================================
// SelectorPildora — el único selector de opciones de la app (GP-224; nació en el alta,
// GP-103, momento 6).
//
// Una pista de 48 dp (superficie 2, radio completo, 4 dp de relleno, sin borde) con las
// opciones del mismo ancho y una píldora naranja de 40 dp que viaja a la elegida con rebote.
// Sin elegir nada no hay píldora. El texto es Barlow Condensed 600 a 16 sp: el de la elegida,
// sobre naranja; el de las demás, en el gris secundario. Con letra grande la opción parte en
// dos líneas y la pista crece: nunca corta.
//
// Cada opción es una vista de texto: TalkBack la lee como botón de opción, con su estado y su
// posición, dentro de un grupo con nombre («Unidad»); con setPestanas, como pestañas
// («pestaña, 1 de 3»). Pulsada, se hunde un 3 % (toque) y lleva una capa del 10 %; con foco de
// teclado, un anillo de 2 dp; desactivada, al 38 %. La elegida sobrevive al giro (si la vista
// tiene id). Con «Quitar animaciones», la píldora salta sin recorrido.
// ============================================================
public class SelectorPildora extends FrameLayout {

    /** Se avisa al elegir una opción con el dedo (o con TalkBack). */
    public interface AlElegir { void elegida(int indice); }

    private static final String CLAVE_SUPER = "super";
    private static final String CLAVE_ELEGIDA = "elegida";

    private final View pildora;
    private final LinearLayout fila;
    // Los colores, leídos una vez del tema de la pantalla (no de los recursos): si la
    // configuración cambia con la pantalla abierta (al encender un servicio de accesibilidad,
    // con la app en un tema distinto del sistema), los recursos devolvían los del otro tema.
    private final int colorTexto, colorTextoElegido, colorSobre;
    private TextView[] opciones = new TextView[0];
    private int elegida = -1;
    // La píldora está viajando: un repintado de la vista no la devuelve a su sitio de golpe.
    private boolean viajando;
    private boolean pestanas;
    @Nullable private CharSequence nombreGrupo;
    @Nullable private AlElegir alElegir;

    public SelectorPildora(@NonNull Context context) {
        this(context, null);
    }

    public SelectorPildora(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        Resources r = getResources();
        colorTexto = color(context, com.google.android.material.R.attr.colorOnSurfaceVariant, R.color.gp_text_secondary);
        colorTextoElegido = color(context, com.google.android.material.R.attr.colorOnPrimary, R.color.gp_on_primary);
        colorSobre = color(context, com.google.android.material.R.attr.colorOnSurface, R.color.gp_on_surface);
        int relleno = r.getDimensionPixelSize(R.dimen.selector_relleno);
        setPadding(relleno, 0, relleno, 0);
        setMinimumHeight(r.getDimensionPixelSize(R.dimen.zona_tactil));
        GradientDrawable pista = new GradientDrawable();
        pista.setColor(color(context, com.google.android.material.R.attr.colorSurfaceVariant, R.color.gp_surface_2));
        setBackground(pista);

        GradientDrawable fondo = new GradientDrawable();
        fondo.setColor(color(context, androidx.appcompat.R.attr.colorPrimary, R.color.gp_primary));
        pildora = new View(context);
        pildora.setBackground(fondo);
        pildora.setVisibility(INVISIBLE);
        pildora.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        addView(pildora, new LayoutParams(0, 0));

        fila = new LinearLayout(context);
        fila.setOrientation(LinearLayout.HORIZONTAL);
        fila.setBaselineAligned(false);
        addView(fila, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        ViewCompat.setAccessibilityDelegate(this, new AccessibilityDelegateCompat() {
            @Override
            public void onInitializeAccessibilityNodeInfo(@NonNull View host, @NonNull AccessibilityNodeInfoCompat info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setClassName(pestanas ? "android.widget.TabWidget" : RadioGroup.class.getName());
                if (nombreGrupo != null) info.setContentDescription(nombreGrupo);
                info.setCollectionInfo(AccessibilityNodeInfoCompat.CollectionInfoCompat.obtain(1, opciones.length,
                        false, AccessibilityNodeInfoCompat.CollectionInfoCompat.SELECTION_MODE_SINGLE));
            }
        });

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SelectorPildora);
            try {
                pestanas = a.getBoolean(R.styleable.SelectorPildora_selectorPestanas, false);
                nombreGrupo = a.getString(R.styleable.SelectorPildora_selectorNombreGrupo);
                int idTextos = a.getResourceId(R.styleable.SelectorPildora_selectorOpciones, 0);
                int idNombres = a.getResourceId(R.styleable.SelectorPildora_selectorNombres, 0);
                if (idTextos != 0) {
                    CharSequence[] textos = r.getTextArray(idTextos);
                    setOpciones(textos, idNombres != 0 ? r.getTextArray(idNombres) : null);
                }
                int inicial = a.getInt(R.styleable.SelectorPildora_selectorElegida, -1);
                if (inicial >= 0) setElegida(inicial, false);
            } finally {
                a.recycle();
            }
        }
    }

    /**
     * Las opciones.
     *
     * @param textos  lo que se ve («2», «45 min»).
     * @param nombres lo que lee TalkBack («2 días»), o null si es lo mismo que se ve.
     */
    public void setOpciones(@NonNull CharSequence[] textos, @Nullable CharSequence[] nombres) {
        Resources r = getResources();
        fila.removeAllViews();
        opciones = new TextView[textos.length];
        if (elegida >= textos.length) elegida = -1;
        // Cada opción mide la pista entera (48 dp) y responde al toque en toda ella; la píldora,
        // la capa y el anillo van 4 dp hacia dentro, en los 40 dp de la opción visible.
        int relleno = r.getDimensionPixelSize(R.dimen.selector_relleno);
        int alto = r.getDimensionPixelSize(R.dimen.selector_opcion_alto) + 2 * relleno;
        int lado = r.getDimensionPixelSize(R.dimen.espacio_4);
        int arriba = relleno + r.getDimensionPixelSize(R.dimen.espacio_4);
        for (int i = 0; i < textos.length; i++) {
            TextView o = new TextView(getContext());
            o.setText(textos[i]);
            if (nombres != null && i < nombres.length && nombres[i] != null) o.setContentDescription(nombres[i]);
            o.setGravity(Gravity.CENTER);
            o.setMinHeight(alto);
            o.setPadding(lado, arriba, lado, arriba);
            o.setMaxLines(2);
            // A letra grande una palabra que no cabe («Moderada») se parte por sílabas, con guion,
            // no por letras: la opción crece a dos líneas y nunca corta.
            partirPorSilabas(o);
            o.setTypeface(ResourcesCompat.getFont(getContext(), R.font.barlow_condensed_semibold));
            o.setTextSize(TypedValue.COMPLEX_UNIT_PX, r.getDimension(R.dimen.text_body));
            o.setAllCaps(false);
            o.setClickable(true);
            o.setFocusable(true);
            // Su anillo de foco lo dibuja la opción (capa()); FocoVisible no pone otro encima.
            o.setTag(R.id.tag_anillo_propio, Boolean.TRUE);
            o.setStateListAnimator(AnimatorInflater.loadStateListAnimator(getContext(), R.animator.toque));
            o.setForeground(capa(relleno));
            final int indice = i;
            o.setOnClickListener(v -> elegirConElDedo(indice));
            ViewCompat.setAccessibilityDelegate(o, new AccessibilityDelegateCompat() {
                @Override
                public void onInitializeAccessibilityNodeInfo(@NonNull View host, @NonNull AccessibilityNodeInfoCompat info) {
                    super.onInitializeAccessibilityNodeInfo(host, info);
                    boolean si = indice == elegida;
                    // El nombre para TalkBack («3 días»), si no es el texto que se ve.
                    if (host.getContentDescription() != null) info.setContentDescription(host.getContentDescription());
                    if (pestanas) {
                        info.setClassName("android.app.ActionBar$Tab");
                        info.setRoleDescription(getResources().getString(R.string.selector_pestana));
                        info.setSelected(si);
                    } else {
                        info.setClassName(RadioButton.class.getName());
                        info.setCheckable(true);
                        info.setChecked(si);
                    }
                    info.setCollectionItemInfo(AccessibilityNodeInfoCompat.CollectionItemInfoCompat.obtain(
                            0, 1, indice, 1, false, si));
                }
            });
            fila.addView(o, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f));
            opciones[i] = o;
        }
        aplicarActivo(isEnabled());
        pintar(false);
        colocarPildora(false);
    }

    /** Como {@link #setOpciones(CharSequence[], CharSequence[])}, con el nombre igual a lo que se ve. */
    public void setOpciones(@NonNull CharSequence[] textos) {
        setOpciones(textos, null);
    }

    /** El nombre del grupo para TalkBack («Unidad», «Sexo»). */
    public void setNombreGrupo(@Nullable CharSequence nombre) {
        nombreGrupo = nombre;
    }

    /** true: semántica de pestañas («pestaña, 1 de 3, seleccionada») en vez de botones de opción. */
    public void setPestanas(boolean pestanas) {
        this.pestanas = pestanas;
    }

    public void setAlElegir(@Nullable AlElegir alElegir) {
        this.alElegir = alElegir;
    }

    /** La elegida, o -1. */
    public int getElegida() {
        return elegida;
    }

    /** Cuántas opciones hay. */
    public int getCuantas() {
        return opciones.length;
    }

    /** El ancho con que está colocada la píldora (para los tests). */
    @androidx.annotation.VisibleForTesting
    int anchoPildora() {
        return pildora.getWidth();
    }

    /** La vista de una opción (para ocultarla, por ejemplo). */
    @NonNull
    public TextView getOpcion(int indice) {
        return opciones[indice];
    }

    /**
     * Elige una opción, o ninguna con -1. No avisa a {@link AlElegir}: eso es solo del dedo.
     *
     * @param animar con la píldora viajando (al cambiar) o en su sitio (al pintar o restaurar).
     */
    public void setElegida(int indice, boolean animar) {
        boolean primera = elegida < 0;
        elegida = indice >= 0 && indice < opciones.length ? indice : -1;
        pintar(animar);
        // La primera vez no hay de dónde viajar: aparece en su sitio.
        colocarPildora(animar && !primera);
        for (TextView o : opciones) o.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        aplicarActivo(enabled);
    }

    private void aplicarActivo(boolean enabled) {
        for (TextView o : opciones) o.setEnabled(enabled);
        setAlpha(enabled ? 1f : ResourcesCompat.getFloat(getResources(), R.dimen.alfa_desactivado));
    }

    // Cada vez que se colocan las opciones, la píldora se ajusta a la elegida: al cambiar las
    // opciones sin que cambie el ancho de la fila (otra ficha con otras unidades), conservaba el
    // ancho de antes (lo vio la prueba en producción: 434 px para una opción de 487).
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (!viajando) colocarPildora(false);
    }

    // La pista, con el radio completo para el alto que tenga (crece con letra grande).
    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        if (getBackground() instanceof GradientDrawable) ((GradientDrawable) getBackground()).setCornerRadius(h / 2f);
    }

    private void elegirConElDedo(int indice) {
        if (!isEnabled() || indice == elegida) return;
        Movimiento.vibrar(opciones[indice], Movimiento.Vibracion.LIGERA);
        setElegida(indice, true);
        if (alElegir != null) alElegir.elegida(indice);
    }

    // Pulsada, una capa del 10 % del texto; con foco de teclado, un anillo de 2 dp. Las dos, 4 dp
    // hacia dentro y con el radio completo, como la píldora.
    @NonNull
    private android.graphics.drawable.Drawable capa(int relleno) {
        Resources r = getResources();
        float radio = radioCompleto();
        int sobre = colorSobre;
        int alfa = Math.round(255 * ResourcesCompat.getFloat(r, R.dimen.selector_capa_pulsado));
        GradientDrawable pulsado = new GradientDrawable();
        pulsado.setCornerRadius(radio);
        pulsado.setColor((sobre & 0x00FFFFFF) | (alfa << 24));
        GradientDrawable foco = new GradientDrawable();
        foco.setCornerRadius(radio);
        foco.setStroke(r.getDimensionPixelSize(R.dimen.selector_anillo_foco), sobre);
        StateListDrawable s = new StateListDrawable();
        s.addState(new int[]{android.R.attr.state_pressed}, pulsado);
        s.addState(new int[]{android.R.attr.state_focused}, foco);
        s.addState(new int[]{}, new ColorDrawable(0));
        return new android.graphics.drawable.InsetDrawable(s, 0, relleno, 0, relleno);
    }

    // Un radio que el dibujo recorta a la mitad del lado corto: radio completo con cualquier alto
    // (la opción crece con letra grande).
    private float radioCompleto() {
        return getResources().getDimension(R.dimen.zona_tactil);
    }

    private void colocarPildora(boolean animar) {
        if (elegida < 0 || elegida >= opciones.length) {
            pildora.animate().cancel();
            pildora.setVisibility(INVISIBLE);
            return;
        }
        TextView o = opciones[elegida];
        if (o.getWidth() == 0) return;
        // La píldora: la opción menos 4 dp arriba y abajo (40 dp con una línea), radio completo.
        int relleno = getResources().getDimensionPixelSize(R.dimen.selector_relleno);
        LayoutParams lp = (LayoutParams) pildora.getLayoutParams();
        int altoPildora = o.getHeight() - 2 * relleno;
        if (lp.width != o.getWidth() || lp.height != altoPildora || lp.topMargin != relleno) {
            lp.width = o.getWidth();
            lp.height = altoPildora;
            lp.topMargin = relleno;
            ((GradientDrawable) pildora.getBackground()).setCornerRadius(radioCompleto());
            pildora.setLayoutParams(lp);
        }
        // Y se coloca ya: cambiar los LayoutParams dentro de onLayout no vuelve a medir la píldora
        // hasta otro pase, que puede no llegar (se quedaba con el ancho de las opciones de antes).
        if (pildora.getWidth() != lp.width || pildora.getHeight() != lp.height) {
            pildora.layout(getPaddingLeft(), relleno, getPaddingLeft() + lp.width, relleno + lp.height);
        }
        float x = getLayoutDirection() == LAYOUT_DIRECTION_RTL
                ? -(fila.getWidth() - o.getRight()) : o.getLeft();
        pildora.setVisibility(VISIBLE);
        if (animar && !Movimiento.quieto(getContext())) {
            viajando = true;
            pildora.animate().translationX(x).setDuration(Movimiento.PILDORA)
                    .setInterpolator(Movimiento.REBOTE).withEndAction(() -> viajando = false).start();
        } else {
            viajando = false;
            pildora.animate().cancel();
            pildora.setTranslationX(x);
        }
    }

    // El texto de cada opción: sobre naranja la elegida, en gris secundario las demás.
    private void pintar(boolean animar) {
        int texto = colorTexto;
        int textoElegido = colorTextoElegido;
        boolean quieto = !animar || Movimiento.quieto(getContext());
        ArgbEvaluator mezcla = new ArgbEvaluator();
        for (int i = 0; i < opciones.length; i++) {
            TextView o = opciones[i];
            int fin = i == elegida ? textoElegido : texto;
            if (quieto) {
                o.setTextColor(fin);
                continue;
            }
            int ini = o.getCurrentTextColor();
            if (ini == fin) continue;
            ValueAnimator a = ValueAnimator.ofFloat(0f, 1f);
            a.setDuration(Movimiento.TEXTO_OPCION);
            a.setInterpolator(Movimiento.ESTANDAR);
            a.addUpdateListener(an -> o.setTextColor((int) mezcla.evaluate((float) an.getAnimatedValue(), ini, fin)));
            a.start();
        }
    }

    // Un color del tema (gp_surface_2 es colorSurfaceVariant, gp_text_secondary es
    // colorOnSurfaceVariant…); sin el atributo, el recurso.
    private static int color(@NonNull Context c, int atributo, int recurso) {
        TypedValue tv = new TypedValue();
        if (c.getTheme().resolveAttribute(atributo, tv, true)) {
            return tv.resourceId != 0 ? c.getColor(tv.resourceId) : tv.data;
        }
        return ContextCompat.getColor(c, recurso);
    }

    // Partir por sílabas, con guion. La constante de Layout vale lo mismo que la de LineBreaker,
    // que lint pide pero no existe hasta la API 29 (la app empieza en la 24).
    @android.annotation.SuppressLint("WrongConstant")
    private static void partirPorSilabas(@NonNull TextView o) {
        o.setBreakStrategy(android.text.Layout.BREAK_STRATEGY_HIGH_QUALITY);
        o.setHyphenationFrequency(android.text.Layout.HYPHENATION_FREQUENCY_FULL);
    }

    // ── El giro: la elegida se guarda con la vista (si tiene id) ───────────────

    @Nullable
    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle b = new Bundle();
        b.putParcelable(CLAVE_SUPER, super.onSaveInstanceState());
        b.putInt(CLAVE_ELEGIDA, elegida);
        return b;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable estado) {
        if (estado instanceof Bundle) {
            Bundle b = (Bundle) estado;
            super.onRestoreInstanceState(b.getParcelable(CLAVE_SUPER));
            setElegida(b.getInt(CLAVE_ELEGIDA, -1), false);
        } else {
            super.onRestoreInstanceState(estado);
        }
    }

    // Las opciones son vistas creadas aquí, sin id: que no guarden ni restauren su estado.
    @Override
    protected void dispatchSaveInstanceState(android.util.SparseArray<Parcelable> container) {
        dispatchFreezeSelfOnly(container);
    }

    @Override
    protected void dispatchRestoreInstanceState(android.util.SparseArray<Parcelable> container) {
        dispatchThawSelfOnly(container);
    }
}
