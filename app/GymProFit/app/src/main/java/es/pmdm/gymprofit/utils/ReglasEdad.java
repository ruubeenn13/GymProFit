package es.pmdm.gymprofit.utils;

import androidx.annotation.Nullable;

// ============================================================
// ReglasEdad — la edad que se escribe en la app (GP-145, lote 1.5.1).
//
// De 14 a 100. El mínimo es el de la política de privacidad y el de la API
// (DEC-038, ReglasPerfil.EDAD_MINIMA); por debajo, la API responde 400 con el código
// EDAD_MINIMA, y la app lo explica en el campo en vez de un error genérico. El tope
// de 100 es solo de la app: deja fuera las erratas («250») sin rechazar a nadie real.
//
// Se lee sin romper con cualquier texto: antes Editar perfil hacía Integer.parseInt a
// pelo y un número largo o una letra cerraban la pantalla.
// ============================================================
public final class ReglasEdad {

    /** La edad mínima de la política, la de la API (DEC-038). */
    public static final int MINIMA = 14;
    /** El tope de lo que se puede escribir. */
    public static final int MAXIMA = 100;

    /** El código que pone la API en «cause» cuando la edad es menor que la mínima. */
    static final String CODIGO_EDAD_MINIMA = "EDAD_MINIMA";

    private ReglasEdad() {}

    /** Qué pasa con lo escrito. */
    public enum Estado { VACIA, VALIDA, MENOR, FUERA }

    /**
     * Cómo es lo escrito en el campo de la edad.
     *
     * @param texto lo tecleado, tal cual; puede ser null.
     * @return VACIA si no hay nada, MENOR si es un número por debajo de 14, VALIDA de 14
     *         a 100, y FUERA para todo lo demás (letras, decimales, más de 100).
     */
    public static Estado estado(@Nullable String texto) {
        if (texto == null || texto.trim().isEmpty()) return Estado.VACIA;
        if (Numeros.entero(texto, MINIMA, MAXIMA) != null) return Estado.VALIDA;
        Integer cualquiera = Numeros.entero(texto, Integer.MIN_VALUE, MINIMA - 1);
        return cualquiera != null ? Estado.MENOR : Estado.FUERA;
    }

    /**
     * La edad escrita, o null si no es válida o no hay.
     */
    @Nullable
    public static Integer leer(@Nullable String texto) {
        return Numeros.entero(texto, MINIMA, MAXIMA);
    }

    /**
     * Si un 400 de la API es por la edad mínima. Busca el código, no el texto, como
     * {@link PoliticaCuenta#campoEnUso(String)}.
     */
    public static boolean esEdadMinima(@Nullable String cuerpo) {
        return cuerpo != null && cuerpo.contains(CODIGO_EDAD_MINIMA);
    }
}
