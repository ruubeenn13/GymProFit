package com.gymprofit.api.exceptions;

// ============================================================
// ObjetivoAlreadyCompletedException — excepción de objetivo ya completado
// Se lanza al intentar modificar/completar un ObjetivoPersonal que ya
// tiene estado "completado", evitando transiciones de estado inválidas.
// ============================================================
public class ObjetivoAlreadyCompletedException extends ExcepcionConClave {

    /**
     * @param clave clave del mensaje en messages*.properties (GP-109)
     * @param args  argumentos del mensaje
     */
    public ObjetivoAlreadyCompletedException(String clave, Object... args) {
        super(clave, args, null);
    }
}
