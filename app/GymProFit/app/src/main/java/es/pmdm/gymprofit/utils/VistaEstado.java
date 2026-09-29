package es.pmdm.gymprofit.utils;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import es.pmdm.gymprofit.R;

// ============================================================
// VistaEstado — el bloque view_estado: cargando, error con reintentar o vacío (lote 1.2.1).
//
// El estado se enseña donde iría el contenido, no solo en un toast que se va: quien no
// ve su programa tiene el porqué y el botón justo ahí. El texto se anuncia a TalkBack al
// cambiar (región viva educada).
// ============================================================
public final class VistaEstado {

    private final View raiz;
    private final View progreso;
    private final TextView texto;
    private final View reintentar;

    /** @param raiz la vista incluida de view_estado. */
    public VistaEstado(View raiz) {
        this.raiz = raiz;
        this.progreso = raiz.findViewById(R.id.pbEstado);
        this.texto = raiz.findViewById(R.id.tvEstado);
        this.reintentar = raiz.findViewById(R.id.btnEstadoReintentar);
        texto.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
    }

    /** Cargando: solo la rueda. */
    public void cargando() {
        raiz.setVisibility(View.VISIBLE);
        progreso.setVisibility(View.VISIBLE);
        texto.setVisibility(View.GONE);
        reintentar.setVisibility(View.GONE);
    }

    /** Un fallo, con su texto y el botón de reintentar. */
    public void error(CharSequence mensaje, Runnable alReintentar) {
        raiz.setVisibility(View.VISIBLE);
        progreso.setVisibility(View.GONE);
        texto.setVisibility(View.VISIBLE);
        texto.setText(mensaje);
        reintentar.setVisibility(View.VISIBLE);
        reintentar.setOnClickListener(v -> alReintentar.run());
    }

    /** Vacío: solo el texto. */
    public void vacio(@StringRes int mensaje) {
        raiz.setVisibility(View.VISIBLE);
        progreso.setVisibility(View.GONE);
        texto.setVisibility(View.VISIBLE);
        texto.setText(mensaje);
        reintentar.setVisibility(View.GONE);
    }

    /** Hay contenido: el bloque no se ve. */
    public void oculto() {
        raiz.setVisibility(View.GONE);
    }

    /** El mensaje de un fallo de red, el de siempre de la app, seguido del propio de la pantalla. */
    public static CharSequence mensaje(android.content.Context ctx, @StringRes int propio, int code,
                                       @Nullable String message) {
        return ctx.getString(propio) + " " + UiFeedback.mensaje(ctx, code, message);
    }
}
