package es.pmdm.gymprofit.ui.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.utils.NavTabs;

// ============================================================
// BarraNavegacion — la barra fija de abajo (GP-105): Inicio · Entrenar · «+» ·
// Nutrición · Progreso.
//
// Sustituye a la FloatingNavBar. La etiqueta se ve siempre; la pestaña activa lleva
// la píldora y el icono relleno, las demás el icono en contorno. El «+» central no es
// una pestaña: abre las acciones rápidas y, abierto, gira a «×».
//
// Son vistas normales infladas de XML, no una vista dibujada a mano: cada pestaña es
// un botón con su nombre, y lo que TalkBack no sabría deducir se le dice aquí —que es
// una pestaña, cuál de cuatro, si está seleccionada, y si las acciones rápidas están
// abiertas o cerradas— (DEC-019).
//
// El hueco de la barra de gestos o de botones del sistema se suma aquí y en ningún
// otro sitio (aplicarInsetInferior): la barra no se superpone al contenido, así que
// ninguna pestaña tiene que reservar nada (GP-059).
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
    private ImageButton btnAcciones;
    private Oyente oyente;
    private int activa = -1;
    private boolean accionesAbiertas = false;

    private int colorMarca, colorTexto, colorSecundario;
    private Typeface normal, negrita;

    public BarraNavegacion(Context c) { this(c, null); }

    public BarraNavegacion(Context c, @Nullable AttributeSet a) {
        super(c, a);
        setOrientation(VERTICAL);
        setBackgroundColor(color(com.google.android.material.R.attr.colorSurface));
        LayoutInflater.from(c).inflate(R.layout.view_barra_navegacion, this, true);

        colorMarca = color(com.google.android.material.R.attr.colorPrimary);
        colorTexto = color(com.google.android.material.R.attr.colorOnSurface);
        colorSecundario = color(com.google.android.material.R.attr.colorOnSurfaceVariant);
        normal = ResourcesCompat.getFont(c, R.font.barlow);
        negrita = Typeface.create(normal, Typeface.BOLD);

        int[] ids = {R.id.tabInicio, R.id.tabEntrenar, R.id.tabNutricion, R.id.tabProgreso};
        for (int i = 0; i < ids.length; i++) {
            final int indice = i;
            View p = findViewById(ids[i]);
            pestanas[i] = p;
            TextView etiqueta = p.findViewById(R.id.tvEtiqueta);
            etiqueta.setText(ETIQUETAS[i]);
            p.setContentDescription(c.getString(ETIQUETAS[i]));
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
        btnAcciones.setOnClickListener(v -> { if (oyente != null) oyente.onAcciones(); });
        pintarAcciones(false);
    }

    public void setOyente(Oyente oyente) { this.oyente = oyente; }

    /** Deja marcada la pestaña activa: píldora, icono relleno y etiqueta en negrita. */
    public void setActiva(int pestana) {
        activa = pestana;
        for (int i = 0; i < pestanas.length; i++) {
            boolean esta = i == pestana;
            View p = pestanas[i];
            p.setSelected(esta);
            p.findViewById(R.id.flPildora).setBackgroundResource(esta ? R.drawable.bg_pildora_pestana : 0);
            ImageView icono = p.findViewById(R.id.ivIcono);
            icono.setImageResource(esta ? ICONOS_RELLENOS[i] : ICONOS[i]);
            icono.setImageTintList(ColorStateList.valueOf(esta ? colorMarca : colorSecundario));
            TextView etiqueta = p.findViewById(R.id.tvEtiqueta);
            etiqueta.setTextColor(esta ? colorTexto : colorSecundario);
            etiqueta.setTypeface(esta ? negrita : normal);
        }
    }

    /** Pestaña activa (NavTabs.INICIO…PROGRESO). */
    public int getActiva() { return activa; }

    /**
     * El «+» abierto o cerrado. Gira 45° —el «+» se convierte en «×»— con la duración
     * del sistema, así que con «Quitar animaciones» el cambio es inmediato.
     */
    public void setAccionesAbiertas(boolean abiertas) {
        if (abiertas == accionesAbiertas) return;
        accionesAbiertas = abiertas;
        btnAcciones.animate().cancel();
        btnAcciones.animate().rotation(abiertas ? 45f : 0f)
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
     * Suma bajo la barra el hueco de la barra de gestos o de botones del sistema. Es
     * el único sitio de la app que lo hace para la navegación principal.
     */
    public void aplicarInsetInferior(int px) {
        setPadding(getPaddingLeft(), getPaddingTop(), getPaddingRight(), px);
    }

    private int color(@AttrRes int attr) {
        TypedValue tv = new TypedValue();
        getContext().getTheme().resolveAttribute(attr, tv, true);
        return tv.data;
    }
}
