package com.gymprofit.api.dto.entity.alimentocomida;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AlimentoComidaPatchDTO {
    private BigDecimal cantidadGramos;
    private Integer caloriasTotales;
}
