package com.gymprofit.api.dto.entity.programa;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

// ============================================================
// VistaPreviaDTO — cómo quedaría un programa al seguirlo, sin guardar nada (GP-074, 1.2.1)
// Mismas reglas que POST /programas/{codigo}/seguir, con el perfil del token.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Vista previa de seguir un programa")
public class VistaPreviaDTO implements Serializable {
    @Schema(example = "GIM-TP")
    private String programaCodigo;
    @Schema(example = "45")
    private Integer minutos;
    @Schema(description = "Ajustes del perfil que se aplican: AVANZADO, FUERZA; vacío si ninguno")
    private List<String> ajustes;
    private List<RutinaVistaPreviaDTO> rutinas;
}
