package com.gymprofit.api.dto.entity.ejerciciorealizado;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

// ============================================================
// EjercicioRealizadoPatchDTO — DTO para actualización parcial de un ejercicio realizado
// Contiene los campos opcionales que se pueden modificar de un registro de
// ejercicio ya realizado dentro de una sesión de entrenamiento (PATCH).
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EjercicioRealizadoPatchDTO {
    // Nº de series completadas realmente por el usuario
    private Integer seriesCompletadas;
    // Nº de repeticiones reales realizadas
    private Integer repeticionesReales;
    // Peso usado en el ejercicio (kg)
    private BigDecimal pesoUsado;
    // Tiempo empleado en segundos (para ejercicios de duración)
    private Integer tiempoSegundos;
    // Notas u observaciones del usuario sobre el ejercicio
    private String notas;
}
