package es.pmdm.gymprofit.ui.cristal;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.ScrollingView;

import com.google.android.material.card.MaterialCardView;

import es.pmdm.gymprofit.R;

// ============================================================
// TarjetaCristal — la tarjeta de cristal estático que lleva sombra (GP-285, lote 1b).
//
// Es la MaterialCardView de siempre (su capa de pulsar, su anillo de foco, sus estilos
// Widget.GymProFit.Card.Cristal*; el cristal lo sigue pintando el fondo de su primer hijo) con
// una cosa más: en el tema claro pinta alrededor la sombra suave del diseño (SombraCristal:
// 28 dp de difuminado, 10 hacia abajo), solo por fuera de su forma. En oscuro el color de la
// sombra es transparente y no pinta nada: es una MaterialCardView sin más.
//
// Para dibujar fuera de su caja:
//   · no se recorta a su contorno (lo de dentro ya es redondo: el cristal lo es, y la capa de
//     pulsar de la tarjeta lleva su propia máscara);
//   · sus padres no recortan a sus hijos, hasta el primero que se desplaza (incluido), que
//     queda recortado a su propia caja por quien lo contiene (un carrusel deja sitio a la
//     sombra con su relleno).
//
// Una tarjeta dentro de otra (la fila del récord) va sin sombra: app:cristalSombra="false".
// ============================================================
public class TarjetaCristal extends MaterialCardView {

    private final int colorSombra;
    private final float difuminado, baja;
    @Nullable private SombraCristal sombra;

    public TarjetaCristal(@NonNull Context context) {
        this(context, null);
    }

    public TarjetaCristal(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, com.google.android.material.R.attr.materialCardViewStyle);
    }

    public TarjetaCristal(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TarjetaCristal);
        boolean conSombra = a.getBoolean(R.styleable.TarjetaCristal_cristalSombra, true);
        a.recycle();
        colorSombra = conSombra ? ContextCompat.getColor(context, R.color.gp_cristal_sombra) : 0;
        difuminado = getResources().getDimension(R.dimen.cristal_estatico_sombra_difuminado);
        baja = getResources().getDimension(R.dimen.cristal_estatico_sombra_baja);
        if (conSombra()) setClipToOutline(false);
    }

    /** Si esta tarjeta pinta sombra: la pide y el tema la tiene (el claro). */
    public boolean conSombra() {
        return colorSombra >>> 24 != 0;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!conSombra()) return;
        // La sombra cae fuera de la tarjeta: que ningún padre la recorte, hasta el que se desplaza,
        // este incluido. Él sigue recortado a su caja por su propio padre (la pantalla, o la
        // sección), pero deja de recortar a su hijo a la caja de este: en un carrusel la fila mide
        // lo que sus tarjetas, y la sombra se cortaba en recto justo en su canto.
        for (ViewParent p = getParent(); p instanceof ViewGroup; p = p.getParent()) {
            ((ViewGroup) p).setClipChildren(false);
            if (p instanceof ScrollingView || p instanceof ScrollView || p instanceof HorizontalScrollView) break;
        }
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        if (conSombra()) {
            if (sombra == null) sombra = SombraCristal.de(getRadius(), difuminado, baja);
            sombra.pintar(canvas, getWidth(), getHeight(), colorSombra);
        }
        super.draw(canvas);
    }

    @Override
    public void setRadius(float radius) {
        super.setRadius(radius);
        sombra = null;
    }
}
