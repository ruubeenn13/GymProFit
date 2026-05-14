package com.gymprofit.api.dto.entity.progresoejercicio;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProgresoEjercicioPatchDTO {
    private LocalDateTime fecha;
    private BigDecimal mejorPeso;
    private Integer mejorRepeticiones;
    private Integer mejorTiempoSegundos;
    private String notas;
}
