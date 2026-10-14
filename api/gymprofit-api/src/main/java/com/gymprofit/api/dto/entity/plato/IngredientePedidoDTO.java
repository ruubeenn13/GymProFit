package com.gymprofit.api.dto.entity.plato;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

// ============================================================
// IngredientePedidoDTO — un ingrediente de un plato, como se pide (lote 1.6.6)
// Como añadir a una comida (AnadirAlimentoDTO): el alimento por id o, un producto que
// aún no está en el catálogo, por su código (se materializa al guardar); la cantidad en
// gramos o en raciones (la ración por su id o por su posición).
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class IngredientePedidoDTO {

    private Integer alimentoId;

    @Pattern(regexp = "[0-9]{1,32}")
    private String barcode;

    private BigDecimal cantidadGramos;

    private Integer racionId;

    @PositiveOrZero
    private Integer racionIndice;

    private BigDecimal raciones;
}
