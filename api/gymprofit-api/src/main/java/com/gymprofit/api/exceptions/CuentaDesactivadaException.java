package com.gymprofit.api.exceptions;

// ============================================================
// CuentaDesactivadaException — la cuenta existe pero está desactivada (GP-083).
//
// La lanzan el filtro JWT y POST /auth/refresh cuando el token o el refresh son
// válidos pero su cuenta tiene activo = false. ControllerExceptionHandler la
// traduce a 401 con CODIGO en el campo "cause", que es lo que la app lee para
// explicar por qué se le cierra la sesión en vez de decir «sesión caducada».
//
// NO se usa en el login con contraseña: Spring comprueba isEnabled() ANTES que la
// contraseña, así que decir ahí «desactivada» se lo diría a quien no la sabe. El
// login sigue respondiendo el 401 genérico de credenciales.
// ============================================================
public class CuentaDesactivadaException extends RuntimeException {

    /** Código estable que viaja en {@code cause} para que el cliente no dependa del texto. */
    public static final String CODIGO = "CUENTA_DESACTIVADA";

    public CuentaDesactivadaException() {
        super("La cuenta está desactivada");
    }
}
