package com.gymprofit.api.dto.entity.alimentocomida;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AlimentoComidaCreateDTO implements Serializable {
    @NotNull
    private Integer comidaId;

    @NotNull
    private Integer alimentoId;

    @NotNull
    @Positive
    private BigDecimal cantidadGramos;
}
