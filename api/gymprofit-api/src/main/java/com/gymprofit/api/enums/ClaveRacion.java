package com.gymprofit.api.enums;

import java.util.Arrays;
import java.util.Optional;

// ============================================================
// ClaveRacion — las raciones que puede tener un alimento propio (lote 1.6.2, DEC-043)
// Lista cerrada a propósito: con texto libre, la app no sabría pluralizarla ni marcarla
// al editar. Se guarda como las demás raciones, con su nombre en los dos idiomas.
// ============================================================
public enum ClaveRacion {
    UNIDAD("1 unidad", "1 unit"),
    RACION("1 ración", "1 serving"),
    ENVASE("1 envase", "1 pack"),
    REBANADA("1 rebanada", "1 slice");

    private final String nombre;
    private final String nombreEn;

    ClaveRacion(String nombre, String nombreEn) {
        this.nombre = nombre;
        this.nombreEn = nombreEn;
    }

    public String getNombre() {
        return nombre;
    }

    public String getNombreEn() {
        return nombreEn;
    }

    /** La clave de una ración por su nombre en español, si es de esta lista. */
    public static Optional<ClaveRacion> porNombre(String nombre) {
        return Arrays.stream(values()).filter(c -> c.nombre.equals(nombre)).findFirst();
    }

    /** La clave escrita por la app («UNIDAD»), o vacío si no es de la lista. */
    public static Optional<ClaveRacion> leer(String texto) {
        return Arrays.stream(values()).filter(c -> c.name().equals(texto)).findFirst();
    }
}
