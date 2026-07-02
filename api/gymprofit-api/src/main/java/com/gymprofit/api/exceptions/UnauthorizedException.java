package com.gymprofit.api.exceptions;

// ============================================================
// UnauthorizedException — excepción de acceso no autorizado
// Se lanza cuando un usuario intenta acceder/modificar un recurso
// sobre el que no tiene permisos (p.ej. IDOR entre usuarios). Capturada
// por ControllerExceptionHandler para devolver 403.
// ============================================================
public class UnauthorizedException extends RuntimeException {

    // Constructor con mensaje personalizado.
    public UnauthorizedException(String message) {
        super(message);
    }

    // Constructor con mensaje por defecto genérico.
    public UnauthorizedException() {
        super("No tienes permisos para realizar esta acción");
    }
}