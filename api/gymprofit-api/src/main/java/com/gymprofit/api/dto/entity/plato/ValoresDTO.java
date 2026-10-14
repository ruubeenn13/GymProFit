package com.gymprofit.api.dto.entity.plato;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

// ============================================================
// ValoresDTO — kcal y macros de una cantidad (lote 1.6.6)
// En un plato: el total de sus ingredientes y lo que vale una ración, que es lo que se
// apunta con «1 ración». gramos solo en la ración de un plato con peso hecho.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ValoresDTO implements Serializable {
    private Integer calorias;
    private BigDecimal proteinas;
    private BigDecimal carbohidratos;
    private BigDecimal grasas;
    private BigDecimal fibra;
    private BigDecimal gramos;
}
