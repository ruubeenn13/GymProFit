package com.gymprofit.api.dto.entity.alimento;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

// ============================================================
// RacionPropiaDTO — la ración de un alimento propio al crearlo o editarlo (lote 1.6.2)
// unidad, de ClaveRacion (UNIDAD, RACION, ENVASE, REBANADA), y sus gramos, de más de 0
// a 2000. AlimentoService lo comprueba y responde 400 con su clave de error.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RacionPropiaDTO {
    private String unidad;
    private BigDecimal gramos;
}
