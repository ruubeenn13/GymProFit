package es.pmdm.gymprofit.utils;

import androidx.annotation.Nullable;

import java.math.BigDecimal;

// ============================================================
// Numeros — lectura segura de números escritos por el usuario.
//
// Existe por dos fallos que cerraban la app:
//   · Integer.parseInt sobre un campo sin maxLength: once dígitos desbordan int
//     y lanzan NumberFormatException (crear rutina, series y repeticiones).
//   · Double.parseDouble sobre "1,75": el teclado español ofrece coma, no punto
//     (altura del onboarding, peso y perímetros de mediciones).
//
// Todos los métodos devuelven null en vez de lanzar: quien llama decide qué
// mensaje enseñar y en qué campo. Nunca se hace un apaño silencioso con un valor
// por defecto, porque un dato inventado de peso o altura falsea el objetivo
// calórico de toda la app.
// ============================================================
public final class Numeros {

    private Numeros() {}

    /**
     * Lee un entero dentro de un rango.
     *
     * @param texto lo tecleado por el usuario (puede venir con espacios)
     * @param min   valor mínimo aceptado, incluido
     * @param max   valor máximo aceptado, incluido
     * @return el número, o {@code null} si está vacío, no es un número o se sale del rango
     */
    @Nullable
    public static Integer entero(@Nullable String texto, int min, int max) {
        if (texto == null) return null;
        String limpio = texto.trim();
        if (limpio.isEmpty()) return null;

        try {
            int valor = Integer.parseInt(limpio);
            return (valor < min || valor > max) ? null : valor;
        } catch (NumberFormatException e) {
            // Texto no numérico o número demasiado grande para un int.
            return null;
        }
    }

    /**
     * Lee un decimal dentro de un rango, aceptando coma o punto como separador.
     *
     * @param texto lo tecleado por el usuario
     * @param min   valor mínimo aceptado, incluido
     * @param max   valor máximo aceptado, incluido
     * @return el número, o {@code null} si está vacío, no es un número o se sale del rango
     */
    @Nullable
    public static Double decimal(@Nullable String texto, double min, double max) {
        String limpio = normalizar(texto);
        if (limpio == null) return null;

        try {
            double valor = Double.parseDouble(limpio);
            if (Double.isNaN(valor) || Double.isInfinite(valor)) return null;
            return (valor < min || valor > max) ? null : valor;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Igual que {@link #decimal}, pero devuelve {@link BigDecimal} para los campos
     * que la API espera con decimales exactos (peso y perímetros corporales).
     *
     * @return el número, o {@code null} si no es válido o se sale del rango
     */
    @Nullable
    public static BigDecimal exacto(@Nullable String texto, double min, double max) {
        String limpio = normalizar(texto);
        if (limpio == null) return null;

        try {
            BigDecimal valor = new BigDecimal(limpio);
            if (valor.doubleValue() < min || valor.doubleValue() > max) return null;
            return valor;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // Deja el texto listo para parsear: sin espacios y con punto decimal.
    // Se rechaza aquí lo que tenga más de un separador ("1,75.5"), que si no
    // llegaría como NumberFormatException desde el parseo.
    @Nullable
    private static String normalizar(@Nullable String texto) {
        if (texto == null) return null;
        String limpio = texto.trim().replace(',', '.');
        if (limpio.isEmpty()) return null;

        int puntos = 0;
        for (int i = 0; i < limpio.length(); i++) {
            if (limpio.charAt(i) == '.') puntos++;
        }
        return puntos > 1 ? null : limpio;
    }
}
