package com.gymprofit.api.dto.entity.sesionentrenamiento;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SesionEntrenamientoCreateDTO implements Serializable {
    @NotNull
    private Integer usuarioId;

    private Integer rutinaId;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;

    @PositiveOrZero
    private Integer duracionMinutos;

    @PositiveOrZero
    private Integer caloriasQuemadas;

    private String notas;
    private Boolean completada;
}