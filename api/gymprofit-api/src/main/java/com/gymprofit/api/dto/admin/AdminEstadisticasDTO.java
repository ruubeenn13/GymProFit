package com.gymprofit.api.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

// ============================================================
// AdminEstadisticasDTO — resumen agregado de métricas globales para el panel admin
// Contiene contadores generales de la plataforma (usuarios, sesiones,
// ejercicios, objetivos, logros...) usados en el dashboard de administración
// de GymProFit.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminEstadisticasDTO implements Serializable {
    // Total de usuarios registrados
    private Long totalUsuarios;
    // Total de usuarios actualmente activos
    private Long usuariosActivos;
    // Total de sesiones de entrenamiento registradas
    private Long totalSesiones;
    // Sesiones de entrenamiento realizadas hoy
    private Long sesionesHoy;
    // Total de ejercicios realizados por todos los usuarios
    private Long totalEjerciciosRealizados;
    // Total de objetivos personales marcados como completados
    private Long totalObjetivosCompletados;
    // Total de logros otorgados a usuarios
    private Long totalLogrosOtorgados;
    // Número de rutinas predefinidas disponibles en el sistema
    private Long rutinasPredefinidas;
    // Número de ejercicios activos en el catálogo
    private Long ejerciciosActivos;
}
