package es.pmdm.gymprofit.utils;

import androidx.annotation.Nullable;

// ============================================================
// Valoracion — qué se manda como valoración de una sesión (GP-077).
//
// La valoración es OPCIONAL. Cero estrellas significa «sin valorar» y se traduce
// en no mandar el campo, para que la base guarde NULL y no un número que el
// usuario no ha dado. Antes la pantalla arrancaba en 3 y ese 3 se guardaba igual
// que uno elegido: desde GP-070 es un dato consultable, así que un valor por
// defecto falsea cualquier media que se saque de ahí.
// ============================================================
public final class Valoracion {

    /** Estrellas máximas; coincide con el @Max de la API. */
    public static final int MAXIMO = 5;

    private Valoracion() {}

    /**
     * Valor que se envía a la API para lo que marca el RatingBar.
     *
     * @param estrellas lo que devuelve {@code RatingBar.getRating()}
     * @return de 1 a 5, o {@code null} si está sin valorar (o fuera de rango), en
     *         cuyo caso el campo no se envía
     */
    @Nullable
    public static Integer paraEnviar(float estrellas) {
        int entero = Math.round(estrellas);
        return entero >= 1 && entero <= MAXIMO ? entero : null;
    }
}
