package com.gymprofit.api.dto.entity.rutinaejercicio;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

// ============================================================
// RutinaEjercicioCreateDTO — DTO de entrada para asociar un ejercicio a una rutina
// Valida los campos obligatorios (rutinaId, ejercicioId) y las restricciones numéricas
// antes de crear la relación rutina-ejercicio. Usado por el endpoint POST correspondiente.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RutinaEjercicioCreateDTO implements Serializable {
    // Id de la rutina a la que se asocia el ejercicio
    @NotNull
    private Integer rutinaId;

    // Id del ejercicio que se añade a la rutina
    @NotNull
    private Integer ejercicioId;

    @Min(1)
    private Integer series;

    @Min(1)
    private Integer repeticiones;

    // Peso recomendado para realizar el ejercicio
    @PositiveOrZero
    private BigDecimal pesoRecomendado;

    // Tiempo de descanso entre series, en segundos
    @PositiveOrZero
    private Integer tiempoDescanso;

    // Posición/orden del ejercicio dentro de la rutina
    @Min(1)
    private Integer orden;

    private String notas;
}
