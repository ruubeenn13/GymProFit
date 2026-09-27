package com.gymprofit.api.exceptions;

// ============================================================
// ContrasenaActualIncorrectaException — la contraseña actual no es la de la cuenta
// al cambiarla (POST /auth/change-password).
//
// ControllerExceptionHandler la traduce a 403 con CODIGO en "cause". No es un 401 a
// propósito: la app trata todo 401 como sesión caducada —renueva el token y, si no
// puede, lleva a Login—, y teclear mal la contraseña actual no es perder la sesión.
// Es el mismo 403 que da el borrado de cuenta (GP-008) con la contraseña equivocada.
// ============================================================
public class ContrasenaActualIncorrectaException extends ExcepcionConClave {

    /** Código estable que viaja en {@code cause} para que el cliente no dependa del texto. */
    public static final String CODIGO = "PASSWORD_ACTUAL_INCORRECTA";

    public ContrasenaActualIncorrectaException() {
        super("error.contrasena.actualIncorrecta", null, null);
    }
}
