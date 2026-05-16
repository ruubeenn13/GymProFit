package com.gymprofit.api.dto.entity.progresoejercicio;

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
public class ProgresoEjercicioCreateDTO implements Serializable {
    @NotNull
    private Integer usuarioId;

    @NotNull
    private Integer ejercicioId;

    @PositiveOrZero
    private BigDecimal mejorPeso;

    @Min(0)
    private Integer mejorRepeticiones;

    @Min(0)
    private Integer mejorTiempoSegundos;

    private String notas;
}
