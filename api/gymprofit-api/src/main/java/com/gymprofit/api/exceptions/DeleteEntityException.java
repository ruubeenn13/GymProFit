package com.gymprofit.api.exceptions;

// ============================================================
// DeleteEntityException — error al eliminar una entidad en el servicio.
// Se lanza desde la capa Service cuando falla el borrado de una entidad;
// la captura ControllerExceptionHandler devolviendo 500.
// ============================================================
public class DeleteEntityException extends RuntimeException {

    // Construye el mensaje con el nombre de la entidad y su id.
    public DeleteEntityException(String entityName, Integer id) {
        super("Error al eliminar " + entityName + " con id: " + id);
    }

    // Igual que el anterior, propagando además la causa original.
    public DeleteEntityException(String entityName, Integer id, Throwable cause) {
        super("Error al eliminar " + entityName + " con id: " + id, cause);
    }

    // Constructor con mensaje directo, sin nombre de entidad/id.
    public DeleteEntityException(String message) {
        super(message);
    }
}