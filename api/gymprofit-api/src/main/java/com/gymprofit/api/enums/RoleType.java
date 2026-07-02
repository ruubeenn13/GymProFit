package com.gymprofit.api.enums;

import lombok.Getter;

// ============================================================
// RoleType — tipos de rol de usuario soportados por GymProFit.
// Cada rol lleva asociado un valor numérico (value) que se usa
// como identificador en la tabla de roles/base de datos.
// ============================================================
@Getter
public enum RoleType {
    ADMIN(1),
    USER(2),
    GUEST(3);

    // Valor numérico asociado al rol (id en base de datos).
    private final int value;

    // Constructor del enum: asigna el valor numérico del rol.
    RoleType(int value) {
        this.value = value;
    }
}
