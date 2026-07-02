package com.gymprofit.api.dto.entity.alimentocomida;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

// ============================================================
// AlimentoComidaCreateDTO — datos de entrada para añadir un alimento a una comida
// Representa la relación N:M entre Comida y Alimento junto con la
// cantidad en gramos consumida, usada al registrar una comida en el diario.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AlimentoComidaCreateDTO implements Serializable {
    // Id de la comida a la que se añade el alimento
    @NotNull
    private Integer comidaId;

    // Id del alimento del catálogo que se añade
    @NotNull
    private Integer alimentoId;

    // Cantidad consumida en gramos, obligatoria y positiva
    @NotNull
    @Positive
    private BigDecimal cantidadGramos;
}
