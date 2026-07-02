package com.gymprofit.api.dto.entity.medicioncorporal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

// ============================================================
// MedicionCorporalCreateDTO — DTO para registrar una nueva medición corporal
// Contiene las medidas antropométricas que un usuario introduce para
// hacer seguimiento de su progreso físico (peso, altura, perímetros, etc.).
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MedicionCorporalCreateDTO implements Serializable {
    // Usuario al que pertenece la medición, obligatorio
    @NotNull
    private Integer usuarioId;

    // Peso corporal en kg, obligatorio y positivo
    @NotNull
    @Positive
    private BigDecimal peso;

    // Altura en cm
    @Positive
    private BigDecimal altura;

    // Porcentaje de grasa corporal
    @PositiveOrZero
    private BigDecimal grasaCorporal;

    // Porcentaje/kg de masa muscular
    @PositiveOrZero
    private BigDecimal masaMuscular;

    // Perímetro de cintura en cm
    @Positive
    private BigDecimal cintura;

    // Perímetro de pecho en cm
    @Positive
    private BigDecimal pecho;

    // Perímetro de brazos en cm
    @Positive
    private BigDecimal brazos;

    // Perímetro de piernas en cm
    @Positive
    private BigDecimal piernas;

    // Notas u observaciones adicionales de la medición
    private String notas;
}
