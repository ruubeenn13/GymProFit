package com.gymprofit.api.dto.entity.objetivopersonal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

// ============================================================
// ObjetivoPersonalPatchDTO — actualización parcial (PATCH) de un objetivo.
// Permite modificar descripción, valores de progreso/meta, unidad o fecha
// límite sin necesidad de reenviar el objetivo completo.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ObjetivoPersonalPatchDTO implements Serializable {
    private String descripcion;
    private BigDecimal valorActual;
    private BigDecimal valorObjetivo;
    private String unidad;
    private LocalDate fechaLimite;
}
