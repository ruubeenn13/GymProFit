package com.gymprofit.api.dto.entity.objetivopersonal;

import com.gymprofit.api.enums.TipoObjetivo;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ObjetivoPersonalCreateDTO implements Serializable {
    @NotNull
    private Integer usuarioId;

    @NotNull
    private TipoObjetivo tipoObjetivo;

    private String descripcion;

    @PositiveOrZero
    private BigDecimal valorActual;

    @PositiveOrZero
    private BigDecimal valorObjetivo;

    private String unidad;
    private LocalDate fechaLimite;
}