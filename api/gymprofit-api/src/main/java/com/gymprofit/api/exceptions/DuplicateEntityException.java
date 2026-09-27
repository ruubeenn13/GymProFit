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
public class DuplicateEntityException extends ExcepcionConClave {

    /** Registro: el nombre de usuario ya lo tiene otra cuenta. */
    public static final String USERNAME_EN_USO = "USERNAME_EN_USO";

    /** Registro: el correo ya lo tiene otra cuenta. */
    public static final String EMAIL_EN_USO = "EMAIL_EN_USO";

    // Código estable para "cause", o null si el error no necesita distinguirse.
    private final String codigo;

    /**
     * @param clave clave del mensaje en messages*.properties (GP-109)
     * @param args  argumentos del mensaje
     */
    public DuplicateEntityException(String clave, Object... args) {
        this(null, clave, args);
    }

    private DuplicateEntityException(String codigo, String clave, Object[] args) {
        super(clave, args, null);
        this.codigo = codigo;
    }

    /**
     * Error de unicidad con un código estable en el campo {@code cause} de la respuesta.
     *
     * @param codigo uno de los códigos de esta clase.
     * @param clave  clave del mensaje, el mismo que sin código.
     * @param args   argumentos del mensaje.
     */
    public static DuplicateEntityException conCodigo(String codigo, String clave, Object... args) {
        return new DuplicateEntityException(codigo, clave, args);
    }

    /** Código estable para {@code cause}, o null si no lo lleva. */
    public String getCodigo() {
        return codigo;
    }
}
