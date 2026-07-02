package com.gymprofit.api.dto.entity.objetivopersonal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

// ============================================================
// ObjetivoPersonalUpdateDTO — actualización específica del valor objetivo
// y del estado de completado de un objetivo personal ya existente.
// Se usa en flujos donde solo interesa cambiar la meta o cerrar el objetivo.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ObjetivoPersonalUpdateDTO implements Serializable {
    private Integer id;
    private BigDecimal valorObjetivo;
    private boolean completado;
}
