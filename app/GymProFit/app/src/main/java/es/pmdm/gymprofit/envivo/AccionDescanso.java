package es.pmdm.gymprofit.envivo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

// ============================================================
// AccionDescanso — «+15 s» y «Saltar» de la notificación de la sesión (GP-210 paso 3,
// momento 27).
//
// Los botones de la notificación llegan a AccionDescansoReceiver, que no hace más que
// pasar su acción aquí. Hacen exactamente lo que los botones del panel de la sesión
// (SesionEnCursoRepositorio.ajustarDescanso y saltarDescanso), así que el descanso, su
// alarma y la notificación quedan como si se hubieran tocado dentro de la app. Puede
// llegar con el proceso muerto: el repositorio lee la sesión del fichero y sigue desde ahí.
// ============================================================
public final class AccionDescanso {

    /** «+15 s» (el PendingIntent de la notificación). */
    public static final String ACCION_MAS = "es.pmdm.gymprofit.DESCANSO_MAS";
    /** «Saltar». */
    public static final String ACCION_SALTAR = "es.pmdm.gymprofit.DESCANSO_SALTAR";

    private AccionDescanso() { }

    /**
     * Hace lo que pide la acción.
     *
     * @param accion {@link #ACCION_MAS} o {@link #ACCION_SALTAR}; cualquier otra cosa se ignora.
     * @return si cambió el descanso de la sesión.
     */
    public static boolean aplicar(@NonNull SesionEnCursoRepositorio repo, @Nullable String accion) {
        if (ACCION_MAS.equals(accion)) return repo.ajustarDescanso(LogicaDescanso.PASO_SEGUNDOS);
        if (ACCION_SALTAR.equals(accion)) {
            if (!repo.editable() || repo.actual() == null || repo.actual().descanso == null) return false;
            repo.saltarDescanso();
            return true;
        }
        return false;
    }
}
