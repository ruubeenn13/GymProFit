package com.gymprofit.api.exceptions;

// ============================================================
// SesionNotCompletedException — excepción de sesión de entrenamiento no completada
// Se lanza cuando se intenta realizar una operación que requiere que una
// SesionEntrenamiento esté finalizada (p.ej. calcular progreso).
// ============================================================
public class SesionNotCompletedException extends RuntimeException {

    // Constructor que compone el mensaje a partir del id de la sesión.
    public SesionNotCompletedException(Integer sesionId) {
        super("La sesión de entrenamiento con id " + sesionId + " no está completada");
    }

    // Constructor con mensaje personalizado.
    public SesionNotCompletedException(String message) {
        super(message);
    }
}