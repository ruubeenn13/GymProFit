package com.gymprofit.api.dto.entity.alimentocomida;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

// ============================================================
// AlimentoComidaPatchDTO — datos de entrada para actualización parcial
// Permite modificar la cantidad de un alimento dentro de una comida ya
// registrada (y opcionalmente recalcular sus calorías totales).
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AlimentoComidaPatchDTO {
    private BigDecimal cantidadGramos;
    private Integer caloriasTotales;

    // Lote 1.6.1: la ración elegida y cuántas, las dos juntas. Sin cantidadGramos, los
    // gramos salen de la ración; con ella, mandan los gramos. Cambiar solo los gramos
    // quita la ración que hubiera.
    private Integer racionId;
    private BigDecimal raciones;
}
