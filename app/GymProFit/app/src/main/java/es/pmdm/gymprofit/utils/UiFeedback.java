package es.pmdm.gymprofit.utils;

import android.content.Context;

import es.pmdm.gymprofit.R;
import es.pmdm.gymprofit.network.ApiCallback;

// ============================================================
// UiFeedback — mapeo centralizado de códigos de error de red a mensajes de usuario.
// Traduce el (code, message) que entrega ApiCallback.onFail a un toast con texto
// legible y localizado (nunca hardcodeado). Evita repetir el switch en cada Activity.
//   code == -1  → fallo de transporte (timeout/red caída) → mensaje de cold-start.
//   code == 401 → ya lo gestiona ApiCallback (notifyUnauthorized) → NO se toca aquí.
//   code == 404 → el recurso PADRE no existe → error real, con su propio mensaje.
//                 Hasta GP-069 la API respondía 404 también a una colección vacía, y
//                 por eso aquí se silenciaba; ahora una lista vacía es 200 con [] y
//                 callarse el 404 escondría el error (DEC-033).
//   code == 429 → demasiados intentos (GP-096): pide esperar, y cuánto si la API
//                 manda Retry-After. Antes salía el genérico «inténtalo de nuevo»,
//                 que invita justo a reintentar enseguida.
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

        UIHelper.mostrarToastError(context, mensaje(context, code, message));
    }

    /**
     * Como {@link #mensaje(Context, int)}, pero con el texto de onFail, que en un 429
     * lleva la espera que pide la API.
     */
    public static String mensaje(Context context, int code, String message) {
        if (code == 429) return demasiadosIntentos(context, segundosDeEspera(message));
        return mensaje(context, code);
    }

    /**
     * Los segundos de Retry-After que ApiCallback deja al principio del mensaje de un
     * 429, o null si no vienen (la API no lo mandó, o es una fecha y no un número).
     */
    public static Integer segundosDeEspera(String message) {
        if (message == null || !message.startsWith(ApiCallback.PREFIJO_RETRY_AFTER)) return null;
        int fin = message.indexOf(';');
        if (fin < 0) return null;
        try {
            int s = Integer.parseInt(message.substring(ApiCallback.PREFIJO_RETRY_AFTER.length(), fin).trim());
            return s > 0 ? s : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Minutos que hay que decir para una espera: null si se dice en segundos (hasta 90),
     * o los minutos redondeados hacia arriba, para no invitar a volver antes de tiempo.
     */
    public static Integer minutosDeEspera(int segundos) {
        return segundos <= 90 ? null : (segundos + 59) / 60;
    }

    // Texto del 429: cuánto esperar si se sabe, «un minuto» si no.
    private static String demasiadosIntentos(Context context, Integer segundos) {
        if (segundos == null) return context.getString(R.string.feedback_error_demasiados);
        Integer minutos = minutosDeEspera(segundos);
        if (minutos == null) {
            return context.getResources().getQuantityString(
                    R.plurals.feedback_error_demasiados_segundos, segundos, segundos);
        }
        return context.getResources().getQuantityString(
                R.plurals.feedback_error_demasiados_minutos, minutos, minutos);
    }

    /**
     * Devuelve el mensaje que le corresponde a un código, sin enseñarlo.
     *
     * <p>Existe porque no todo fallo cabe en un toast: el guardado de una sesión
     * (GP-006) necesita el mismo texto dentro de un diálogo que además ofrece
     * reintentar. El mapeo vive en un solo sitio para que las dos formas de
     * enseñarlo no se desincronicen.
     *
     * @param code código entregado por ApiCallback.onFail.
     * @return texto localizado, listo para mostrar.
     */
    public static String mensaje(Context context, int code) {
        int res;
        if (code == 404) {
            // El recurso que se pide no existe. Ya NO significa "lista vacía": desde
            // GP-069 eso es un 200 con [] y no pasa por aquí.
            res = R.string.feedback_error_no_encontrado;
        } else if (code == -1) {
            // Sin respuesta HTTP: normalmente el servidor Render despertando (~60s).
            res = R.string.feedback_error_cold_start;
        } else if (code == 429) {
            res = R.string.feedback_error_demasiados;
        } else if (code >= 500) {
            res = R.string.feedback_error_servidor;
        } else {
            res = R.string.feedback_error_generico;
        }

        return context.getString(res);
    }
}
