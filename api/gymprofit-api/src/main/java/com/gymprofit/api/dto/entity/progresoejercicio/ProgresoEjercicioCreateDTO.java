package com.gymprofit.api.dto.entity.progresoejercicio;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProgresoEjercicioCreateDTO implements Serializable {
    private Integer usuarioId;
    private Integer ejercicioId;
    private BigDecimal mejorPeso;
    private Integer mejorRepeticiones;
    private Integer mejorTiempoSegundos;
    private String notas;
}
