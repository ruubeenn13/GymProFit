package com.gymprofit.api.dto.entity.programa;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

// ============================================================
// DiaCicloDTO — un día del ciclo del programa que se sigue, con la rutina del usuario
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Un día del ciclo del programa que se sigue")
public class DiaCicloDTO implements Serializable {
    @Schema(description = "Posición en el ciclo, desde 1", example = "2")
    private Integer posicion;
    @Schema(example = "GIM-PIERNA-A")
    private String plantillaCodigo;
    @Schema(description = "La copia del usuario; nula si la borró del todo", example = "10702")
    private Integer rutinaId;
    @Schema(example = "Pierna A")
    private String rutinaNombre;
    private Integer duracionMinutos;
    @Schema(description = "Si su copia sigue activa; si no, el ciclo la salta")
    private Boolean activa;
    @Schema(description = "Si se ha hecho en esta vuelta del ciclo")
    private Boolean hecha;
}
