package com.gymprofit.api.dto.entity.programa;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

// ============================================================
// DiaProgramaDTO — un día de la semana de un programa (GP-074)
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Un día de la semana de un programa, en orden")
public class DiaProgramaDTO implements Serializable {
    @Schema(description = "Posición en la semana, empezando en 1", example = "1")
    private Integer posicion;
    @Schema(description = "Código de la rutina plantilla que toca", example = "GIM-TORSO-A")
    private String rutinaCodigo;
    @Schema(description = "Nombre de la rutina, en el idioma de la petición", example = "Torso A")
    private String rutinaNombre;
    @Schema(description = "Duración estimada de la plantilla, en minutos", example = "55")
    private Integer duracionMinutos;
}
