package com.gymprofit.api.exceptions;

// ============================================================
// ExternalServiceException — fallo al consultar una API externa
// Se lanza cuando Open Food Facts o wger no responden o devuelven un
// error. El ControllerExceptionHandler la mapea a 502 (Bad Gateway)
// para distinguir el fallo del proveedor de un error propio de la API.
// ============================================================
public class ExternalServiceException extends RuntimeException {

    public ExternalServiceException(String message) {
        super(message);
    }

    public ExternalServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
