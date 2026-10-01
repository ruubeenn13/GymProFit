package es.pmdm.gymprofit.ui.widget;

import android.view.View;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

import com.google.android.material.materialswitch.MaterialSwitch;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.utils.Movimiento;

// ============================================================
// FilaAviso — una fila de view_fila_aviso con su interruptor (GP-112).
//
// La fila entera cambia el interruptor, con vibración ligera, y TalkBack la lee como un
// interruptor: «Recordarme entrenar, si pasan 3 días sin entrenar, activado». La usan el
// alta («¿Te avisamos?») y Ajustes › Notificaciones, para que digan lo mismo.
// ============================================================
public final class FilaAviso {

    /** Se avisa al cambiarla con el dedo, con el estado nuevo. */
    public interface AlCambiar { void cambiada(boolean activa); }

    private final View fila;
    @Nullable private final MaterialSwitch interruptor;
    private boolean activa;

    /**
     * Prepara una fila.
     *
     * @param conInterruptor false para la del fin del descanso, que solo informa.
     */
    public FilaAviso(@NonNull View fila, @DrawableRes int icono, @StringRes int titulo, @StringRes int sub,
                     boolean conInterruptor) {
        this.fila = fila;
        ((ImageView) fila.findViewById(R.id.ivIconoAviso)).setImageResource(icono);
        TextView t = fila.findViewById(R.id.tvTituloAviso);
        TextView s = fila.findViewById(R.id.tvSubAviso);
        t.setText(titulo);
        s.setText(sub);
        MaterialSwitch sw = fila.findViewById(R.id.swAviso);
        String texto = fila.getContext().getString(R.string.ajustes_fila_sub_a11y, t.getText(), s.getText());
        fila.setContentDescription(texto);
        if (!conInterruptor) {
            sw.setVisibility(View.GONE);
            interruptor = null;
            fila.setBackground(null);
            fila.setFocusable(true);
            return;
        }
        interruptor = sw;
        fila.setClickable(true);
        fila.setFocusable(true);
        ViewCompat.setAccessibilityDelegate(fila, new AccessibilityDelegateCompat() {
            @Override
            public void onInitializeAccessibilityNodeInfo(@NonNull View host,
                                                          @NonNull AccessibilityNodeInfoCompat info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setClassName(Switch.class.getName());
                info.setCheckable(true);
                info.setChecked(activa);
            }
        });
    }

    /** Lo que se hace al cambiarla con el dedo. */
    public void alCambiar(@NonNull AlCambiar alCambiar) {
        fila.setOnClickListener(v -> {
            setActiva(!activa);
            Movimiento.vibrar(v, Movimiento.Vibracion.LIGERA);
            alCambiar.cambiada(activa);
        });
    }

    public boolean isActiva() {
        return activa;
    }

    public void setActiva(boolean activa) {
        this.activa = activa;
        if (interruptor != null) interruptor.setChecked(activa);
        fila.sendAccessibilityEvent(android.view.accessibility.AccessibilityEvent.TYPE_VIEW_SELECTED);
    }

    public void setHabilitada(boolean habilitada) {
        fila.setEnabled(habilitada);
        if (interruptor != null) interruptor.setEnabled(habilitada);
    }
}
