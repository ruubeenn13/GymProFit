package es.pmdm.gymprofit.ui.tarjetas;

import android.animation.ValueAnimator;
import android.widget.TextView;

import androidx.annotation.DimenRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashSet;
import java.util.Set;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.ui.widget.BarrasVecesView;
import es.pmdm.gymprofit.utils.Movimiento;

// ============================================================
// EntradaTarjeta — el movimiento de una tarjeta de sesión o de rutina, una sola vez (GP-232).
//
// Al entrar la tarjeta: los kilos cuentan hasta su cifra (Movimiento.contar en CIFRAS, el momento
// 7) y las barras crecen una tras otra con la dorada brillando al final (BarrasVecesView.entrar).
// Cada pantalla tiene una y recuerda qué tarjetas ya entraron: al desplazar, una lista recicla
// sus filas y volverían a contar; así no se repite. Con «Quitar animaciones», todo en su sitio.
// ============================================================
public final class EntradaTarjeta {

    private final Set<String> vistas = new HashSet<>();

    /**
     * Pone los kilos y las barras; si la tarjeta no ha entrado nunca, con movimiento.
     *
     * @param clave    lo que identifica la tarjeta en la pantalla («s:412», «r:88»).
     * @param kilos    la vista de la cifra; null si la tarjeta no la enseña.
     * @param barras   las barras, ya con sus veces puestas; null si no lleva.
     */
    public void pintar(@NonNull String clave, @Nullable TextView kilos, double valor, @DimenRes int tamanoKg,
                       @NonNull TextosTarjeta textos, @Nullable BarrasVecesView barras) {
        if (kilos != null) {
            parar(kilos);
            kilos.setText(textos.kilos(valor, tamanoKg));
        }
        boolean primera = vistas.add(clave);
        if (!primera || Movimiento.quieto((kilos != null ? kilos : barras).getContext())) return;
        if (barras != null) barras.entrar();
        if (kilos != null && valor > 0) {
            ValueAnimator a = Movimiento.contar(0, Movimiento.CIFRAS,
                    f -> kilos.setText(textos.kilos(valor * f, tamanoKg)));
            kilos.setTag(R.id.tag_cuenta_kilos, a);
        }
    }

    /** Olvida lo que entró: al recargar la pantalla entera, vuelve a entrar. */
    public void olvidar() {
        vistas.clear();
    }

    // Una cuenta que seguía de una tarjeta reciclada no pisa la cifra nueva.
    private static void parar(@NonNull TextView kilos) {
        Object a = kilos.getTag(R.id.tag_cuenta_kilos);
        if (a instanceof ValueAnimator) ((ValueAnimator) a).cancel();
        kilos.setTag(R.id.tag_cuenta_kilos, null);
    }
}
