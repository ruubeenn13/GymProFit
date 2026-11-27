package es.pmdm.gymprofit.ui.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.TooltipCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.ui.cristal.CristalVivo;
import es.pmdm.gymprofit.utils.NavTabs;

// ============================================================
// BarraNavegacion — la barra de abajo (GP-105; Liquid Glass en GP-285, DEC-070): una cápsula
// de cristal vivo con Inicio · Entrenar · Nutrición · Progreso y, separado a su derecha, el «+».
//
// Flota sobre el contenido, que pasa por debajo: a 16 dp de los lados y 26 del borde de abajo,
// más el hueco de la barra de gestos o de botones del sistema. La etiqueta se ve siempre; la
// pestaña activa lleva la «lente» (bg_lente), el icono relleno y la etiqueta en negrita, los
// dos en el acento; las demás, el icono en contorno. El «+» no es una pestaña: abre las
// acciones rápidas y, abierto, gira a «×». Es del mismo cristal vivo que la cápsula, teñido de
// naranja (cristalAccion): gira su icono, no el cristal, que lleva el reflejo arriba.
//
// Son vistas normales infladas de XML, no una vista dibujada a mano: cada pestaña es
// un botón con su nombre, y lo que TalkBack no sabría deducir se le dice aquí —que es
// una pestaña, cuál de cuatro, si está seleccionada, y si las acciones rápidas están
// abiertas o cerradas— (DEC-019).
//
// El hueco de la barra de gestos o de botones del sistema se suma aquí (aplicarInsetInferior).
// La barra se superpone al contenido: el relleno inferior de cada pestaña lo pone
// MainActivity con el alto del pie. Lo que queda a los lados y debajo de la cápsula es
// relleno transparente y no se queda con los toques.
//
// Las etiquetas crecen con la letra del sistema solo hasta 1,3 (GP-128) y nunca más de lo que
// cabe en su pestaña (en un móvil estrecho, «Nutrición» a 1,3 no cabe). Cada botón enseña su
// nombre en un tooltip al mantenerlo pulsado.
// ============================================================
public class BarraNavegacion extends LinearLayout {

    /** Lo que la barra pide a quien la aloja. */
    public interface Oyente {
        /** Se ha tocado una pestaña (NavTabs.INICIO…PROGRESO). */
        void onPestana(int pestana);
        /** Se ha tocado el «+» (o la «×»). */
        void onAcciones();
    }

    // Por pestaña: etiqueta, icono en contorno e icono relleno, en el orden de NavTabs.
    private static final int[] ETIQUETAS = {
            R.string.nav_inicio, R.string.nav_entrenar, R.string.nav_nutricion, R.string.nav_progreso};
    private static final int[] ICONOS = {
            R.drawable.ic_ms_home, R.drawable.ic_ms_fitness_center,
            R.drawable.ic_ms_restaurant, R.drawable.ic_ms_monitoring};
    private static final int[] ICONOS_RELLENOS = {
            R.drawable.ic_ms_home_fill, R.drawable.ic_ms_fitness_center_fill,
            R.drawable.ic_ms_restaurant_fill, R.drawable.ic_ms_monitoring_fill};

    private final View[] pestanas = new View[NavTabs.TOTAL];
    private CristalVivo btnAcciones;
    private ImageView ivAcciones;
    private Oyente oyente;
    private int activa = -1;
    private boolean accionesAbiertas = false;

    private int colorActiva, colorResto;
    private Typeface normal, negrita;
    private final int margenLado, margenAbajo;

    public BarraNavegacion(Context c) { this(c, null); }

    public BarraNavegacion(Context c, @Nullable AttributeSet a) {
        super(c, a);
        setOrientation(HORIZONTAL);
        // La sombra de la cápsula se pinta fuera de ella.
        setClipChildren(false);
        setClipToPadding(false);
        margenLado = getResources().getDimensionPixelSize(R.dimen.barra_margen_lado);
        margenAbajo = getResources().getDimensionPixelSize(R.dimen.barra_margen_abajo);
        aplicarInsetInferior(0);
        LayoutInflater.from(c).inflate(R.layout.view_barra_navegacion, this, true);

        // Sobre el cristal vivo: medidos en sus tres niveles (ContrasteCristalTest).
        colorActiva = ContextCompat.getColor(c, R.color.gp_vivo_acento);
        colorResto = ContextCompat.getColor(c, R.color.gp_vivo_texto);
        normal = ResourcesCompat.getFont(c, R.font.barlow);
        negrita = Typeface.create(normal, Typeface.BOLD);

        float tamEtiqueta = tamanoEtiqueta(Float.MAX_VALUE);
        int[] ids = {R.id.tabInicio, R.id.tabEntrenar, R.id.tabNutricion, R.id.tabProgreso};
        for (int i = 0; i < ids.length; i++) {
            final int indice = i;
            View p = findViewById(ids[i]);
            pestanas[i] = p;
            // Su anillo de foco va en la píldora (anillo_pildora), no alrededor de la pestaña (FocoVisible).
            p.setTag(R.id.tag_anillo_propio, Boolean.TRUE);
            TextView etiqueta = p.findViewById(R.id.tvEtiqueta);
            etiqueta.setText(ETIQUETAS[i]);
            etiqueta.setTextSize(TypedValue.COMPLEX_UNIT_PX, tamEtiqueta);
            p.setContentDescription(c.getString(ETIQUETAS[i]));
            // Mantener pulsado enseña el nombre: a letra grande la etiqueta se queda en 1,3.
            TooltipCompat.setTooltipText(p, c.getString(ETIQUETAS[i]));
            p.setOnClickListener(v -> { if (oyente != null) oyente.onPestana(indice); });
            ViewCompat.setAccessibilityDelegate(p, new AccessibilityDelegateCompat() {
                @Override
                public void onInitializeAccessibilityNodeInfo(@NonNull View host,
                                                              @NonNull AccessibilityNodeInfoCompat info) {
                    super.onInitializeAccessibilityNodeInfo(host, info);
                    info.setRoleDescription(getContext().getString(R.string.nav_rol_pestana));
                    info.setSelected(indice == activa);
                    info.setCollectionItemInfo(AccessibilityNodeInfoCompat.CollectionItemInfoCompat
                            .obtain(0, 1, indice, 1, false, indice == activa));
                }
            });
        }

        // La fila entera es la colección de cuatro pestañas: «pestaña 2 de 4».
        View fila = findViewById(R.id.filaBarra);
        ViewCompat.setAccessibilityDelegate(fila, new AccessibilityDelegateCompat() {
            @Override
            public void onInitializeAccessibilityNodeInfo(@NonNull View host,
                                                          @NonNull AccessibilityNodeInfoCompat info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setCollectionInfo(AccessibilityNodeInfoCompat.CollectionInfoCompat
                        .obtain(1, NavTabs.TOTAL, false));
            }
        });
        fila.setContentDescription(c.getString(R.string.nav_principal));

        btnAcciones = findViewById(R.id.btnAcciones);
        ivAcciones = findViewById(R.id.ivAcciones);
        btnAcciones.setOnClickListener(v -> { if (oyente != null) oyente.onAcciones(); });
        // Su vista es el cristal, no un botón: a TalkBack se le dice que lo es.
        ViewCompat.setAccessibilityDelegate(btnAcciones, new AccessibilityDelegateCompat() {
            @Override
            public void onInitializeAccessibilityNodeInfo(@NonNull View host,
                                                          @NonNull AccessibilityNodeInfoCompat info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setClassName(Button.class.getName());
            }
        });
        // El «+» no tiene etiqueta: su nombre, al mantenerlo pulsado, como las pestañas.
        TooltipCompat.setTooltipText(btnAcciones, btnAcciones.getContentDescription());
        BotonExpresivo.alPulsar(btnAcciones);
        pintarAcciones(false);
    }

    /** Hasta dónde crecen las etiquetas con la letra del sistema (GP-128). */
    static final float ESCALA_MAXIMA_ETIQUETA = 1.3f;

    /**
     * Escala de las etiquetas de la barra para una escala de letra del sistema (GP-128).
     *
     * <p>A letra grande no caben ni en dos líneas —«Nutrición» es una sola palabra—, así
     * que siguen a la del sistema hasta 1,3 y ahí se quedan, como la barra de iOS. El
     * nombre entero lo dan el tooltip al mantener pulsado y TalkBack, que lo lee siempre.
     */
    public static float escalaEtiqueta(float escalaSistema) {
        return Math.min(escalaSistema, ESCALA_MAXIMA_ETIQUETA);
    }

    /**
     * La escala de las etiquetas cuando además tienen que caber (GP-285): la de la letra del
     * sistema con su tope de 1,3, o menos si la etiqueta más ancha no cabe en su pestaña; nunca
     * por debajo de 1, que es el tamaño mínimo de la app (13 sp).
     *
     * @param escalaSistema escala de letra del sistema.
     * @param anchoA1       ancho de la etiqueta más ancha a escala 1, en px.
     * @param anchoPestana  ancho disponible para ella, en px.
     */
    public static float escalaEtiqueta(float escalaSistema, float anchoA1, float anchoPestana) {
        float pedida = escalaEtiqueta(escalaSistema);
        if (anchoA1 <= 0 || anchoPestana <= 0) return pedida;
        float cabe = anchoPestana / anchoA1;
        if (pedida <= cabe) return pedida;
        // No cabe: hasta donde quepa, sin bajar de 1 (ni de lo pedido, si ya era menos).
        return Math.max(Math.min(1f, pedida), cabe);
    }

    // Tamaño de las etiquetas en px: el de text_eyebrow a la escala de escalaEtiqueta(), sin
    // pasar del ancho de una pestaña. Se calcula a mano porque un tamaño en sp crece sin tope
    // con la letra del sistema.
    private float tamanoEtiqueta(float anchoPestana) {
        TypedValue valor = new TypedValue();
        getResources().getValue(R.dimen.text_eyebrow, valor, true);
        float base = TypedValue.complexToFloat(valor.data) * getResources().getDisplayMetrics().density;
        // La más ancha, en negrita (la activa), a escala 1.
        android.graphics.Paint pincel = new android.graphics.Paint();
        pincel.setTypeface(negrita);
        pincel.setTextSize(base);
        float ancha = 0f;
        for (int etiqueta : ETIQUETAS) ancha = Math.max(ancha, pincel.measureText(getContext().getString(etiqueta)));
        return base * escalaEtiqueta(getResources().getConfiguration().fontScale, ancha, anchoPestana);
    }

    // Con el ancho ya sabido, las etiquetas se ajustan a lo que cabe en una pestaña.
    @Override
    protected void onLayout(boolean cambia, int l, int t, int r, int b) {
        super.onLayout(cambia, l, t, r, b);
        if (pestanas[0] == null || pestanas[0].getWidth() == 0) return;
        float aire = getResources().getDimension(R.dimen.espacio_4);
        float tam = tamanoEtiqueta(pestanas[0].getWidth() - 2 * aire);
        TextView primera = pestanas[0].findViewById(R.id.tvEtiqueta);
        if (Math.abs(primera.getTextSize() - tam) < 0.5f) return;
        for (View p : pestanas) {
            ((TextView) p.findViewById(R.id.tvEtiqueta)).setTextSize(TypedValue.COMPLEX_UNIT_PX, tam);
        }
        // El tamaño cambia a mitad de una colocación, y la medida que eso pide se pierde: cada
        // etiqueta se quedaba con la caja del tamaño anterior y su texto descentrado (visto a
        // letra 1,3 al volver a la app). Se pide otra en cuanto acaba esta.
        post(this::requestLayout);
    }

    public void setOyente(Oyente oyente) { this.oyente = oyente; }

    /** Deja marcada la pestaña activa: la lente, el icono relleno y la etiqueta en negrita. */
    public void setActiva(int pestana) {
        activa = pestana;
        for (int i = 0; i < pestanas.length; i++) {
            boolean esta = i == pestana;
            View p = pestanas[i];
            p.setSelected(esta);
            p.findViewById(R.id.flPildora).setBackgroundResource(esta ? R.drawable.bg_lente : 0);
            ImageView icono = p.findViewById(R.id.ivIcono);
            icono.setImageResource(esta ? ICONOS_RELLENOS[i] : ICONOS[i]);
            icono.setImageTintList(ColorStateList.valueOf(esta ? colorActiva : colorResto));
            TextView etiqueta = p.findViewById(R.id.tvEtiqueta);
            etiqueta.setTextColor(esta ? colorActiva : colorResto);
            etiqueta.setTypeface(esta ? negrita : normal);
        }
    }

    /** Pestaña activa (NavTabs.INICIO…PROGRESO). */
    public int getActiva() { return activa; }

    /**
     * El «+» abierto o cerrado. Su icono gira 45° —el «+» se convierte en «×»— con la duración
     * del sistema, así que con «Quitar animaciones» el cambio es inmediato.
     */
    public void setAccionesAbiertas(boolean abiertas) {
        if (abiertas == accionesAbiertas) return;
        accionesAbiertas = abiertas;
        ivAcciones.animate().cancel();
        ivAcciones.animate().rotation(abiertas ? 45f : 0f)
                .setDuration(getResources().getInteger(android.R.integer.config_shortAnimTime))
                .start();
        pintarAcciones(abiertas);
    }

    // Nombre y estado del «+» para TalkBack: «Acciones rápidas, cerradas, botón».
    private void pintarAcciones(boolean abiertas) {
        ViewCompat.setStateDescription(btnAcciones, getContext().getString(
                abiertas ? R.string.acciones_estado_abiertas : R.string.acciones_estado_cerradas));
        ViewCompat.replaceAccessibilityAction(btnAcciones,
                AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_CLICK,
                getContext().getString(abiertas ? R.string.acciones_accion_cerrar : R.string.acciones_accion_abrir),
                null);
    }

    /**
     * Deja la barra a sus márgenes (16 dp a los lados, 26 abajo) más el hueco de la barra de
     * gestos o de botones del sistema.
     */
    public void aplicarInsetInferior(int px) {
        setPadding(margenLado, 0, margenLado, margenAbajo + px);
    }
}
