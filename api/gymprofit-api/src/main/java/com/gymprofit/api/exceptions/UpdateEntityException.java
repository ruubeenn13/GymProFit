package com.gymprofit.api.exceptions;

// ============================================================
// UpdateEntityException — excepción de fallo al actualizar una entidad
// Se lanza cuando la actualización de una entidad (vía servicio) falla,
// incluyendo el nombre de la entidad y el DTO recibido en el mensaje.
// ============================================================
public class UpdateEntityException extends RuntimeException {

    // Constructor que compone el mensaje a partir del nombre de entidad y el DTO.
    public UpdateEntityException(String entityName, Object dto) {
        super("Error al actualizar " + entityName + ": " + dto.toString());
    }

    // Constructor que compone el mensaje e incluye la causa original.
    public UpdateEntityException(String entityName, Object dto, Throwable cause) {
        super("Error al actualizar " + entityName + ": " + dto.toString(), cause);
    }

    // Constructor con mensaje personalizado.
    public UpdateEntityException(String message) {
        super(message);
    }
}