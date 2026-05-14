package com.gymprofit.api.dto.entity.ejerciciorealizado;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EjercicioRealizadoPatchDTO {
    private Integer seriesCompletadas;
    private Integer repeticionesReales;
    private BigDecimal pesoUsado;
    private Integer tiempoSegundos;
    private String notas;
}
