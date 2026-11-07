package es.pmdm.gymprofit.ui.widget;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.utils.Movimiento;

// ============================================================
// HojaDuracion — la duración de «Apuntar entreno» (GP-241, punto 1): − y + de 5 en 5, de 5 a 300
// minutos, sin teclado. Mantener un botón repite, como los − y + de Tus datos. Cada toque se
// aplica al momento (el cuadro de la cabecera cambia detrás); «Listo» solo cierra.
// ============================================================
public final class HojaDuracion {

    // Mantener pulsado: espera antes de repetir y cada cuánto repite (los de Tus datos).
    private static final long REPETIR_ESPERA_MS = 400, REPETIR_CADA_MS = 90;

    private HojaDuracion() { }

    /**
     * Abre la hoja.
     *
     * @param actual   los minutos de ahora.
     * @param sumar    suma (o resta) minutos y deja la duración ya cambiada.
     * @param paso     cuánto suma cada toque.
     */
    public static void mostrar(@NonNull Activity act, @NonNull IntSupplier actual, @NonNull IntConsumer sumar,
                               int paso, int minimo, int maximo) {
        BottomSheetDialog hoja = new BottomSheetDialog(act);
        View raiz = LayoutInflater.from(act).inflate(R.layout.dialog_duracion, null, false);
        TextView minutos = raiz.findViewById(R.id.tvMinutosDuracion);
        View menos = raiz.findViewById(R.id.btnMenosDuracion), mas = raiz.findViewById(R.id.btnMasDuracion);
        Runnable pintar = () -> {
            int n = actual.getAsInt();
            minutos.setText(act.getString(R.string.sesiones_min, n));
            minutos.setContentDescription(act.getResources().getQuantityString(R.plurals.tarjeta_minutos, n, n));
            // En los límites el botón se apaga: pulsarlo no haría nada.
            activar(menos, n > minimo);
            activar(mas, n < maximo);
        };
        pintar.run();
        repetir(raiz.findViewById(R.id.btnMenosDuracion), () -> { sumar.accept(-paso); pintar.run(); });
        repetir(raiz.findViewById(R.id.btnMasDuracion), () -> { sumar.accept(paso); pintar.run(); });
        raiz.findViewById(R.id.btnListoDuracion).setOnClickListener(v -> hoja.dismiss());
        hoja.setContentView(raiz);
        hoja.show();
    }

    private static void activar(View boton, boolean activo) {
        boton.setEnabled(activo);
        boton.setAlpha(activo ? 1f : androidx.core.content.res.ResourcesCompat.getFloat(boton.getResources(), R.dimen.alfa_desactivado));
    }

    // Un toque suma una vez; mantenerlo repite. Con TalkBack el toque es un clic y suma una vez.
    @SuppressLint("ClickableViewAccessibility")
    private static void repetir(@NonNull View boton, @NonNull Runnable accion) {
        Handler repeticion = new Handler(Looper.getMainLooper());
        final Runnable[] bucle = new Runnable[1];
        final boolean[] repetido = {false};
        bucle[0] = () -> {
            repetido[0] = true;
            accion.run();
            repeticion.postDelayed(bucle[0], REPETIR_CADA_MS);
        };
        boton.setOnClickListener(v -> {
            if (repetido[0]) {
                repetido[0] = false;
                return;
            }
            Movimiento.vibrar(v, Movimiento.Vibracion.LIGERA);
            accion.run();
        });
        boton.setOnTouchListener((v, ev) -> {
            switch (ev.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    repetido[0] = false;
                    repeticion.postDelayed(bucle[0], REPETIR_ESPERA_MS);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    repeticion.removeCallbacks(bucle[0]);
                    break;
                default:
                    break;
            }
            return false;
        });
    }
}
