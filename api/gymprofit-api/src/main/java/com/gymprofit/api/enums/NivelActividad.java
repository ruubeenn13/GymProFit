package com.gymprofit.api.enums;

// ============================================================
// NivelActividad — la actividad física diaria fuera del gimnasio (GP-111).
// Es el multiplicador que pasa del metabolismo basal al gasto diario en la
// calculadora de la app. Distinto de NivelExperiencia, que mide el entrenamiento.
// Opcional: null = no lo ha dicho.
// ============================================================
public enum NivelActividad {
    SEDENTARIO,
    LIGERO,
    MODERADO,
    ACTIVO
}
