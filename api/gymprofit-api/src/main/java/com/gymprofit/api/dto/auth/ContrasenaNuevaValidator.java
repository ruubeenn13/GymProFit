package com.gymprofit.api.dto.auth;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.nio.charset.StandardCharsets;

// ============================================================
// ContrasenaNuevaValidator — comprueba @ContrasenaNueva (GP-101).
// Un solo mensaje por fallo, el primero que se encuentra: longitud, bytes, caracteres.
// El nulo lo rechaza @NotBlank, que va al lado.
// ============================================================
public class ContrasenaNuevaValidator implements ConstraintValidator<ContrasenaNueva, String> {

    @Override
    public boolean isValid(String valor, ConstraintValidatorContext ctx) {
        if (valor == null) return true;
        String error = error(valor);
        if (error == null) return true;
        ctx.disableDefaultConstraintViolation();
        ctx.buildConstraintViolationWithTemplate(error).addConstraintViolation();
        return false;
    }

    /**
     * Por qué no vale una contraseña, o null si vale.
     *
     * @param valor la contraseña tal cual llega.
     */
    static String error(String valor) {
        if (valor.codePointCount(0, valor.length()) < ContrasenaNueva.MINIMO_CARACTERES) {
            return "La contraseña debe tener al menos " + ContrasenaNueva.MINIMO_CARACTERES + " caracteres";
        }
        if (valor.getBytes(StandardCharsets.UTF_8).length > ContrasenaNueva.MAXIMO_BYTES) {
            return "La contraseña es demasiado larga";
        }
        boolean imprimible = valor.codePoints().noneMatch(c ->
                Character.isISOControl(c) || Character.getType(c) == Character.UNASSIGNED);
        if (!imprimible) {
            return "La contraseña solo puede llevar caracteres imprimibles";
        }
        return null;
    }
}
