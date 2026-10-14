package com.gymprofit.api.dto.entity.diario;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// ============================================================
// AguaPedidoDTO — PUT /diario/{fecha}/agua: el total del día, no un vaso (lote 1.6.8)
// Mandar el total hace la petición repetible: reintentarla no suma dos veces.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AguaPedidoDTO {

    /** Tope por encima de lo que nadie bebe en un día, para que un error no guarde basura. */
    public static final int MAXIMO = 20000;

    @NotNull
    @Min(0)
    @Max(MAXIMO)
    private Integer mililitros;
}
