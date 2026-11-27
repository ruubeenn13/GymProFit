package es.pmdm.gymprofit.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import es.pmdm.gymprofit.R;

// ============================================================
// FilaBotones — el principal y el secundario de una tarjeta, en fila o uno sobre otro
// (GP-285, lote 1c).
//
// En fila, el principal ocupa lo que deja el secundario. Si su texto no cabe entero ahí (letra
// 2,0, móviles estrechos: «Empezar» se cortaba a «Empeza»), se ponen uno sobre otro, los dos a
// todo el ancho y el principal arriba. También se apilan a la fuerza cuando quien los usa lo
// pide (dos textos largos, forzarApilado). Un botón nunca se corta: DEC-019.
//
// Lo decide al medirse, con el ancho que cada botón pide sin límite, y solo toca a sus hijos
// cuando la decisión cambia (la misma regla que FilaRecord): nada de onLayout.
// Espera dos hijos: el principal primero y el secundario después.
// ============================================================
public class FilaBotones extends LinearLayout {

    private final int separacion;
    private boolean forzado;
    // La disposición puesta ahora: null hasta la primera medida.
    @Nullable private Boolean apilada;

    public FilaBotones(@NonNull Context context) {
        this(context, null);
    }

    public FilaBotones(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        separacion = getResources().getDimensionPixelSize(R.dimen.espacio_8);
    }

    /** Uno sobre otro siempre, quepan o no (dos textos largos). */
    public void forzarApilado(boolean forzar) {
        if (forzado == forzar) return;
        forzado = forzar;
        requestLayout();
    }

    /** Si están uno sobre otro ahora (para pruebas). */
    public boolean apilada() {
        return apilada != null && apilada;
    }

    @Override
    protected void onMeasure(int anchoSpec, int altoSpec) {
        // Sin ancho dado (algo que se desplaza en horizontal) caben siempre.
        boolean apilar = forzado || (MeasureSpec.getMode(anchoSpec) != MeasureSpec.UNSPECIFIED
                && !cabenEnFila(MeasureSpec.getSize(anchoSpec)));
        if (apilada == null || apilada != apilar) colocar(apilar);
        super.onMeasure(anchoSpec, altoSpec);
    }

    // Si el principal entero y el secundario caben lado a lado en ese ancho.
    private boolean cabenEnFila(int ancho) {
        if (getChildCount() < 2) return true;
        View principal = getChildAt(0), secundario = getChildAt(1);
        int libre = ancho - getPaddingStart() - getPaddingEnd() - deseado(principal);
        if (secundario.getVisibility() == GONE) return libre >= 0;
        return libre - separacion - deseado(secundario) >= 0;
    }

    private static int deseado(@NonNull View v) {
        int nada = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        v.measure(nada, nada);
        return v.getMeasuredWidth();
    }

    private void colocar(boolean apilar) {
        apilada = apilar;
        setOrientation(apilar ? VERTICAL : HORIZONTAL);
        if (getChildCount() < 2) return;
        LayoutParams p1 = (LayoutParams) getChildAt(0).getLayoutParams();
        p1.width = apilar ? ViewGroup.LayoutParams.MATCH_PARENT : 0;
        p1.weight = apilar ? 0 : 1;
        LayoutParams p2 = (LayoutParams) getChildAt(1).getLayoutParams();
        p2.width = apilar ? ViewGroup.LayoutParams.MATCH_PARENT : ViewGroup.LayoutParams.WRAP_CONTENT;
        p2.setMarginStart(apilar ? 0 : separacion);
        p2.leftMargin = apilar ? 0 : separacion;
        p2.topMargin = apilar ? separacion : 0;
        // Sus medidas cambian en esta misma pasada: que las vuelvan a calcular.
        getChildAt(0).forceLayout();
        getChildAt(1).forceLayout();
    }
}
