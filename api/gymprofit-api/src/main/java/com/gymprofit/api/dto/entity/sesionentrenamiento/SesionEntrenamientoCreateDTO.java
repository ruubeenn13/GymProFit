package com.gymprofit.api.dto.entity.sesionentrenamiento;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SesionEntrenamientoCreateDTO implements Serializable {
    private Integer usuarioId;
    private Integer rutinaId;
    private LocalDateTime fechaInicio;
}
