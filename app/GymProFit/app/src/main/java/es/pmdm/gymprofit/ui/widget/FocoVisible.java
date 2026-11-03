package es.pmdm.gymprofit.ui.widget;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import es.pmdm.gymprofit.R;

// ============================================================
// FocoVisible — el foco del teclado o del DPAD se ve igual en toda la app (GP-227): un anillo de
// 2 dp en gp_on_surface, el del selector de opciones (GP-224). Pulsado, la capa del 10 % la ponen
// los fondos (color/tarjeta_capa).
//
// Una sola regla para todo lo pulsable, y no un fondo por pieza: los botones de Material solo
// cambiaban su capa de estado (1,13 a 1,19:1), los redondos y las filas sin fondo pulsable
// llevaban el resaltado gris del sistema (1,17 a 1,47:1) (revisor, GP-228). Cada ventana escucha
// el foco y, a la vista que lo gana:
//   - una tarjeta (MaterialCardView): el anillo sustituye a su borde (1 dp, ninguno o dorado) y
//     se lo devuelve al perder el foco, con su mismo radio;
//   - un contenedor que se desplaza (scroll, lista): nada, ni anillo ni el velo del sistema: no es
//     un control, el foco se ve en lo que tiene dentro;
//   - un campo de texto: su borde de foco, el de la app;
//   - una vista marcada con R.id.tag_anillo_propio (la pestaña de la barra, que lo pone en su
//     píldora): nada;
//   - cualquier otra: el anillo en su capa de encima (ViewOverlay), 2 dp hacia dentro, con el radio
//     de su forma: el del botón de Material, un círculo si es un botón redondo, y el de los chips si
//     no; y sin el resaltado del sistema.
// En modo táctil no hay anillo: al tocar la pantalla se quita.
// ============================================================
public final class FocoVisible implements ViewTreeObserver.OnGlobalFocusChangeListener,
        ViewTreeObserver.OnTouchModeChangeListener {

    /** Un botón cuadrado de hasta este lado (dp) es redondo: el anillo es un círculo. */
    private static final int LADO_REDONDO_DP = 64;

    @Nullable private View conAnillo;

    private FocoVisible() { }

    /** Escucha el foco de la ventana de la pantalla (una vez por ventana). */
    public static void instalar(@NonNull Activity a) {
        View decor = a.getWindow().getDecorView();
        if (decor.getTag(R.id.tag_foco_tarjeta) != null) return;
        FocoVisible f = new FocoVisible();
        decor.setTag(R.id.tag_foco_tarjeta, f);
        decor.getViewTreeObserver().addOnGlobalFocusChangeListener(f);
        decor.getViewTreeObserver().addOnTouchModeChangeListener(f);
    }

    @Override
    public void onGlobalFocusChanged(@Nullable View antes, @Nullable View ahora) {
        if (antes != null) quitar(antes);
        if (ahora == null || ahora.isInTouchMode()) return;
        poner(ahora);
    }

    @Override
    public void onTouchModeChanged(boolean tactil) {
        if (tactil && conAnillo != null) quitar(conAnillo);
    }

    /** Qué hace el foco con una vista (lógica sin dibujo, para el test). */
    enum Trato { BORDE_DE_TARJETA, NADA, ANILLO }

    static Trato trato(@NonNull View v) {
        if (v instanceof MaterialCardView) return Trato.BORDE_DE_TARJETA;
        if (v instanceof ScrollView || v instanceof NestedScrollView || v instanceof HorizontalScrollView
                || v instanceof RecyclerView || v instanceof EditText
                || v.getTag(R.id.tag_anillo_propio) != null) {
            return Trato.NADA;
        }
        return Trato.ANILLO;
    }

    private void poner(View v) {
        sinResaltado(v);
        Trato t = trato(v);
        if (t == Trato.BORDE_DE_TARJETA) ponerBorde((MaterialCardView) v);
        else if (t == Trato.ANILLO) ponerAnillo(v);
        conAnillo = v;
    }

    private void quitar(View v) {
        if (v instanceof MaterialCardView) quitarBorde((MaterialCardView) v);
        Object o = v.getTag(R.id.tag_anillo_foco);
        if (o instanceof GradientDrawable) {
            v.getOverlay().remove((GradientDrawable) o);
            v.setTag(R.id.tag_anillo_foco, null);
        }
        if (conAnillo == v) conAnillo = null;
    }

    // El resaltado gris que Android pinta encima de lo enfocado sin estado de foco propio.
    private static void sinResaltado(View v) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) v.setDefaultFocusHighlightEnabled(false);
    }

    private static void ponerAnillo(View v) {
        if (v.getWidth() == 0 || v.getHeight() == 0) return;
        float d = v.getResources().getDisplayMetrics().density;
        int dentro = Math.round(2 * d);
        int ancho = v.getResources().getDimensionPixelSize(R.dimen.selector_anillo_foco);
        GradientDrawable anillo = new GradientDrawable();
        anillo.setColor(0);
        anillo.setStroke(ancho, ContextCompat.getColor(v.getContext(), R.color.gp_on_surface));
        int w = v.getWidth() - 2 * dentro, h = v.getHeight() - 2 * dentro;
        anillo.setCornerRadius(radio(v, w, h, d));
        anillo.setBounds(dentro, dentro, dentro + w, dentro + h);
        v.getOverlay().add(anillo);
        v.setTag(R.id.tag_anillo_foco, anillo);
    }

    // El radio del anillo según la forma de la vista.
    private static float radio(View v, int w, int h, float d) {
        if (v instanceof MaterialButton) {
            float r = ((MaterialButton) v).getShapeAppearanceModel().getTopLeftCornerSize()
                    .getCornerSize(new RectF(0, 0, w, h));
            return Math.min(r, Math.min(w, h) / 2f);
        }
        boolean cuadrado = Math.abs(v.getWidth() - v.getHeight()) <= 4 * d;
        if (cuadrado && v.getWidth() <= LADO_REDONDO_DP * d) return Math.min(w, h) / 2f;
        return Math.min(v.getResources().getDimension(R.dimen.radio_chip), Math.min(w, h) / 2f);
    }

    /** El borde que tenía la tarjeta antes del anillo. */
    private static final class Borde {
        final int ancho;
        @Nullable final ColorStateList color;

        Borde(int ancho, @Nullable ColorStateList color) {
            this.ancho = ancho;
            this.color = color;
        }
    }

    private static void ponerBorde(MaterialCardView t) {
        if (t.getTag(R.id.tag_borde_tarjeta) == null) {
            t.setTag(R.id.tag_borde_tarjeta, new Borde(t.getStrokeWidth(), t.getStrokeColorStateList()));
        }
        t.setStrokeWidth(t.getResources().getDimensionPixelSize(R.dimen.selector_anillo_foco));
        t.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(t.getContext(), R.color.gp_on_surface)));
    }

    private static void quitarBorde(MaterialCardView t) {
        Object o = t.getTag(R.id.tag_borde_tarjeta);
        if (!(o instanceof Borde)) return;
        Borde b = (Borde) o;
        t.setTag(R.id.tag_borde_tarjeta, null);
        t.setStrokeWidth(b.ancho);
        if (b.color != null) t.setStrokeColor(b.color);
    }
}
