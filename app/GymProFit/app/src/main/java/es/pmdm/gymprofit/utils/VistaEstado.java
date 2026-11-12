package es.pmdm.gymprofit.utils;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import es.pmdm.gymprofit.R;

// ============================================================
// VistaEstado — el bloque view_estado: cargando, error con reintentar o vacío (lote 1.2.1;
// GP-064 lo hace el único de la app y le pone icono, título y acción).
//
// El estado se enseña donde iría el contenido, no solo en un toast que se va: quien no
// ve su programa tiene el porqué y el botón justo ahí. Qué pieza se ve en cada estado lo
// decide EstadoContenido; aquí solo se aplica. El bloque es una región viva educada: TalkBack
// lee el estado al cambiar.
// ============================================================
public final class VistaEstado {

    private final View raiz;
    private final View progreso;
    private final ImageView icono;
    private final TextView titulo;
    private final TextView texto;
    private final View reintentar;
    private final TextView accion;

    /** @param raiz la vista incluida de view_estado. */
    public VistaEstado(View raiz) {
        this.raiz = raiz;
        this.progreso = raiz.findViewById(R.id.pbEstado);
        this.icono = raiz.findViewById(R.id.ivEstado);
        this.titulo = raiz.findViewById(R.id.tvEstadoTitulo);
        this.texto = raiz.findViewById(R.id.tvEstado);
        this.reintentar = raiz.findViewById(R.id.btnEstadoReintentar);
        this.accion = raiz.findViewById(R.id.btnEstadoAccion);
    }

    /** Cargando: solo la rueda. */
    public void cargando() {
        aplicar(EstadoContenido.cargando());
    }

    /** Un fallo, con su texto y el botón de reintentar. */
    public void error(CharSequence mensaje, Runnable alReintentar) {
        aplicar(EstadoContenido.error());
        texto.setText(mensaje);
        reintentar.setOnClickListener(v -> alReintentar.run());
    }

    /** Vacío: el icono de lo que falta y su texto. */
    public void vacio(@DrawableRes int icono, @StringRes int mensaje) {
        vacio(icono, raiz.getContext().getText(mensaje));
    }

    /** Vacío con el texto ya montado (p. ej. con una cifra dentro). */
    public void vacio(@DrawableRes int icono, CharSequence mensaje) {
        aplicar(EstadoContenido.vacio(icono, false, false));
        texto.setText(mensaje);
        accion.setOnClickListener(null);
    }

    /**
     * Vacío con título (0 si no lleva) y, si la pantalla tiene algo que proponer, su acción en
     * el botón relleno, que lleva a donde se llena (apuntar una medida, crear una rutina…):
     * {@code textoAccion} 0 o {@code alPulsar} nulo, sin botón.
     */
    public void vacio(@DrawableRes int icono, @StringRes int tituloRes, @StringRes int mensaje,
                      @StringRes int textoAccion, @Nullable Runnable alPulsar) {
        boolean conAccion = textoAccion != 0 && alPulsar != null;
        aplicar(EstadoContenido.vacio(icono, tituloRes != 0, conAccion));
        if (tituloRes != 0) titulo.setText(tituloRes);
        texto.setText(mensaje);
        if (conAccion) {
            accion.setText(textoAccion);
            accion.setOnClickListener(v -> alPulsar.run());
        } else {
            accion.setOnClickListener(null);
        }
    }

    /** Hay contenido: el bloque no se ve. */
    public void oculto() {
        aplicar(EstadoContenido.oculto());
    }

    private void aplicar(EstadoContenido e) {
        raiz.setVisibility(e.visible ? View.VISIBLE : View.GONE);
        if (!e.visible) return;
        progreso.setVisibility(e.progreso ? View.VISIBLE : View.GONE);
        icono.setVisibility(e.icono ? View.VISIBLE : View.GONE);
        if (e.icono) icono.setImageResource(e.iconoRes);
        titulo.setVisibility(e.titulo ? View.VISIBLE : View.GONE);
        texto.setVisibility(e.texto ? View.VISIBLE : View.GONE);
        reintentar.setVisibility(e.reintentar ? View.VISIBLE : View.GONE);
        accion.setVisibility(e.accion ? View.VISIBLE : View.GONE);
    }

    /** El mensaje de un fallo de red, el de siempre de la app, seguido del propio de la pantalla. */
    public static CharSequence mensaje(android.content.Context ctx, @StringRes int propio, int code,
                                       @Nullable String message) {
        return ctx.getString(propio) + " " + UiFeedback.mensaje(ctx, code, message);
    }
}
