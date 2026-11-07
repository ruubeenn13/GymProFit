package es.pmdm.gymprofit.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import es.pmdm.gymprofit.R;

// ============================================================
// FilaQueBaja — los kilos a la izquierda y las barras a la derecha, y si no caben, las barras
// debajo (GP-232).
//
// Con letra 1,3 los kilos crecen y las barras no: si los dos no caben en una línea con su hueco
// (espacio_12) entre ellos, las barras bajan bajo los kilos, alineadas a la izquierda, a
// espacio_8. Así ningún texto se corta. Los dos hijos van por su orden: primero los kilos y
// luego las barras; en la misma línea, se alinean por abajo, como en el lienzo.
// ============================================================
public class FilaQueBaja extends ViewGroup {

    private final int hueco;
    private final int separacion;
    private boolean abajo;
    // En la misma línea, centrados en vez de por abajo: un título junto a un botón (GP-241).
    private boolean centrados;

    public FilaQueBaja(@NonNull Context context) { this(context, null); }

    public FilaQueBaja(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        hueco = getResources().getDimensionPixelSize(R.dimen.espacio_12);
        separacion = getResources().getDimensionPixelSize(R.dimen.espacio_8);
    }

    /** Centra los dos hijos en vertical cuando van en la misma línea (por defecto, por abajo). */
    public void setCentrados(boolean centrados) {
        this.centrados = centrados;
        requestLayout();
    }

    /** true si las barras han bajado bajo los kilos (para los tests y el revisor). */
    public boolean barrasAbajo() {
        return abajo;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int disponible = MeasureSpec.getSize(widthMeasureSpec) - getPaddingLeft() - getPaddingRight();
        boolean sinLimite = MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED;
        View kilos = getChildAt(0);
        View barras = getChildCount() > 1 ? getChildAt(1) : null;
        int mide = sinLimite ? MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
                : MeasureSpec.makeMeasureSpec(Math.max(0, disponible), MeasureSpec.AT_MOST);
        measureChild(kilos, mide, heightMeasureSpec);
        boolean hayBarras = barras != null && barras.getVisibility() != GONE;
        if (hayBarras) measureChild(barras, mide, heightMeasureSpec);

        int anchoBarras = hayBarras ? barras.getMeasuredWidth() : 0;
        int altoBarras = hayBarras ? barras.getMeasuredHeight() : 0;
        abajo = hayBarras && !sinLimite && kilos.getMeasuredWidth() + hueco + anchoBarras > disponible;
        int alto = abajo ? kilos.getMeasuredHeight() + separacion + altoBarras
                : Math.max(kilos.getMeasuredHeight(), altoBarras);
        int ancho = sinLimite ? kilos.getMeasuredWidth() + (hayBarras ? hueco + anchoBarras : 0)
                : MeasureSpec.getSize(widthMeasureSpec);
        setMeasuredDimension(resolveSize(ancho + (sinLimite ? getPaddingLeft() + getPaddingRight() : 0), widthMeasureSpec),
                resolveSize(alto + getPaddingTop() + getPaddingBottom(), heightMeasureSpec));
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        View kilos = getChildAt(0);
        View barras = getChildCount() > 1 ? getChildAt(1) : null;
        int izq = getPaddingLeft();
        int arriba = getPaddingTop();
        int der = r - l - getPaddingRight();
        boolean hayBarras = barras != null && barras.getVisibility() != GONE;
        if (abajo) {
            kilos.layout(izq, arriba, izq + kilos.getMeasuredWidth(), arriba + kilos.getMeasuredHeight());
            int y = arriba + kilos.getMeasuredHeight() + separacion;
            barras.layout(izq, y, izq + barras.getMeasuredWidth(), y + barras.getMeasuredHeight());
            return;
        }
        int fondo = arriba + Math.max(kilos.getMeasuredHeight(), hayBarras ? barras.getMeasuredHeight() : 0);
        if (centrados) {
            int alto = fondo - arriba;
            int yk = arriba + (alto - kilos.getMeasuredHeight()) / 2;
            kilos.layout(izq, yk, izq + kilos.getMeasuredWidth(), yk + kilos.getMeasuredHeight());
            if (hayBarras) {
                int yb = arriba + (alto - barras.getMeasuredHeight()) / 2;
                barras.layout(der - barras.getMeasuredWidth(), yb, der, yb + barras.getMeasuredHeight());
            }
            return;
        }
        kilos.layout(izq, fondo - kilos.getMeasuredHeight(), izq + kilos.getMeasuredWidth(), fondo);
        if (hayBarras) {
            barras.layout(der - barras.getMeasuredWidth(), fondo - barras.getMeasuredHeight(), der, fondo);
        }
    }
}
