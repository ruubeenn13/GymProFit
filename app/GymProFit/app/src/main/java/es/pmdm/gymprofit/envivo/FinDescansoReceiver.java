package es.pmdm.gymprofit.envivo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import es.pmdm.gymprofit.utils.PreferencesManager;

// ============================================================
// FinDescansoReceiver — la alarma del fin del descanso ha sonado (GP-013).
//
// Si la app está delante no hace nada: avisa el vigilante de la aplicación, sin
// notificación. Si no, quita el descanso de la sesión (al abrir la app no volverá a
// sonar) y publica «Descanso terminado · Hip thrust con barra, serie 3». Puede llegar con
// el proceso muerto: la aplicación arranca, lee la sesión del fichero y sigue desde ahí.
//
// No se exporta: solo lo despierta su propia alarma.
// ============================================================
public class FinDescansoReceiver extends BroadcastReceiver {

    /** La acción de la alarma (el PendingIntent de AlarmasDescanso). */
    public static final String ACCION = "es.pmdm.gymprofit.FIN_DESCANSO";

    @Override
    public void onReceive(Context ctx, Intent intent) {
        SesionEnCursoRepositorio repo = SesionEnCursoRepositorio.get(ctx);
        repo.usarCuenta(new PreferencesManager(ctx).getUsuarioId());
        if (intent == null || !ACCION.equals(intent.getAction()) || VigilanteDescanso.delante()) return;
        SesionEnCursoRepositorio.FinDescanso fin = repo.terminarPorAlarma();
        if (fin != null) NotificacionDescanso.publicar(ctx, fin);
    }
}
