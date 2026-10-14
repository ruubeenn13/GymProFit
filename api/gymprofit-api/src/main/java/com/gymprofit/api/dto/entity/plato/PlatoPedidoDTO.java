package com.gymprofit.api.dto.entity.plato;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

// ============================================================
// PlatoPedidoDTO — crear o cambiar un plato propio (lote 1.6.6, DEC-047)
// Sin dueño: es el del token (DEC-013). Cambiarlo sustituye sus ingredientes.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlatoPedidoDTO {

    @NotBlank
    @Size(max = 100)
    private String nombre;

    // Cuántas raciones salen.
    @NotNull
    @Min(1)
    @Max(99)
    private Integer raciones;

    // El peso del plato hecho, en gramos; opcional.
    private BigDecimal pesoGramos;

    @NotNull
    @Size(min = 1, max = 50)
    @Valid
    private List<IngredientePedidoDTO> ingredientes;
}
