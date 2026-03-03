package com.gymprofit.api.exceptions;

public class UpdateEntityException extends RuntimeException {

    public UpdateEntityException(String entityName, Object dto) {
        super("Error al actualizar " + entityName + ": " + dto.toString());
    }

    public UpdateEntityException(String entityName, Object dto, Throwable cause) {
        super("Error al actualizar " + entityName + ": " + dto.toString(), cause);
    }

    public UpdateEntityException(String message) {
        super(message);
    }
}