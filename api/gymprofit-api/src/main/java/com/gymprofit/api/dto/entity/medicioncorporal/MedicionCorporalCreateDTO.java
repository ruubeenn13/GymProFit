package com.gymprofit.api.dto.entity.medicioncorporal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MedicionCorporalCreateDTO implements Serializable {
    @NotNull
    private Integer usuarioId;

    @NotNull
    @Positive
    private BigDecimal peso;

    @Positive
    private BigDecimal altura;

    @PositiveOrZero
    private BigDecimal grasaCorporal;

    @PositiveOrZero
    private BigDecimal masaMuscular;

    @Positive
    private BigDecimal cintura;

    @Positive
    private BigDecimal pecho;

    @Positive
    private BigDecimal brazos;

    @Positive
    private BigDecimal piernas;

    private String notas;
}
