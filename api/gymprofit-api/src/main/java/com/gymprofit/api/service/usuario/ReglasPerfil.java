package com.gymprofit.api.service.usuario;

import com.gymprofit.api.exceptions.InvalidDataException;

// ============================================================
// ReglasPerfil — las reglas de los campos del perfil que comparten el alta y el PATCH
// (lote 1.5.0): el alta nueva crea la cuenta con su perfil en una sola llamada, y un
// dato no puede valer en una puerta y no en la otra.
// ============================================================
public final class ReglasPerfil {

    /** Largo máximo del nombre para mostrar (GP-116), el de la columna usuarios.nombre. */
    public static final int NOMBRE_MAX = 40;

    /** Edad mínima: la política de privacidad dice que la app no es para menores de 14. */
    public static final int EDAD_MINIMA = 14;

    private ReglasPerfil() {
    }

    /**
     * Rechaza una edad menor que la mínima. Sin edad no hay nada que comprobar: es opcional.
     *
     * @param edad la edad que llega del cliente, o null.
     * @throws InvalidDataException 400 con {@code EDAD_MINIMA} en "cause".
     */
    public static void edad(Integer edad) {
        if (edad != null && edad < EDAD_MINIMA) {
            throw InvalidDataException.conCodigo(InvalidDataException.EDAD_MINIMA, "error.edad.minima", EDAD_MINIMA);
        }
    }

    /**
     * Nombre para mostrar (GP-116): se recorta y se cuentan puntos de código, como la
     * columna VARCHAR(40) de utf8mb4 (un emoji es uno).
     *
     * @param nombre lo que llega del cliente, o null.
     * @return null si no llegó; "" si llegó en blanco (quien llama decide si borra); el
     *         nombre recortado si no.
     * @throws InvalidDataException si pasa de 40.
     */
    public static String nombre(String nombre) {
        if (nombre == null) return null;
        String recortado = nombre.strip();
        if (recortado.codePointCount(0, recortado.length()) > NOMBRE_MAX) {
            throw new InvalidDataException("error.nombre.largo", NOMBRE_MAX);
        }
        return recortado;
    }
}
