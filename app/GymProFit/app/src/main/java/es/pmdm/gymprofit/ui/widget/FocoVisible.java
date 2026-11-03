package es.pmdm.gymprofit.ui.widget;

import android.app.Activity;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;

import es.pmdm.gymprofit.R;

// ============================================================
// FocoVisible — el foco del teclado o del DPAD se ve igual en toda la app (GP-227): un anillo de
// 2 dp, POR FUERA de lo que tiene el foco y separado 2 dp de su borde. Pulsado, la capa del 10 %
// la ponen los fondos (color/tarjeta_capa).
//
// Una sola regla para todo lo pulsable, y no un fondo por pieza: los botones de Material solo
// cambiaban su capa de estado (1,13 a 1,19:1) y lo demás llevaba el gris del sistema (revisor,
// GP-228). Y por fuera porque por dentro no sirve: pisaba el texto de las filas sin aire, quedaba a
// 1,4:1 dentro de un botón relleno y no se distinguía del borde de una tarjeta oscura. Por fuera
// lo que tiene al lado es lo que hay detrás de la vista, así que su color es el colorOnSurface
// del padre (dentro de la tarjeta protagonista, gp_on_hero; fuera, gp_on_surface).
//
// El anillo va en la capa de encima de la ventana (no lo recorta ningún padre) y sigue a la vista
// en cada fotograma mientras tiene el foco (se desplaza con su lista). No llevan anillo:
//   - lo que se desplaza (scrolls, listas): no es un control, el foco se ve en lo que tiene dentro;
//   - los campos de texto: su borde de foco, el de la app;
//   - lo marcado con R.id.tag_anillo_propio (la pestaña de la barra, en su píldora, y las opciones
//     del selector, que dibuja el suyo).
// En modo táctil no hay anillo: al tocar la pantalla se quita.
// ============================================================
public final class FocoVisible implements ViewTreeObserver.OnGlobalFocusChangeListener,
        ViewTreeObserver.OnTouchModeChangeListener, ViewTreeObserver.OnPreDrawListener {

    /** Un botón cuadrado de hasta este lado (dp) es redondo: el anillo es un círculo. */
    private static final int LADO_REDONDO_DP = 64;
    /** Lo que se separa el anillo del borde de la vista (dp). */
    private static final int HUECO_DP = 2;

    private final ViewGroup decor;
    private final GradientDrawable anillo = new GradientDrawable();
    private final int[] enVentana = new int[2];
    private final int[] decorEnVentana = new int[2];
    private final android.graphics.Rect donde = new android.graphics.Rect();
    @Nullable private View conAnillo;

    private FocoVisible(ViewGroup decor) {
        this.decor = decor;
        anillo.setColor(0);
    }

    /** Escucha el foco de la ventana de la pantalla (una vez por ventana). */
    public static void instalar(@NonNull Activity a) {
        View d = a.getWindow().getDecorView();
        if (!(d instanceof ViewGroup) || d.getTag(R.id.tag_foco_tarjeta) != null) return;
        FocoVisible f = new FocoVisible((ViewGroup) d);
        d.setTag(R.id.tag_foco_tarjeta, f);
        d.getViewTreeObserver().addOnGlobalFocusChangeListener(f);
        d.getViewTreeObserver().addOnTouchModeChangeListener(f);
    }

    @Override
    public void onGlobalFocusChanged(@Nullable View antes, @Nullable View ahora) {
        quitar();
        if (ahora == null || ahora.isInTouchMode()) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ahora.setDefaultFocusHighlightEnabled(false);
        if (!llevaAnillo(ahora)) return;
        conAnillo = ahora;
        int ancho = ahora.getResources().getDimensionPixelSize(R.dimen.selector_anillo_foco);
        anillo.setStroke(ancho, colorAlrededor(ahora));
        donde.setEmpty();
        decor.getOverlay().add(anillo);
        decor.getViewTreeObserver().addOnPreDrawListener(this);
        colocar();
    }

    @Override
    public void onTouchModeChanged(boolean tactil) {
        if (tactil) quitar();
    }

    /** Recoloca el anillo en cada fotograma: la vista se desplaza con su lista. */
    @Override
    public boolean onPreDraw() {
        if (conAnillo == null) return true;
        if (!conAnillo.isAttachedToWindow() || !conAnillo.isShown()) {
            quitar();
            return true;
        }
        colocar();
        return true;
    }

    /** Si una vista lleva el anillo (lógica sin dibujo). */
    static boolean llevaAnillo(@NonNull View v) {
        return !(v instanceof ScrollView || v instanceof NestedScrollView || v instanceof HorizontalScrollView
                || v instanceof RecyclerView || v instanceof EditText
                || v.getTag(R.id.tag_anillo_propio) != null);
    }

    private void quitar() {
        if (conAnillo == null) return;
        decor.getOverlay().remove(anillo);
        decor.getViewTreeObserver().removeOnPreDrawListener(this);
        conAnillo = null;
    }

    // El anillo alrededor de la vista: separado HUECO_DP de su borde y con su forma.
    private void colocar() {
        View v = conAnillo;
        if (v == null) return;
        float d = v.getResources().getDisplayMetrics().density;
        int ancho = v.getResources().getDimensionPixelSize(R.dimen.selector_anillo_foco);
        int fuera = Math.round(HUECO_DP * d) + ancho;
        v.getLocationInWindow(enVentana);
        decor.getLocationInWindow(decorEnVentana);
        int x = enVentana[0] - decorEnVentana[0], y = enVentana[1] - decorEnVentana[1];
        // Solo si se ha movido: invalidar en cada fotograma redibujaría la ventana sin parar.
        if (donde.left == x - fuera && donde.top == y - fuera && donde.width() == v.getWidth() + 2 * fuera
                && donde.height() == v.getHeight() + 2 * fuera && anillo.getCallback() != null) return;
        donde.set(x - fuera, y - fuera, x + v.getWidth() + fuera, y + v.getHeight() + fuera);
        anillo.setBounds(donde);
        anillo.setCornerRadius(radio(v, d) + fuera);
        decor.invalidate();
    }

    // El radio del borde de la vista: el de su tarjeta o botón, un círculo si es un botón redondo,
    // y el de los chips si no.
    private static float radio(View v, float d) {
        int w = v.getWidth(), h = v.getHeight();
        if (v instanceof MaterialCardView) return ((MaterialCardView) v).getRadius();
        if (v instanceof MaterialButton) {
            float r = ((MaterialButton) v).getShapeAppearanceModel().getTopLeftCornerSize()
                    .getCornerSize(new RectF(0, 0, w, h));
            return Math.min(r, Math.min(w, h) / 2f);
        }
        boolean cuadrado = Math.abs(w - h) <= 4 * d;
        if (cuadrado && w <= LADO_REDONDO_DP * d) return Math.min(w, h) / 2f;
        return Math.min(v.getResources().getDimension(R.dimen.radio_chip), Math.min(w, h) / 2f);
    }

    // El color del texto sobre lo que hay detrás de la vista: el colorOnSurface del padre.
    private static int colorAlrededor(View v) {
        ViewParent p = v.getParent();
        View fondo = p instanceof View ? (View) p : v;
        return MaterialColors.getColor(fondo, com.google.android.material.R.attr.colorOnSurface);
    }
}
