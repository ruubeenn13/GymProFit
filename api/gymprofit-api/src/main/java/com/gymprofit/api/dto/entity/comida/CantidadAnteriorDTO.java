package com.gymprofit.api.dto.entity.comida;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

// ============================================================
// CantidadAnteriorDTO — lo que tenía una línea antes de sumarle (lote 1.6.3, A2)
// Tal cual está en la base, para que la app deshaga exacto con el PATCH de siempre
// (PATCH /alimentos-comida/{id} con estos tres campos: mandan los gramos).
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CantidadAnteriorDTO {
    private BigDecimal cantidadGramos;
    // La ración y cuántas, si se apuntó por raciones; null las dos, en gramos.
    private Integer racionId;
    private BigDecimal raciones;
}
