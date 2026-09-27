package es.pmdm.gymprofit.utils;

import androidx.annotation.Nullable;

// ============================================================
// Correo — el correo enmascarado de Ajustes (GP-105): «r***@gmail.com».
//
// Ajustes se abre a tres toques de cualquier pantalla, a menudo delante de otra
// persona: se enseña lo justo para reconocer la cuenta, no la dirección entera.
// ============================================================
public final class Correo {

    private Correo() { }

    /**
     * Deja la primera letra de la parte local y el dominio entero.
     *
     * @return el correo enmascarado, o cadena vacía si no hay correo.
     */
    public static String enmascarar(@Nullable String email) {
        if (email == null) return "";
        String e = email.trim();
        int arroba = e.indexOf('@');
        if (arroba <= 0) return e.isEmpty() ? "" : e.charAt(0) + "***";
        return e.charAt(0) + "***" + e.substring(arroba);
    }
}
