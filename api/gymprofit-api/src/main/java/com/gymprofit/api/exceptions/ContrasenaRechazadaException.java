package com.gymprofit.api.exceptions;

// ============================================================
// ContrasenaRechazadaException — la contraseña nueva tiene la forma correcta pero no
// vale para esa cuenta (GP-101). ControllerExceptionHandler la devuelve como 400 con
// el código en "cause", para que la app lo enseñe en el campo sin depender del texto.
// ============================================================
public class ContrasenaRechazadaException extends RuntimeException {

    /** Está en la lista de contraseñas comunes o filtradas. */
    public static final String PASSWORD_COMUN = "PASSWORD_COMUN";

    /** Es o contiene el nombre del servicio o el nombre de usuario. */
    public static final String PASSWORD_CONTIENE_NOMBRE = "PASSWORD_CONTIENE_NOMBRE";

    private final String codigo;

    public ContrasenaRechazadaException(String message, String codigo) {
        super(message);
        this.codigo = codigo;
    }

    /** Código estable para {@code cause}. */
    public String getCodigo() {
        return codigo;
    }
}
