package es.pmdm.gymprofit.ui.widget;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.button.MaterialButton;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.ui.cristal.CristalVivo;
import es.pmdm.gymprofit.utils.Movimiento;

// ============================================================
// BotonExpresivo — el botón principal de Material 3 Expressive (GP-210 paso 2).
//
// Cambia de forma al pulsarlo: las esquinas pasan de 26 dp a 12 dp en 120 ms con la curva
// estándar (Movimiento.TOQUE) y vuelven al soltar. Es un MaterialButton normal: el radio de
// reposo lo da cornerRadius (Widget.GymProFit.Button.Expresivo, 26 dp) y el de pulsado el
// dimen boton_radio_pulsado. Lo usan «Empezar», «Hecho» y el «+».
//
// Material 1.14 trae su propio cambio de forma, pero lo mueve con un resorte que no da los
// 120 ms de DEC-039; aquí el tiempo es el de Movimiento y no hay nada nuevo que añadir.
// Con «Quitar animaciones» el animador salta a su final: la forma cambia sin moverse.
// ============================================================
public class BotonExpresivo extends MaterialButton {

    private final int radioPulsado;
    private ValueAnimator animador;

    public BotonExpresivo(@NonNull Context context) {
        this(context, null);
    }

    public BotonExpresivo(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, com.google.android.material.R.attr.materialButtonStyle);
    }

    public BotonExpresivo(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        radioPulsado = getResources().getDimensionPixelSize(R.dimen.boton_radio_pulsado);
    }

    @Override
    public void setPressed(boolean pulsado) {
        boolean cambia = pulsado != isPressed();
        super.setPressed(pulsado);
        if (cambia) moverForma(pulsado ? radioPulsado : getResources().getDimensionPixelSize(R.dimen.boton_radio_reposo));
    }

    // De la esquina de ahora a la de destino, en TOQUE ms. Si ya iba hacia otra, parte de donde estaba.
    private void moverForma(int hacia) {
        if (animador != null) animador.cancel();
        animador = ValueAnimator.ofInt(getCornerRadius(), hacia);
        animador.setDuration(Movimiento.TOQUE);
        animador.setInterpolator(Movimiento.ESTANDAR);
        animador.addUpdateListener(a -> setCornerRadius((int) a.getAnimatedValue()));
        animador.start();
    }

    /**
     * Lo mismo para un botón que no es un MaterialButton sino una pieza de cristal vivo (el «+»
     * de la barra): al tocarlo, sus esquinas pasan de las que tiene en reposo (en el «+», 32 dp:
     * un círculo; GP-285) a boton_radio_pulsado en TOQUE ms, y vuelven al soltar. No consume el
     * toque: el clic sigue su camino (por eso no hace falta performClick aquí).
     */
    @SuppressLint("ClickableViewAccessibility")
    public static void alPulsar(@NonNull CristalVivo boton) {
        float pulsado = boton.getResources().getDimension(R.dimen.boton_radio_pulsado);
        ValueAnimator[] animador = new ValueAnimator[1];
        // Las esquinas de reposo, tal como están la primera vez que se toca (ya con su medida).
        float[] reposo = {-1f};
        boton.setOnTouchListener((v, e) -> {
            int accion = e.getActionMasked();
            boolean baja = accion == MotionEvent.ACTION_DOWN;
            if (baja || accion == MotionEvent.ACTION_UP || accion == MotionEvent.ACTION_CANCEL) {
                if (reposo[0] < 0) reposo[0] = boton.radioEsquinas();
                if (animador[0] != null) animador[0].cancel();
                animador[0] = ValueAnimator.ofFloat(boton.radioEsquinas(), baja ? pulsado : reposo[0]);
                animador[0].setDuration(Movimiento.TOQUE);
                animador[0].setInterpolator(Movimiento.ESTANDAR);
                animador[0].addUpdateListener(a -> boton.ponerRadioEsquinas((float) a.getAnimatedValue()));
                animador[0].start();
            }
            return false;
        });
    }

    @Override
    protected void onDetachedFromWindow() {
        if (animador != null) animador.cancel();
        super.onDetachedFromWindow();
    }
}
