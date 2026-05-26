package com.gymprofit.api.dto.entity.alimento;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AlimentoCreateDTO implements Serializable {
    @NotBlank
    @Size(max = 100)
    private String nombre;

    private String categoria;

    @NotNull
    @Min(0)
    private Integer calorias;

    @PositiveOrZero
    private BigDecimal proteinas;

    @PositiveOrZero
    private BigDecimal carbohidratos;

    @PositiveOrZero
    private BigDecimal grasas;

    @PositiveOrZero
    private BigDecimal fibra;

    @Min(1)
    private Integer porcionGramos;

    private String descripcion;
    private Integer usuarioId;
}
