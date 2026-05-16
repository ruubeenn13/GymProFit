package com.gymprofit.api.dto.entity.ejerciciorealizado;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EjercicioRealizadoCreateDTO implements Serializable {
    @NotNull
    private Integer sesionId;

    @NotNull
    private Integer ejercicioId;

    @Min(0)
    private Integer seriesCompletadas;

    @Min(0)
    private Integer repeticionesReales;

    @PositiveOrZero
    private BigDecimal pesoUsado;

    @PositiveOrZero
    private Integer tiempoSegundos;

    private String notas;
}
