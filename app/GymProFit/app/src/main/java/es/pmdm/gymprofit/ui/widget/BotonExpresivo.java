package es.pmdm.gymprofit.ui.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.button.MaterialButton;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.utils.Movimiento;

// ============================================================
// BotonExpresivo — el botón principal de Material 3 Expressive (GP-210 paso 2).
//
// Cambia de forma al pulsarlo: las esquinas pasan de 26 dp a 12 dp en 120 ms con la curva
// estándar (Movimiento.TOQUE) y vuelven al soltar. Es un MaterialButton normal: el radio de
// reposo lo da cornerRadius (Widget.GymProFit.Button.Expresivo, 26 dp) y el de pulsado el
// dimen boton_radio_pulsado. Lo usan «Empezar» y «Hecho». El «+» de la barra no: es cristal,
// y un botón de cristal no cambia de forma al pulsarlo (CristalVivo.hundirAlPulsar, GP-285).
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

    @Override
    protected void onDetachedFromWindow() {
        if (animador != null) animador.cancel();
        super.onDetachedFromWindow();
    }
}
