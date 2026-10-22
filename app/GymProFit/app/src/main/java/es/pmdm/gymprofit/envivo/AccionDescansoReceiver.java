package es.pmdm.gymprofit.envivo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import es.pmdm.gymprofit.utils.PreferencesManager;

// ============================================================
// AccionDescansoReceiver — «+15 s» y «Saltar» de la notificación de la sesión (GP-210
// paso 3, momento 27).
//
// Recibe el toque del botón y se lo pasa a AccionDescanso, que cambia el descanso de la
// sesión como lo haría el panel de la pantalla. Puede llegar con el proceso muerto: la
// aplicación arranca, el repositorio lee la sesión del fichero de la cuenta y sigue desde
// ahí. El cambio se publica y la notificación se vuelve a pintar sola (la mueve el
// observador de GymProFitApp), con la alarma del fin ya movida.
//
// No se exporta: solo lo despiertan los botones de la propia notificación, que van con
// PendingIntent inmutable.
// ============================================================
public class AccionDescansoReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (intent == null) return;
        SesionEnCursoRepositorio repo = SesionEnCursoRepositorio.get(ctx);
        repo.usarCuenta(new PreferencesManager(ctx).getUsuarioId());
        AccionDescanso.aplicar(repo, intent.getAction());
    }
}
