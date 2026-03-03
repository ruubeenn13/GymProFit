package com.gymprofit.api.exceptions;

public class SesionNotCompletedException extends RuntimeException {

    public SesionNotCompletedException(Integer sesionId) {
        super("La sesión de entrenamiento con id " + sesionId + " no está completada");
    }

    public SesionNotCompletedException(String message) {
        super(message);
    }
}