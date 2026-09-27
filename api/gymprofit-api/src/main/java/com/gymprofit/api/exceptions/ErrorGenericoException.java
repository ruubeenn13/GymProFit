package com.gymprofit.api.exceptions;

// ============================================================
// ErrorGenericoException — excepción genérica de negocio no cubierta
// por las excepciones custom más específicas. La captura
// ControllerExceptionHandler devolviendo 500 sin exponer la causa.
// ============================================================
public class ErrorGenericoException extends ExcepcionConClave {

    /**
     * @param clave clave del mensaje en messages*.properties (GP-109)
     * @param args  argumentos del mensaje
     */
    public ErrorGenericoException(String clave, Object... args) {
        super(clave, args, null);
    }
}
