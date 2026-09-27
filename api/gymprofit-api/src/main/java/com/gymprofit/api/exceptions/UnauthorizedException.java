package com.gymprofit.api.exceptions;

// ============================================================
// UnauthorizedException — excepción de acceso no autorizado
// Se lanza cuando un usuario intenta acceder/modificar un recurso
// sobre el que no tiene permisos (p.ej. IDOR entre usuarios). Capturada
// por ControllerExceptionHandler para devolver 403.
// ============================================================
public class UnauthorizedException extends ExcepcionConClave {

    /**
     * @param clave clave del mensaje en messages*.properties (GP-109)
     * @param args  argumentos del mensaje
     */
    public UnauthorizedException(String clave, Object... args) {
        super(clave, args, null);
    }

    // Mensaje por defecto genérico.
    public UnauthorizedException() {
        this("error.acceso.sinPermiso");
    }
}
