package com.gymprofit.api.dto.entity.alimentocomida;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AlimentoComidaDTO implements Serializable {
    private Integer id;
    private Integer comidaId;
    private Integer alimentoId;
    private BigDecimal cantidadGramos;
    private Integer caloriasTotales;
    private String nombreAlimento;
    private String categoriaAlimento;
    private BigDecimal proteinasTotales;
    private BigDecimal carbohidratosTotales;
    private BigDecimal grasasTotales;
}
