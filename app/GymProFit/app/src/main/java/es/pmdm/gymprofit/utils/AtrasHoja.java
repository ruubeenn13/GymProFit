package es.pmdm.gymprofit.utils;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.view.View;

import androidx.activity.BackEventCompat;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;

import com.google.android.material.card.MaterialCardView;

import es.pmdm.gymprofit.R;

// ============================================================
// AtrasHoja — atrás predictivo en las hojas propias (GP-210 paso 2, DEC-039).
//
// Una hoja que no es un BottomSheetDialog (la del «+» de la barra, la del plato guardado) la
// cierra este callback. Mientras el dedo recorre el gesto, la hoja lo sigue: se encoge hasta el
// 90 % (Movimiento.ATRAS_ESCALA) con las esquinas a 28 dp si es una tarjeta. Al soltar:
//   · si se confirma, vuelve a su tamaño en 250 ms con la curva estándar (Movimiento.ATRAS_SUELTA) a
//     la vez que cerrar() la quita como siempre;
//   · si se cancela, vuelve a su sitio en esos mismos 250 ms.
// Con «Quitar animaciones» no hay seguimiento que ver: los animadores saltan a su final.
// ============================================================
public abstract class AtrasHoja extends OnBackPressedCallback {

    private final View hoja;
    private final float radioHoja;
    private final float radioAtras;
    private float progreso;

    /**
     * @param hoja la vista de la hoja; el pivote del encogimiento es el centro de su borde inferior.
     */
    protected AtrasHoja(@NonNull View hoja) {
        super(false);
        this.hoja = hoja;
        this.radioAtras = hoja.getResources().getDimension(R.dimen.hoja_radio_atras);
        this.radioHoja = hoja instanceof MaterialCardView ? ((MaterialCardView) hoja).getRadius() : radioAtras;
    }

    /** Cierra la hoja: lo mismo que haría su botón de cerrar. */
    protected abstract void cerrar();

    @Override
    public void handleOnBackStarted(@NonNull BackEventCompat evento) {
        hoja.animate().cancel();
        hoja.setPivotX(hoja.getWidth() / 2f);
        hoja.setPivotY(hoja.getHeight());
    }

    @Override
    public void handleOnBackProgressed(@NonNull BackEventCompat evento) {
        progreso = evento.getProgress();
        float escala = 1f - (1f - Movimiento.ATRAS_ESCALA) * progreso;
        hoja.setScaleX(escala);
        hoja.setScaleY(escala);
        if (hoja instanceof MaterialCardView) {
            ((MaterialCardView) hoja).setRadius(radioHoja + (radioAtras - radioHoja) * progreso);
        }
    }

    @Override
    public void handleOnBackPressed() {
        if (progreso > 0f) volverATamano();
        progreso = 0f;
        cerrar();
    }

    @Override
    public void handleOnBackCancelled() {
        if (progreso > 0f) volverATamano();
        progreso = 0f;
    }

    // De la escala del dedo a la normal, en ATRAS_SUELTA con la curva estándar. Si se cancela, la
    // hoja queda en su sitio; si se confirma, lo mueve cerrar() a la vez.
    private void volverATamano() {
        ObjectAnimator a = ObjectAnimator.ofPropertyValuesHolder(hoja,
                PropertyValuesHolder.ofFloat(View.SCALE_X, hoja.getScaleX(), 1f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, hoja.getScaleY(), 1f));
        a.setDuration(Movimiento.ATRAS_SUELTA);
        a.setInterpolator(Movimiento.ESTANDAR);
        a.start();
        if (hoja instanceof MaterialCardView) {
            MaterialCardView c = (MaterialCardView) hoja;
            android.animation.ValueAnimator r = android.animation.ValueAnimator.ofFloat(c.getRadius(), radioHoja);
            r.setDuration(Movimiento.ATRAS_SUELTA);
            r.setInterpolator(Movimiento.ESTANDAR);
            r.addUpdateListener(v -> c.setRadius((float) v.getAnimatedValue()));
            r.start();
        }
    }
}
