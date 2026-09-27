package com.gymprofit.api.exceptions;

// ============================================================
// ExternalServiceException — fallo al consultar una API externa
// Se lanza cuando Open Food Facts o wger no responden o devuelven un
// error. El ControllerExceptionHandler la mapea a 502 (Bad Gateway)
// para distinguir el fallo del proveedor de un error propio de la API.
// ============================================================
public class ExternalServiceException extends ExcepcionConClave {

    /**
     * @param cause fallo del servicio externo; va al log, nunca al cliente.
     * @param clave clave del mensaje en messages*.properties (GP-109)
     * @param args  argumentos del mensaje
     */
    public ExternalServiceException(Throwable cause, String clave, Object... args) {
        super(clave, args, cause);
    }
}
