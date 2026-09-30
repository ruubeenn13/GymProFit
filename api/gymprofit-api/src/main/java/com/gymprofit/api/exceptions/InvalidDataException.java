package com.gymprofit.api.exceptions;

// ============================================================
// InvalidDataException — excepción de datos de entrada inválidos
// Se lanza cuando un DTO o parámetro no cumple las reglas de negocio.
// Capturada por ControllerExceptionHandler para devolver 400.
//
// Puede llevar un código estable que viaja en "cause", para que el cliente sepa qué
// campo marcar sin depender del texto (lote 1.5.0: el alta nueva).
// ============================================================
public class InvalidDataException extends ExcepcionConClave {

    /** El nombre de usuario lleva «@», que queda para el correo (GP-103). */
    public static final String USERNAME_NO_VALIDO = "USERNAME_NO_VALIDO";

    // Código estable para "cause", o null si el error no necesita distinguirse.
    private final String codigo;

    /**
     * @param clave clave del mensaje en messages*.properties (GP-109)
     * @param args  argumentos del mensaje
     */
    public InvalidDataException(String clave, Object... args) {
        this(null, clave, args);
    }

    private InvalidDataException(String codigo, String clave, Object[] args) {
        super(clave, args, null);
        this.codigo = codigo;
    }

    /**
     * Dato no válido con un código estable en el campo {@code cause} de la respuesta.
     *
     * @param codigo uno de los códigos de esta clase.
     * @param clave  clave del mensaje.
     * @param args   argumentos del mensaje.
     */
    public static InvalidDataException conCodigo(String codigo, String clave, Object... args) {
        return new InvalidDataException(codigo, clave, args);
    }

    /** Código estable para {@code cause}, o null si no lo lleva. */
    public String getCodigo() {
        return codigo;
    }
}
