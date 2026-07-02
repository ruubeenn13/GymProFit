package com.gymprofit.api.dto.entity.ejerciciorealizado;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

// ============================================================
// EjercicioRealizadoDTO — representación completa de un ejercicio realizado
// Se devuelve al consultar el historial de una sesión de entrenamiento,
// reflejando el rendimiento real (series, repeticiones, peso, tiempo).
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EjercicioRealizadoDTO implements Serializable {
    // Identificador del registro
    private Integer id;
    // Id de la sesión de entrenamiento asociada
    private Integer sesionId;
    // Id del ejercicio del catálogo realizado
    private Integer ejercicioId;
    // Series completadas
    private Integer seriesCompletadas;
    // Repeticiones reales realizadas
    private Integer repeticionesReales;
    // Peso utilizado (kg)
    private BigDecimal pesoUsado;
    // Tiempo empleado en segundos
    private Integer tiempoSegundos;
    // Notas u observaciones
    private String notas;
}
