package com.gymprofit.api.exceptions;

// ============================================================
// ErrorGenericoException — excepción genérica de negocio no cubierta
// por las excepciones custom más específicas. La captura
// ControllerExceptionHandler devolviendo 500 sin exponer la causa.
// ============================================================
public class ErrorGenericoException extends RuntimeException {

    // Constructor con mensaje directo.
    public ErrorGenericoException(String message) {
        super(message);
    }

    // Constructor con mensaje y causa original.
    public ErrorGenericoException(String message, Throwable cause) {
        super(message, cause);
    }
}