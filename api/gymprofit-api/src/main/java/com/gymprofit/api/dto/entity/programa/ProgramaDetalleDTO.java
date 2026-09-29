package com.gymprofit.api.dto.entity.programa;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

// ============================================================
// ProgramaDetalleDTO — un programa con cada rutina y sus ejercicios (GP-074)
// rutinas lleva cada rutina distinta una vez, en el orden en que aparece en la semana.
// ============================================================
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Schema(description = "Un programa con su semana y cada rutina con sus ejercicios")
public class ProgramaDetalleDTO extends ProgramaDTO {
    @Schema(description = "Cada rutina distinta del programa, una vez")
    private List<RutinaConEjerciciosDTO> rutinas;
}
