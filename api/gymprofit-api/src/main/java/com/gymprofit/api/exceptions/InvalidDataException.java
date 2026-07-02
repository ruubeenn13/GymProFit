package com.gymprofit.api.exceptions;

// ============================================================
// InvalidDataException — excepción de datos de entrada inválidos
// Se lanza cuando un DTO o parámetro no cumple las reglas de negocio.
// Capturada por ControllerExceptionHandler para devolver 400.
// ============================================================
public class InvalidDataException extends RuntimeException {

    // Constructor con mensaje personalizado.
    public InvalidDataException(String message) {
        super(message);
    }

    // Constructor que compone el mensaje a partir del campo inválido y la razón.
    public InvalidDataException(String field, String reason) {
        super("Dato inválido en campo '" + field + "': " + reason);
    }
}