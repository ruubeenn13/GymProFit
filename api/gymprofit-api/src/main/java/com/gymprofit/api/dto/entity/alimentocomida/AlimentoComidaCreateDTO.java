package com.gymprofit.api.dto.entity.alimentocomida;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AlimentoComidaCreateDTO implements Serializable {
    private Integer comidaId;
    private Integer alimentoId;
    private BigDecimal cantidadGramos;
}
