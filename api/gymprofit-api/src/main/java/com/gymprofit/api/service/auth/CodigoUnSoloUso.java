package com.gymprofit.api.service.auth;

import java.security.SecureRandom;

// ============================================================
// CodigoUnSoloUso — las reglas de los códigos de seis dígitos que viajan por correo
//
// Las comparten la recuperación de contraseña (GP-007, GP-044) y la verificación del
// correo (GP-045, DEC-066). Viven aquí para que las dos cadenas no se separen: si una
// cambia la vida del código o los intentos, cambia la otra.
//
// Lo que no está aquí, porque cada servicio lo aplica con su propio repositorio, es el
// resto del mecanismo, que es el mismo en los dos: guardar solo el hash BCrypt, matar
// los códigos vivos al emitir uno nuevo, un solo uso y quemar el código al agotar los
// intentos.
// ============================================================
public final class CodigoUnSoloUso {

    /** Vida del código. Corta a propósito: lo justo para ir al correo y volver. */
    public static final int MINUTOS_VALIDEZ = 15;

    /** Intentos fallidos antes de quemar el código y obligar a pedir otro. */
    public static final int MAX_INTENTOS = 5;

    // SecureRandom y no Random: de esto depende el acceso a una cuenta.
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private CodigoUnSoloUso() {
    }

    /**
     * Genera un código nuevo.
     *
     * @return seis dígitos, con los ceros a la izquierda incluidos: 000123 es tan válido
     *         como 987654.
     */
    public static String generar() {
        return String.format("%06d", ALEATORIO.nextInt(1_000_000));
    }
}
