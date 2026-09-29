package com.gymprofit.api.enums;

// ============================================================
// Sexo — el que el usuario elige en el onboarding para calcular sus calorías (GP-111).
// Solo interviene en la fórmula del metabolismo basal (Mifflin-St Jeor), que tiene
// una constante distinta para cada uno. Opcional: null = no lo ha dicho.
// ============================================================
public enum Sexo {
    HOMBRE,
    MUJER
}
