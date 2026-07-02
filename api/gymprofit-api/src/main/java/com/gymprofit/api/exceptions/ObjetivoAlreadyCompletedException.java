package com.gymprofit.api.exceptions;

// ============================================================
// ObjetivoAlreadyCompletedException — excepción de objetivo ya completado
// Se lanza al intentar modificar/completar un ObjetivoPersonal que ya
// tiene estado "completado", evitando transiciones de estado inválidas.
// ============================================================
public class ObjetivoAlreadyCompletedException extends RuntimeException {

    // Constructor que compone el mensaje a partir del id del objetivo.
    public ObjetivoAlreadyCompletedException(Integer objetivoId) {
        super("El objetivo con id " + objetivoId + " ya está completado");
    }

    // Constructor con mensaje personalizado.
    public ObjetivoAlreadyCompletedException(String message) {
        super(message);
    }
}