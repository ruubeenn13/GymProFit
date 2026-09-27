package com.gymprofit.api.exceptions;

// ============================================================
// NotFoundEntityException — excepción de entidad no encontrada
// Se lanza cuando una búsqueda por id (o similar) no devuelve resultados.
// Capturada por ControllerExceptionHandler para devolver 404.
// ============================================================
public class NotFoundEntityException extends ExcepcionConClave {

    /**
     * @param clave clave del mensaje en messages*.properties (GP-109)
     * @param args  argumentos del mensaje
     */
    public NotFoundEntityException(String clave, Object... args) {
        super(clave, args, null);
    }
}
