package com.gymprofit.api.enums;

// ============================================================
// MotivoAviso — por qué se reporta un alimento (lote 1.6.1). Lista cerrada a propósito:
// sin texto libre, un aviso no puede llevar un dato personal.
// ============================================================
public enum MotivoAviso {
    // Los valores no cuadran con la etiqueta.
    VALORES,
    // El nombre o la marca.
    NOMBRE,
    // La ración o el envase.
    RACION,
    // Está repetido.
    REPETIDO,
    // Otra cosa.
    OTRO
}
