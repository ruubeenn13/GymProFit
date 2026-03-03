package com.gymprofit.api.exceptions;

public class ObjetivoAlreadyCompletedException extends RuntimeException {

    public ObjetivoAlreadyCompletedException(Integer objetivoId) {
        super("El objetivo con id " + objetivoId + " ya está completado");
    }

    public ObjetivoAlreadyCompletedException(String message) {
        super(message);
    }
}