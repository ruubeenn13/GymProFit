package com.gymprofit.api.enums;

// ============================================================
// TipoNotificacion — categorías de notificaciones enviadas al usuario.
// Permite distinguir el origen/propósito de cada notificación
// (recordatorios, logros desbloqueados, objetivos, avisos del sistema).
// ============================================================
public enum TipoNotificacion {
    // Recordatorio para el usuario (ej. entrenar, registrar comida).
    RECORDATORIO,
    // Notificación de un logro desbloqueado.
    LOGRO,
    // Notificación relacionada con el progreso/cumplimiento de un objetivo.
    OBJETIVO,
    // Notificación genérica del sistema.
    SISTEMA
}
