package com.gymprofit.api.exceptions;

// ============================================================
// DuplicateEntityException — error por violación de unicidad (registro duplicado).
// Se lanza cuando ya existe una entidad con el mismo valor en un campo único
// (ej. email); la captura ControllerExceptionHandler devolviendo 400.
//
// Puede llevar un código estable que viaja en "cause", para que el cliente sepa
// qué campo está repetido sin depender del texto (GP-095: el registro distingue
// usuario en uso de correo en uso).
// ============================================================
public class DuplicateEntityException extends RuntimeException {

    /** Registro: el nombre de usuario ya lo tiene otra cuenta. */
    public static final String USERNAME_EN_USO = "USERNAME_EN_USO";

    /** Registro: el correo ya lo tiene otra cuenta. */
    public static final String EMAIL_EN_USO = "EMAIL_EN_USO";

    // Código estable para "cause", o null si el error no necesita distinguirse.
    private final String codigo;

    // Constructor con mensaje directo.
    public DuplicateEntityException(String message) {
        super(message);
        this.codigo = null;
    }

    // Construye el mensaje indicando el campo y el valor duplicado.
    public DuplicateEntityException(String field, String value) {
        super("Ya existe un registro con " + field + ": " + value);
        this.codigo = null;
    }

    // El tercer parámetro solo desempata con el constructor (field, value), que ya
    // ocupa la firma de dos String. Se construye por conCodigo().
    private DuplicateEntityException(String message, String codigo, boolean conCodigo) {
        super(message);
        this.codigo = codigo;
    }

    /**
     * Error de unicidad con un código estable en el campo {@code cause} de la respuesta.
     *
     * @param message texto para el cliente, el mismo que sin código.
     * @param codigo  uno de los códigos de esta clase.
     */
    public static DuplicateEntityException conCodigo(String message, String codigo) {
        return new DuplicateEntityException(message, codigo, true);
    }

    /** Código estable para {@code cause}, o null si no lo lleva. */
    public String getCodigo() {
        return codigo;
    }
}
