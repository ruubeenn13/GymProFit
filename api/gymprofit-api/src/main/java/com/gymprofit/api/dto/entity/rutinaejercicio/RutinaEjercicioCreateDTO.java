package com.gymprofit.api.dto.entity.rutinaejercicio;

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
public class RutinaEjercicioCreateDTO implements Serializable {
    @NotNull
    private Integer rutinaId;

    @NotNull
    private Integer ejercicioId;

    @Min(1)
    private Integer series;

    @Min(1)
    private Integer repeticiones;

    @PositiveOrZero
    private BigDecimal pesoRecomendado;

    @PositiveOrZero
    private Integer tiempoDescanso;

    @Min(1)
    private Integer orden;

    private String notas;
}
