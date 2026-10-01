package es.pmdm.gymprofit.utils;

import androidx.annotation.NonNull;

// ============================================================
// PermisoAvisos — en qué punto está el permiso de notificaciones (GP-151, lote 1.5.2).
//
// «¿Te avisamos?» y Ajustes › Notificaciones hacen con su botón una de tres cosas:
// - CONCEDIDO: nada que pedir; el botón dice «Guardar» y solo guarda.
// - PEDIBLE: se pide a Android, que enseña su diálogo.
// - BLOQUEADO: Android ya no enseña el diálogo (se negó dos veces, o se apagaron en los
//   ajustes antes de Android 13); el botón lleva a los ajustes de notificaciones de la app.
//
// Android no dice si el permiso está bloqueado: solo si «conviene explicarlo», que es
// verdad tras la primera negativa y mentira tanto antes de pedirlo nunca como después de
// la segunda. Para separar esos dos se apunta en el móvil si se pidió alguna vez
// (PreferencesManager). La 1.4.0 lo pedía sin apuntarlo: quien la negó dos veces allí
// llega sin marca y se le «pide»; Android contesta al instante sin enseñar nada, y eso
// es lo que reconoce sinDialogo(), para mandarlo a los ajustes en vez de dejarle sin nada.
// Sin Android, para probarlo en la JVM.
// ============================================================
public final class PermisoAvisos {

    private PermisoAvisos() {}

    /** Android 13 (API 33), el primero en que las notificaciones piden permiso. */
    public static final int SDK_CON_PERMISO = 33;

    /**
     * Por debajo de esto, la negativa llegó sin que nadie viera el diálogo. Un toque en
     * «No permitir» lleva bastante más: hay que leer el diálogo y llegar al botón.
     */
    public static final long MS_SIN_DIALOGO = 400;

    public enum Estado { CONCEDIDO, PEDIBLE, BLOQUEADO }

    /**
     * @param sdk           {@code Build.VERSION.SDK_INT}.
     * @param concedido     si las notificaciones de la app están permitidas.
     * @param explicar      {@code shouldShowRequestPermissionRationale}.
     * @param pedidoAntes   si esta instalación lo pidió alguna vez.
     */
    @NonNull
    public static Estado estado(int sdk, boolean concedido, boolean explicar, boolean pedidoAntes) {
        if (concedido) return Estado.CONCEDIDO;
        if (sdk < SDK_CON_PERMISO) return Estado.BLOQUEADO;
        if (explicar || !pedidoAntes) return Estado.PEDIBLE;
        return Estado.BLOQUEADO;
    }

    /**
     * Si la respuesta a una petición llegó sin que Android enseñara el diálogo: negada,
     * sin explicación posible y casi al instante.
     */
    public static boolean sinDialogo(boolean concedido, boolean explicar, long ms) {
        return !concedido && !explicar && ms < MS_SIN_DIALOGO;
    }

    /** «¿Te avisamos?» sale una vez por cuenta y móvil. */
    public static boolean tocaPreguntar(boolean yaPreguntado) {
        return !yaPreguntado;
    }
}
