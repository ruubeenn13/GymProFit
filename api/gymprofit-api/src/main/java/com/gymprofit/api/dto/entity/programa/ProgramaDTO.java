package com.gymprofit.api.dto.entity.programa;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

// ============================================================
// ProgramaDTO — un programa del catálogo, para la lista (GP-074)
// Textos en el idioma de la petición, como el resto del catálogo.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Un programa del catálogo: varios días, cada uno una rutina")
public class ProgramaDTO implements Serializable {
    @Schema(description = "Código estable del programa", example = "GIM-TP")
    private String codigo;
    @Schema(example = "Torso y pierna")
    private String nombre;
    private String descripcion;
    @Schema(description = "PRINCIPIANTE o INTERMEDIO. El avanzado usa los de intermedio", example = "INTERMEDIO")
    private String nivel;
    @Schema(description = "GIMNASIO, MANCUERNAS o PESO_CORPORAL", example = "GIMNASIO")
    private String equipamiento;
    @Schema(example = "4")
    private Integer diasMin;
    @Schema(example = "4")
    private Integer diasMax;
    @Schema(description = "La semana: rutinas en orden, que pueden repetirse")
    private List<DiaProgramaDTO> semana;
}
