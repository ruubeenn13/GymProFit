package es.pmdm.gymprofit.utils;

import androidx.annotation.DrawableRes;

import es.pmdm.gymprofit.R;

// ============================================================
// EstadoContenido — qué piezas de view_estado se ven en cada estado (GP-064).
//
// Sin vistas, para poder probarlo en la JVM; VistaEstado lo aplica. La carga es solo la
// rueda. El error lleva siempre su icono (la nube tachada), su texto y «Reintentar», un botón
// de contorno. El vacío lleva el icono de lo que falta, su texto y, si la pantalla lo tiene,
// un título y su acción en un botón relleno: el otro botón, no el de reintentar. Un vacío
// nunca toma el icono del error ni su botón: se confundirían.
// ============================================================
public final class EstadoContenido {

    /** Icono del error: el mismo en toda la app. */
    @DrawableRes public static final int ICONO_ERROR = R.drawable.ic_ms_cloud_off;
    /** Icono que toma un vacío que pidiera el del error. */
    @DrawableRes static final int ICONO_VACIO_NEUTRO = R.drawable.ic_ms_info;

    public final boolean visible;
    public final boolean progreso;
    public final boolean icono;
    public final boolean titulo;
    public final boolean texto;
    /** «Reintentar», de contorno: solo en el error. */
    public final boolean reintentar;
    /** La acción del vacío, rellena: solo en un vacío que tiene algo que proponer. */
    public final boolean accion;
    @DrawableRes public final int iconoRes;

    private EstadoContenido(boolean visible, boolean progreso, boolean icono, boolean titulo,
                            boolean texto, boolean reintentar, boolean accion, int iconoRes) {
        this.visible = visible;
        this.progreso = progreso;
        this.icono = icono;
        this.titulo = titulo;
        this.texto = texto;
        this.reintentar = reintentar;
        this.accion = accion;
        this.iconoRes = iconoRes;
    }

    /** Cargando: solo la rueda. */
    public static EstadoContenido cargando() {
        return new EstadoContenido(true, true, false, false, false, false, false, 0);
    }

    /** Un fallo: icono, texto y «Reintentar». */
    public static EstadoContenido error() {
        return new EstadoContenido(true, false, true, false, true, true, false, ICONO_ERROR);
    }

    /** Vacío: icono y texto; título y acción si la pantalla los tiene. */
    public static EstadoContenido vacio(@DrawableRes int icono, boolean conTitulo, boolean conAccion) {
        int res = icono == ICONO_ERROR ? ICONO_VACIO_NEUTRO : icono;
        return new EstadoContenido(true, false, true, conTitulo, true, false, conAccion, res);
    }

    /** Hay contenido: el bloque no se ve. */
    public static EstadoContenido oculto() {
        return new EstadoContenido(false, false, false, false, false, false, false, 0);
    }
}
