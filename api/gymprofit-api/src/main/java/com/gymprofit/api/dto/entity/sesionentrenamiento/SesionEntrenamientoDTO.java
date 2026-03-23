package com.gymprofit.api.dto.entity.sesionentrenamiento;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SesionEntrenamientoDTO implements Serializable {
    private Integer id;
    private Integer usuarioId;
    private Integer rutinaId;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private Integer duracionMinutos;
    private Integer caloriasQuemadas;
    private String notas;
    private Boolean completada;
}
