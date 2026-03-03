package com.gymprofit.api.exceptions;

public class InvalidDataException extends RuntimeException {

    public InvalidDataException(String message) {
        super(message);
    }

    public InvalidDataException(String field, String reason) {
        super("Dato inválido en campo '" + field + "': " + reason);
    }
}