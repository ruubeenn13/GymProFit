package com.gymprofit.api.exceptions;

public class DuplicateEntityException extends RuntimeException {

    public DuplicateEntityException(String message) {
        super(message);
    }

    public DuplicateEntityException(String field, String value) {
        super("Ya existe un registro con " + field + ": " + value);
    }
}