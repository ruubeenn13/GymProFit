package com.gymprofit.api.dto.entity.ejerciciorealizado;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

// ============================================================
// EjercicioRealizadoCreateDTO — datos para registrar un ejercicio realizado
// Usado al finalizar/registrar un ejercicio dentro de una sesión de
// entrenamiento, con las series, repeticiones, peso y tiempo reales.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EjercicioRealizadoCreateDTO implements Serializable {
    // Id de la sesión de entrenamiento a la que pertenece (obligatorio)
    @NotNull
    private Integer sesionId;

    // Id del ejercicio del catálogo realizado (obligatorio)
    @NotNull
    private Integer ejercicioId;

    // Número de series completadas
    @Min(0)
    private Integer seriesCompletadas;

    // Número de repeticiones realmente realizadas
    @Min(0)
    private Integer repeticionesReales;

    // Peso utilizado (kg)
    @PositiveOrZero
    private BigDecimal pesoUsado;

    // Tiempo empleado en segundos
    @PositiveOrZero
    private Integer tiempoSegundos;

    // Notas u observaciones opcionales
    private String notas;

    // El cliente manda las series y el servicio deduce de ellas el resumen
    // (seriesCompletadas y pesoUsado), para que no puedan contradecirse.
    @jakarta.validation.Valid
    @Schema(description = "Series realmente hechas, en orden. Si no se mandan, se guarda solo el resumen")
    private java.util.List<com.gymprofit.api.dto.entity.serierealizada.SerieRealizadaCreateDTO> series;
}
