package es.pmdm.gymprofit.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import es.pmdm.gymprofit.R;

// ============================================================
// CarruselView — un carrusel de Inicio (GP-285, lote 1b): «Hoy toca» y «Tus últimos entrenos».
//
// Un HorizontalScrollView con dos cosas más:
//
//   · El último elemento a la vista no se corta en seco contra el borde: se desvanece en un
//     fundido (carrusel_fundido, 28 dp). Es el borde difuminado de Android, que funde a
//     transparente (lo de detrás es cristal sobre el fondo ambiental, no un color liso); a la
//     izquierda solo aparece cuando hay elementos que ya han pasado.
//   · Encaja por elemento: al soltar, se queda con un elemento empezando en su sitio
//     (EncajeCarrusel hace la cuenta).
//
// Su único hijo es la fila de elementos. Cada elemento es un nodo para TalkBack (lo pone quien
// los rellena); al enfocar uno que no está a la vista, Android lo trae.
// ============================================================
public class CarruselView extends HorizontalScrollView {

    // Si el gesto en curso ha acabado en lanzamiento: entonces ya se ha encajado ahí.
    private boolean lanzado;

    public CarruselView(@NonNull Context context) {
        this(context, null);
    }

    public CarruselView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setHorizontalFadingEdgeEnabled(true);
        setFadingEdgeLength(getResources().getDimensionPixelSize(R.dimen.carrusel_fundido));
        setOverScrollMode(OVER_SCROLL_NEVER);
        setHorizontalScrollBarEnabled(false);
    }

    // El fundido va en el borde de la vista, no en el de su relleno. El carrusel no recorta a su
    // relleno (así llega al borde de su contenedor), y Android pone el borde difuminado donde
    // acaba el relleno: los últimos 20 dp volvían a verse sin fundir, con el corte en seco al
    // final. Con estas cuatro, la zona del fundido es la caja entera.
    @Override
    protected boolean isPaddingOffsetRequired() {
        return true;
    }

    @Override
    protected int getLeftPaddingOffset() {
        return -getPaddingLeft();
    }

    @Override
    protected int getRightPaddingOffset() {
        return getPaddingRight();
    }

    @Override
    protected int getTopPaddingOffset() {
        return -getPaddingTop();
    }

    @Override
    protected int getBottomPaddingOffset() {
        return getPaddingBottom();
    }

    // Un lanzamiento no deja el carrusel donde caiga: lo lleva al elemento que toca.
    @Override
    public void fling(int velocityX) {
        lanzado = true;
        encajar(velocityX);
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent ev) {
        if (ev.getActionMasked() == MotionEvent.ACTION_DOWN) lanzado = false;
        boolean usado = super.onTouchEvent(ev);
        int accion = ev.getActionMasked();
        // Soltado sin lanzar (o gesto cancelado): al elemento más cercano.
        if ((accion == MotionEvent.ACTION_UP || accion == MotionEvent.ACTION_CANCEL) && !lanzado) encajar(0);
        return usado;
    }

    private void encajar(int velocidad) {
        if (getChildCount() == 0 || !(getChildAt(0) instanceof ViewGroup)) return;
        ViewGroup fila = (ViewGroup) getChildAt(0);
        int n = fila.getChildCount();
        int[] bordes = new int[n];
        int visibles = 0;
        for (int i = 0; i < n; i++) {
            View hijo = fila.getChildAt(i);
            if (hijo.getVisibility() != GONE) bordes[visibles++] = hijo.getLeft();
        }
        int maximo = Math.max(0, fila.getWidth() + getPaddingLeft() + getPaddingRight() - getWidth());
        int destino = EncajeCarrusel.destino(java.util.Arrays.copyOf(bordes, visibles), getScrollX(), velocidad, maximo);
        smoothScrollTo(destino, getScrollY());
    }
}
