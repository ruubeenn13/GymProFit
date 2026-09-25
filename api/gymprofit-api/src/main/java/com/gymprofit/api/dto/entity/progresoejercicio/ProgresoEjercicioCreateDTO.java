package com.gymprofit.api.dto.entity.progresoejercicio;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

// ============================================================
// ProgresoEjercicioCreateDTO — datos para registrar el progreso de un usuario
// en un ejercicio concreto (mejores marcas de peso, repeticiones o tiempo).
// Usado al crear un nuevo registro de progreso en GymProFit.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProgresoEjercicioCreateDTO implements Serializable {
    // Usuario al que pertenece el progreso (obligatorio).
    @NotNull
    private Integer usuarioId;

    // Ejercicio sobre el que se registra el progreso (obligatorio).
    @NotNull
    private Integer ejercicioId;

    // Mejor peso levantado registrado hasta el momento.
    @PositiveOrZero
    private BigDecimal mejorPeso;

    // Mejor número de repeticiones registrado.
    @Min(0)
    private Integer mejorRepeticiones;

    // Mejor tiempo (en segundos) registrado, para ejercicios cronometrados.
    @Min(0)
    private Integer mejorTiempoSegundos;

    private String notas;
}
