package es.pmdm.gymprofit.ui.widget;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.view.View;
import android.view.ViewTreeObserver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;

import es.pmdm.gymprofit.R;

// ============================================================
// FocoTarjeta — el anillo de foco de las tarjetas (GP-227): con el foco del teclado o del DPAD,
// un borde de 2 dp en gp_on_surface, como el selector de opciones (GP-224); pulsada, la capa del
// 10 % la pone el estilo (Widget.GymProFit.Card, color/tarjeta_capa).
//
// MaterialCardView no cambia el grosor del borde por estado, y el borde de cada tarjeta no es
// el mismo (1 dp en claro, ninguno en oscuro, dorado en las de récord), así que no basta con un
// selector en el estilo: cada ventana escucha el foco y, a la tarjeta que lo gana, le guarda su
// borde, le pone el anillo y se lo devuelve al perderlo. En modo táctil una tarjeta no toma el
// foco, así que el anillo solo sale con teclado o DPAD. Se instala para todas las pantallas desde
// GymProFitApp.
// ============================================================
public final class FocoTarjeta implements ViewTreeObserver.OnGlobalFocusChangeListener {

    private FocoTarjeta() { }

    /** Escucha el foco de la ventana de la pantalla (una vez por ventana). */
    public static void instalar(@NonNull Activity a) {
        View decor = a.getWindow().getDecorView();
        if (decor.getTag(R.id.tag_foco_tarjeta) != null) return;
        FocoTarjeta f = new FocoTarjeta();
        decor.setTag(R.id.tag_foco_tarjeta, f);
        decor.getViewTreeObserver().addOnGlobalFocusChangeListener(f);
    }

    @Override
    public void onGlobalFocusChanged(@Nullable View antes, @Nullable View ahora) {
        if (antes instanceof MaterialCardView) quitar((MaterialCardView) antes);
        if (ahora instanceof MaterialCardView) poner((MaterialCardView) ahora);
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

    private static void poner(MaterialCardView t) {
        if (t.getTag(R.id.tag_borde_tarjeta) == null) {
            t.setTag(R.id.tag_borde_tarjeta, new Borde(t.getStrokeWidth(), t.getStrokeColorStateList()));
        }
        t.setStrokeWidth(t.getResources().getDimensionPixelSize(R.dimen.selector_anillo_foco));
        t.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(t.getContext(), R.color.gp_on_surface)));
    }

    private static void quitar(MaterialCardView t) {
        Object o = t.getTag(R.id.tag_borde_tarjeta);
        if (!(o instanceof Borde)) return;
        Borde b = (Borde) o;
        t.setTag(R.id.tag_borde_tarjeta, null);
        t.setStrokeWidth(b.ancho);
        if (b.color != null) t.setStrokeColor(b.color);
    }
}
