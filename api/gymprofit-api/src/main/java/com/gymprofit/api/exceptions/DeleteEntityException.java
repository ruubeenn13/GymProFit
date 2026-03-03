package com.gymprofit.api.exceptions;

public class DeleteEntityException extends RuntimeException {

    public DeleteEntityException(String entityName, Integer id) {
        super("Error al eliminar " + entityName + " con id: " + id);
    }

    public DeleteEntityException(String entityName, Integer id, Throwable cause) {
        super("Error al eliminar " + entityName + " con id: " + id, cause);
    }

    public DeleteEntityException(String message) {
        super(message);
    }
}