package com.gymprofit.api.enums;

// ============================================================
// TipoLogro — tipos de logros/insignias que puede desbloquear un usuario.
// Se usan para categorizar los distintos hitos de gamificación
// (primera sesión, constancia, número de sesiones, objetivos, etc.)
// dentro del sistema de logros de GymProFit.
// ============================================================
public enum TipoLogro {
    // Se otorga al completar la primera sesión de entrenamiento.
    PRIMERA_SESION,
    // Reconoce la regularidad/constancia en el entrenamiento.
    CONSTANCIA,
    // Reconoce un alto nivel de dedicación del usuario.
    DEDICADO,
    // Se otorga al alcanzar un número elevado de sesiones (ej. 100).
    CENTENARIO,
    // Se otorga al completar un objetivo personal.
    OBJETIVO_CUMPLIDO,
    // Nivel máximo de logro por rendimiento/actividad sostenida.
    MAQUINA
}
