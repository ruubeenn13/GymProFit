package com.gymprofit.api.dto.entity.record;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// ============================================================
// PuntoProgresionDTO — la mejor serie de un ejercicio en una sesión (GP-088)
// Un punto de la gráfica de progresión de la ficha de ejercicio.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PuntoProgresionDTO implements Serializable {

    private Integer sesionId;
    private LocalDateTime fecha;

    // PESO o REPETICIONES, como en RecordDTO.
    private String tipo;
    private BigDecimal peso;
    private Integer repeticiones;
    private BigDecimal unoRmEstimado;
}
