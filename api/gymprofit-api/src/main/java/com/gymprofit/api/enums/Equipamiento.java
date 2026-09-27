package com.gymprofit.api.enums;

// ============================================================
// Equipamiento — con qué se hace un ejercicio, como lista cerrada (GP-085)
// Sustituye para filtrar al texto libre de equipo_necesario, que viene de la
// importación con 36 formas distintas de decir lo mismo. El relleno inicial está en
// la migración V202609271301, con el criterio para los ejercicios de varios aparatos.
// ============================================================
public enum Equipamiento {
    BARRA("Barra", "Barbell"),
    MANCUERNAS("Mancuernas", "Dumbbells"),
    KETTLEBELL("Kettlebell", "Kettlebell"),
    MAQUINA("Máquina", "Machine"),
    POLEA("Polea", "Cable"),
    PESO_CORPORAL("Peso corporal", "Bodyweight"),
    BANDA("Banda", "Resistance band"),
    OTRO("Otro", "Other");

    private final String etiqueta;
    private final String etiquetaEn;

    Equipamiento(String etiqueta, String etiquetaEn) {
        this.etiqueta = etiqueta;
        this.etiquetaEn = etiquetaEn;
    }

    /** Nombre en español. */
    public String getEtiqueta() {
        return etiqueta;
    }

    /** Nombre en inglés. */
    public String getEtiquetaEn() {
        return etiquetaEn;
    }
}
