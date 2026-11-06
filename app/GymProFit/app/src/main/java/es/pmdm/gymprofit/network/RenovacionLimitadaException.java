package es.pmdm.gymprofit.network;

import java.io.IOException;

// ============================================================
// RenovacionLimitadaException — /auth/refresh contestó 429 a mitad de uso, o sigue la
// espera que pidió (GP-237, DEC-059).
//
// La lanza el TokenAuthenticator para que la petición acabe en ApiCallback.onFailure
// con la espera, y no como un sin red: ApiCallback la entrega como el 429 de GP-096
// (onFail(429) con «Retry-After=N;»), y UiFeedback dice cuánto esperar. La sesión sigue.
// ============================================================
final class RenovacionLimitadaException extends IOException {

    /** Lo que falta de espera, en segundos. */
    final int segundosDeEspera;

    RenovacionLimitadaException(int segundosDeEspera) {
        super("Demasiados intentos al renovar la sesión");
        this.segundosDeEspera = segundosDeEspera;
    }
}
