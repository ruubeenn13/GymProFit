package com.gymprofit.api.exceptions;

public class CreateEntityException extends RuntimeException {

    public CreateEntityException(String entityName, Object dto) {
        super("Error al crear " + entityName + ": " + dto.toString());
    }

    public CreateEntityException(String entityName, Object dto, Throwable cause) {
        super("Error al crear " + entityName + ": " + dto.toString(), cause);
    }
}