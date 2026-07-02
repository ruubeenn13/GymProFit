package com.gymprofit.api.exceptions;

// ============================================================
// CreateEntityException — error al crear una entidad en el servicio.
// Se lanza desde la capa Service cuando falla la persistencia de una
// nueva entidad; la captura ControllerExceptionHandler devolviendo 500.
// ============================================================
public class CreateEntityException extends RuntimeException {

    // Construye el mensaje con el nombre de la entidad y el DTO que se intentó crear.
    public CreateEntityException(String entityName, Object dto) {
        super("Error al crear " + entityName + ": " + dto.toString());
    }

    // Igual que el anterior, propagando además la causa original (ej. excepción de Hibernate).
    public CreateEntityException(String entityName, Object dto, Throwable cause) {
        super("Error al crear " + entityName + ": " + dto.toString(), cause);
    }
}