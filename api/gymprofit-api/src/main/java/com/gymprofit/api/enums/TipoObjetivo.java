package com.gymprofit.api.enums;

// ============================================================
// TipoObjetivo — tipos de objetivos personales que puede fijarse un usuario.
// Clasifica la finalidad de un ObjetivoPersonal (perder peso, ganar
// masa muscular, mejorar resistencia, etc.) dentro de GymProFit.
// ============================================================
public enum TipoObjetivo {
    PERDER_PESO,
    GANAR_MASA_MUSCULAR,
    MEJORAR_RESISTENCIA,
    MEJORAR_FLEXIBILIDAD,
    MEJORAR_FUERZA,
    MANTENER_PESO,
    REDUCIR_GRASA_CORPORAL,
    MEJORAR_VELOCIDAD,
    AUMENTAR_CALORIAS,
    MEJORAR_MOVILIDAD,
    COMPLETAR_RETO,
    // Objetivo personalizado no cubierto por las categorías anteriores.
    OTRO
}