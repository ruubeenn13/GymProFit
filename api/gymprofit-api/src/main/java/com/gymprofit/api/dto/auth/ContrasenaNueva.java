package com.gymprofit.api.dto.auth;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// ============================================================
// @ContrasenaNueva — la forma de una contraseña que se pone nueva (GP-101, DEC-034).
//
// Alta, recuperar y cambiar. Nunca al entrar: las cuentas que ya existen siguen
// entrando con la contraseña que tengan.
//   · Mínimo 8 CARACTERES, contados como caracteres (code points), no como unidades
//     UTF-16: un emoji es un carácter aunque ocupe dos unidades.
//   · Máximo 72 BYTES en UTF-8, que es lo que admite BCrypt. Spring Security rechaza
//     más, y eso salía como el 400 genérico «Parámetro con valor inválido».
//   · Cualquier carácter imprimible, espacios y Unicode incluidos. Sin reglas de
//     composición: ni mayúsculas, ni números, ni símbolos obligatorios.
// Lo que depende de la cuenta (la lista de bloqueo y el nombre) lo comprueba
// PoliticaContrasena en el servicio, con un código en "cause".
// ============================================================
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ContrasenaNuevaValidator.class)
public @interface ContrasenaNueva {

    /** Mínimo de caracteres. NIST pide 15 si la contraseña es el único factor; ver DEC-034. */
    int MINIMO_CARACTERES = 8;

    /** Máximo de bytes en UTF-8: el límite de BCrypt. */
    int MAXIMO_BYTES = 72;

    String message() default "La contraseña no es válida";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
