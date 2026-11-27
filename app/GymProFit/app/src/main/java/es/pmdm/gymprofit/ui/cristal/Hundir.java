package es.pmdm.gymprofit.ui.cristal;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import es.pmdm.gymprofit.utils.Movimiento;

// ============================================================
// Hundir — cómo responde al dedo un botón de Liquid Glass (GP-285, DEC-070).
//
// Un botón de Liquid Glass no cambia de forma al pulsarlo: se hunde a HUNDIDO y, si es de marca
// (su fondo es un Cristal MARCA: el principal y el «+»), se le enciende el brillo de arriba y la
// media luna (Cristal.ponerLuz). En TOQUE ms con la curva estándar, el «se hunde al tocar» del
// momento 1 de DEC-039; al soltar vuelve igual, desde donde esté. Con «Quitar animaciones» no se
// hunde ni se mueve: solo cambia la luz, sin transición.
//
// Va con el toque y no con setPressed: dentro de algo que se desplaza (Inicio) Android retrasa
// el estado pulsado, y el botón tardaba en responder. No consume el toque: el clic, la
// vibración y la accesibilidad siguen su camino. Es la única fuente de este movimiento: la usan
// BotonMarca, BotonCristal y el «+» de la barra.
// ============================================================
public final class Hundir {

    /** La escala de un botón pulsado del todo. */
    public static final float HUNDIDO = 0.94f;

    private Hundir() {}

    /** Hace que la vista se hunda al tocarla y vuelva al soltarla. */
    @SuppressLint("ClickableViewAccessibility")
    public static void alPulsar(@NonNull View vista) {
        Estado estado = new Estado(vista);
        vista.setOnTouchListener((v, e) -> {
            int accion = e.getActionMasked();
            boolean baja = accion == MotionEvent.ACTION_DOWN;
            if (baja || accion == MotionEvent.ACTION_UP || accion == MotionEvent.ACTION_CANCEL) {
                estado.mover(baja ? 1f : 0f);
            }
            return false;
        });
        vista.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(@NonNull View v) {
                // Nada: empieza en reposo.
            }

            @Override
            public void onViewDetachedFromWindow(@NonNull View v) {
                estado.parar();
            }
        });
    }

    // Lo pulsada que está una vista (0 a 1) y lo que la mueve.
    private static final class Estado {
        private final View vista;
        private float pulsado;
        @Nullable private ValueAnimator animador;

        Estado(@NonNull View vista) {
            this.vista = vista;
        }

        void mover(float hacia) {
            parar();
            if (Movimiento.quieto(vista.getContext())) {
                poner(hacia, true);
                return;
            }
            animador = ValueAnimator.ofFloat(pulsado, hacia);
            animador.setDuration(Movimiento.TOQUE);
            animador.setInterpolator(Movimiento.ESTANDAR);
            animador.addUpdateListener(a -> poner((float) a.getAnimatedValue(), false));
            animador.start();
        }

        void parar() {
            if (animador != null) animador.cancel();
            animador = null;
        }

        // La escala y la luz con la misma fracción; quieto, sin escala.
        private void poner(float fraccion, boolean quieto) {
            pulsado = fraccion;
            float escala = quieto ? 1f : 1f - (1f - HUNDIDO) * fraccion;
            vista.setScaleX(escala);
            vista.setScaleY(escala);
            Drawable fondo = vista.getBackground();
            if (fondo instanceof Cristal) ((Cristal) fondo).ponerLuz(fraccion);
        }
    }
}
