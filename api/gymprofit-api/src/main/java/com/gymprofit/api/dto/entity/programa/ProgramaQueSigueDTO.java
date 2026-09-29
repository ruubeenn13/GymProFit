package com.gymprofit.api.dto.entity.programa;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

// ============================================================
// ProgramaQueSigueDTO — GET /programas/seguido (GP-074, lote 1.2.1)
// El programa que sigue el usuario del token, su ciclo con sus rutinas y la que toca.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "El programa que sigue el usuario, su ciclo y la rutina que toca")
public class ProgramaQueSigueDTO implements Serializable {
    @Schema(description = "Id del «programa que sigue»", example = "12")
    private Integer id;
    private ProgramaDTO programa;
    @Schema(example = "60")
    private Integer minutos;
    private LocalDateTime fechaInicio;
    @Schema(example = "1")
    private Integer posicionInicial;
    @Schema(description = "La que toca, desde 1; nula si ha borrado todas sus rutinas", example = "2")
    private Integer posicionHoy;
    private List<DiaCicloDTO> ciclo;
    @Schema(description = "Sus rutinas activas, una vez cada una, en el orden en que tocan desde hoy "
            + "(la primera es la de hoy), con sus ejercicios")
    private List<RutinaConEjerciciosDTO> rutinas;
}
