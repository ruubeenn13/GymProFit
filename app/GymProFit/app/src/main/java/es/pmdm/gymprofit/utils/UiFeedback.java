package es.pmdm.gymprofit.utils;

import android.content.Context;

import es.pmdm.gymprofit.R;

// ============================================================
// UiFeedback — mapeo centralizado de códigos de error de red a mensajes de usuario.
// Traduce el (code, message) que entrega ApiCallback.onFail a un toast con texto
// legible y localizado (nunca hardcodeado). Evita repetir el switch en cada Activity.
//   code == -1  → fallo de transporte (timeout/red caída) → mensaje de cold-start.
//   code == 401 → ya lo gestiona ApiCallback (notifyUnauthorized) → NO se toca aquí.
//   code == 404 → la API responde 404 en colecciones vacías / recurso inexistente:
//                 es un estado vacío benigno, NO un error de red → sin toast.
//   code >= 500 → error del servidor.
//   resto       → error genérico.
// ============================================================
public final class UiFeedback {

    private UiFeedback() {}

    // Muestra un toast de error apropiado según el código devuelto por la API.
    public static void toastError(Context context, int code, String message) {
        if (context == null) return;

        // 401: la sesión expirada ya dispara el logout global; no duplicar aviso.
        if (code == 401) return;

        // 404: en esta API significa "sin datos" (lista vacía) o recurso inexistente,
        // no un fallo de conexión → se trata como estado vacío, sin toast de error.
        if (code == 404) return;

        int res;
        if (code == -1) {
            // Sin respuesta HTTP: normalmente el servidor Render despertando (~60s).
            res = R.string.feedback_error_cold_start;
        } else if (code >= 500) {
            res = R.string.feedback_error_servidor;
        } else {
            res = R.string.feedback_error_generico;
        }

        UIHelper.mostrarToastError(context, context.getString(res));
    }
}
