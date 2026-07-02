package com.gymprofit.api.exceptions;

// ============================================================
// DuplicateEntityException — error por violación de unicidad (registro duplicado).
// Se lanza cuando ya existe una entidad con el mismo valor en un campo único
// (ej. email); la captura ControllerExceptionHandler devolviendo 400.
// ============================================================
public class DuplicateEntityException extends RuntimeException {

    // Constructor con mensaje directo.
    public DuplicateEntityException(String message) {
        super(message);
    }

    // Construye el mensaje indicando el campo y el valor duplicado.
    public DuplicateEntityException(String field, String value) {
        super("Ya existe un registro con " + field + ": " + value);
    }
}