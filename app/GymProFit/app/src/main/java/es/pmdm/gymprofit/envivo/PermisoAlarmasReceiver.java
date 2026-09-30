package es.pmdm.gymprofit.envivo;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import es.pmdm.gymprofit.utils.PreferencesManager;

// ============================================================
// PermisoAlarmasReceiver — el permiso de alarmas exactas ha cambiado (GP-013).
//
// Con un descanso en marcha, su alarma se vuelve a poner del tipo que toca: exacta si
// el permiso acaba de llegar. Se exporta porque lo manda el sistema; la acción está
// protegida, así que ninguna otra app puede mandarla.
// ============================================================
public class PermisoAlarmasReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (intent == null
                || !AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED.equals(intent.getAction())) {
            return;
        }
        SesionEnCursoRepositorio repo = SesionEnCursoRepositorio.get(ctx);
        repo.usarCuenta(new PreferencesManager(ctx).getUsuarioId());
        repo.revisarPermisoAlarma();
    }
}
