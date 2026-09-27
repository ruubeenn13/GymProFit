package com.gymprofit.api.exceptions;

// ============================================================
// InvalidCredentialsException — excepción de credenciales incorrectas
// Se lanza durante el login cuando el usuario/contraseña no coinciden.
// Capturada por ControllerExceptionHandler para devolver 401.
// ============================================================
public class InvalidCredentialsException extends ExcepcionConClave {

    /**
     * @param clave clave del mensaje en messages*.properties (GP-109)
     * @param args  argumentos del mensaje
     */
    public InvalidCredentialsException(String clave, Object... args) {
        super(clave, args, null);
    }

    // Mensaje por defecto genérico.
    public InvalidCredentialsException() {
        this("error.credenciales");
    }
}
