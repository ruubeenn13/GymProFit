package es.pmdm.gymprofit.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

// ============================================================
// ErrorAlta — por qué no se ha creado la cuenta y en qué campo se dice (GP-103).
//
// «Guarda tu plan» pone cada error bajo su campo (tablero 10 del lienzo): el correo en
// uso, con «Entrar con él»; las reglas de la contraseña, que la API comprueba también
// contra el nombre y el correo; y sin red, un reintento que no pierde el plan. Lo demás
// (límite de peticiones, servidor) va por el aviso común, que ya sabe decirlo.
//
// Lee el código de «cause», no el texto, igual que PoliticaCuenta. Sin Android, para
// probarlo en la JVM.
// ============================================================
public final class ErrorAlta {

    private ErrorAlta() {}

    /**
     * Qué dice la tarjeta del programa de «Tu plan» cuando no carga, siempre con
     * «Reintentar»: cuánto esperar si se pasó del cupo (429, GP-148), que no hay red, o
     * el genérico.
     */
    public enum Programa { ESPERA, SIN_RED, OTRO }

    /** @param code código HTTP, o -1 si no hubo respuesta. */
    @NonNull
    public static Programa programa(int code) {
        if (code == 429) return Programa.ESPERA;
        return code == -1 ? Programa.SIN_RED : Programa.OTRO;
    }

    /** Dónde va el error. */
    public enum Tipo { CORREO_EN_USO, PASSWORD_COMUN, PASSWORD_CONTIENE_NOMBRE, SIN_RED, OTRO }

    /**
     * @param code   código HTTP, o -1 si no hubo respuesta.
     * @param cuerpo cuerpo de error tal cual, o null.
     */
    @NonNull
    public static Tipo de(int code, @Nullable String cuerpo) {
        if (code == -1) return Tipo.SIN_RED;
        if (code != 400 && code != 409) return Tipo.OTRO;
        if (PoliticaCuenta.campoEnUso(cuerpo) == PoliticaCuenta.CampoEnUso.CORREO) return Tipo.CORREO_EN_USO;
        switch (PoliticaCuenta.rechazoPassword(cuerpo)) {
            case COMUN: return Tipo.PASSWORD_COMUN;
            case CONTIENE_NOMBRE: return Tipo.PASSWORD_CONTIENE_NOMBRE;
            default: return Tipo.OTRO;
        }
    }
}
