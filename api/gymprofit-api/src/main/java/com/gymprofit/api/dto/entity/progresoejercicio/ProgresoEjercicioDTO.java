package com.gymprofit.api.dto.entity.progresoejercicio;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProgresoEjercicioDTO implements Serializable {
    private Integer id;
    private Integer usuarioId;
    private Integer ejercicioId;
    private LocalDateTime fecha;
    private BigDecimal mejorPeso;
    private Integer mejorRepeticiones;
    private Integer mejorTiempoSegundos;
    private String notas;
}
