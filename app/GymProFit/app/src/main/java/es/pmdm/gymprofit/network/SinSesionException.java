package es.pmdm.gymprofit.network;

import java.io.IOException;

// ============================================================
// SinSesionException — una petición que necesita sesión, sin token ni refresh (GP-258).
//
// La lanza el AuthInterceptor antes de que la petición salga: la API diría 401, y ese 401
// no es una sesión que se acaba. Llega a ApiCallback.onFailure, que da onFail(401) sin
// avisar de nada: no había sesión que cerrar.
// ============================================================
final class SinSesionException extends IOException {

    SinSesionException(String ruta) {
        super("Sin sesión: " + ruta + " no sale");
    }
}
