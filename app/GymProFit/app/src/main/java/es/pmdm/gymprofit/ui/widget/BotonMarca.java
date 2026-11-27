package es.pmdm.gymprofit.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ScrollingView;

import com.google.android.material.card.MaterialCardView;

import es.pmdm.gymprofit.ui.cristal.Hundir;

// ============================================================
// BotonMarca — el botón principal de Liquid Glass (GP-285, lote 1c; DEC-070).
//
// El mismo material que el «+» de la barra: el naranja de la marca macizo con su acabado
// (bg_marca, Cristal.MARCA), no cristal, para que la acción principal no se confunda con lo
// demás. Se usa con el estilo Widget.GymProFit.Boton.Marca. Nunca dos en la misma vista.
//
// Es un BotonExpresivo que no cambia de forma al pulsarlo: se hunde y se le enciende el brillo
// (Hundir). Su sombra naranja cae fuera de su caja: sus padres no recortan a sus hijos, hasta
// la tarjeta o lo que se desplaza (que siguen recortando lo suyo).
// ============================================================
public class BotonMarca extends BotonExpresivo {

    public BotonMarca(@NonNull Context context) {
        this(context, null);
    }

    public BotonMarca(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        Hundir.alPulsar(this);
    }

    @Override
    protected boolean cambiaForma() {
        return false;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        quitarRecorte(this);
    }

    /** Que la sombra de un botón no la corten sus padres, hasta su tarjeta o lo que se desplaza. */
    static void quitarRecorte(@NonNull android.view.View boton) {
        for (ViewParent p = boton.getParent(); p instanceof ViewGroup; p = p.getParent()) {
            if (p instanceof MaterialCardView || p instanceof ScrollingView
                    || p instanceof ScrollView || p instanceof HorizontalScrollView) break;
            ((ViewGroup) p).setClipChildren(false);
        }
    }
}
