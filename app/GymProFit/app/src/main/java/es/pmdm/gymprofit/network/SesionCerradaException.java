package es.pmdm.gymprofit.network;

import java.io.IOException;

// ============================================================
// SesionCerradaException — /auth/refresh contestó 401 a mitad de uso (DEC-059).
//
// La lanza el TokenAuthenticator para que la petición acabe en ApiCallback.onFailure
// con el motivo, y no en un 401 cualquiera: así el aviso de cuenta desactivada sale
// también cuando lo dice /auth/refresh y no solo la petición original (GP-083).
// ApiCallback la trata como el 401 de siempre: borra la sesión y lleva a Entrar una vez.
// ============================================================
final class SesionCerradaException extends IOException {

    /** La API cerró la sesión porque la cuenta está desactivada. */
    final boolean cuentaDesactivada;

    SesionCerradaException(boolean cuentaDesactivada) {
        super("Sesión cerrada por /auth/refresh");
        this.cuentaDesactivada = cuentaDesactivada;
    }
}
