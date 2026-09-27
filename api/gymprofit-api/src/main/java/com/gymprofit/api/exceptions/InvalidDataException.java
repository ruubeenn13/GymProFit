package com.gymprofit.api.exceptions;

// ============================================================
// InvalidDataException — excepción de datos de entrada inválidos
// Se lanza cuando un DTO o parámetro no cumple las reglas de negocio.
// Capturada por ControllerExceptionHandler para devolver 400.
// ============================================================
public class InvalidDataException extends ExcepcionConClave {

    /**
     * @param clave clave del mensaje en messages*.properties (GP-109)
     * @param args  argumentos del mensaje
     */
    public InvalidDataException(String clave, Object... args) {
        super(clave, args, null);
    }
}
