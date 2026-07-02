package com.gymprofit.api.exceptions;

// ============================================================
// NotFoundEntityException — excepción de entidad no encontrada
// Se lanza cuando una búsqueda por id (o similar) no devuelve resultados.
// Capturada por ControllerExceptionHandler para devolver 404.
// ============================================================
public class NotFoundEntityException extends RuntimeException {

    // Constructor con mensaje personalizado.
    public NotFoundEntityException(String message) {
        super(message);
    }

    // Constructor con mensaje y causa original de la excepción.
    public NotFoundEntityException(String message, Throwable cause) {
        super(message, cause);
    }
}
