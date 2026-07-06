package es.pmdm.gymprofit.network;

import android.app.Activity;

import java.lang.ref.WeakReference;

import es.pmdm.gymprofit.utils.LoadingDialog;
import es.pmdm.gymprofit.utils.UiFeedback;

// ============================================================
// UiApiCallback<T> — ApiCallback con feedback de UI estándar (spinner + toast).
// Ahorra repetir en cada pantalla el mostrar/ocultar del LoadingDialog y el toast
// de error. La pantalla solo implementa onData(T) (el antiguo onOk); el resto es
// automático:
//   - Constructor (opcional autoLoading): muestra el spinner justo antes del enqueue.
//   - onOk  → oculta el spinner y delega en onData(body).
//   - onFail→ oculta el spinner y muestra el toast mapeado por UiFeedback.
// Las pantallas que ya gestionan el error a mano siguen usando ApiCallback directo.
// La Activity se guarda en WeakReference para no fugarla si el callback sobrevive.
// ============================================================
public abstract class UiApiCallback<T> extends ApiCallback<T> {

    private final WeakReference<Activity> activityRef;
    private final boolean autoLoading;

    // Muestra spinner automáticamente durante la petición.
    public UiApiCallback(Activity activity) {
        this(activity, true);
    }

    // autoLoading=false: no muestra spinner (solo aporta el toast de error automático).
    public UiApiCallback(Activity activity, boolean autoLoading) {
        this.activityRef = new WeakReference<>(activity);
        this.autoLoading = autoLoading;
        if (autoLoading && activity != null) {
            LoadingDialog.show(activity);
        }
    }

    // Éxito (2xx): la pantalla implementa aquí su lógica; el body puede ser null en 204.
    public abstract void onData(T body);

    // Oculta el spinner y delega en onData. No sobrescribir: usar onData.
    @Override
    public final void onOk(T body) {
        ocultarCarga();
        onData(body);
    }

    // Oculta el spinner y muestra el toast de error estándar. Las subclases pueden
    // sobrescribir para añadir comportamiento (p.ej. poner "—" en las vistas) llamando
    // a super.onFail(code, message) para conservar el spinner-hide + toast.
    @Override
    public void onFail(int code, String message) {
        ocultarCarga();
        Activity activity = activityRef.get();
        if (activity != null) {
            UiFeedback.toastError(activity, code, message);
        }
    }

    // Oculta el spinner si estaba activo y la Activity sigue viva.
    private void ocultarCarga() {
        if (!autoLoading) return;
        Activity activity = activityRef.get();
        if (activity != null) {
            LoadingDialog.hide(activity);
        }
    }
}
