package com.gymprofit.api.exceptions;

// ============================================================
// SesionNotCompletedException — excepción de sesión de entrenamiento no completada
// Se lanza cuando se intenta realizar una operación que requiere que una
// SesionEntrenamiento esté finalizada (p.ej. calcular progreso).
// ============================================================
public class SesionNotCompletedException extends ExcepcionConClave {

    /**
     * @param clave clave del mensaje en messages*.properties (GP-109)
     * @param args  argumentos del mensaje
     */
    public SesionNotCompletedException(String clave, Object... args) {
        super(clave, args, null);
    }
}
