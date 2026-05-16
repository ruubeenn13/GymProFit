package com.gymprofit.api.dto.entity.rutina;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RutinaCreateDTO implements Serializable {
    private Integer usuarioId;

    @NotBlank
    @Size(max = 100)
    private String nombre;

    private String descripcion;

    @Positive
    private Integer duracionMinutos;

    private String nivel;

    @NotNull
    private Boolean esPredefinida;

    private String categoria;
    private String diasSemana;
}
