package com.gymprofit.api.dto.entity.programa;

import com.gymprofit.api.dto.entity.rutinaejercicio.RutinaEjercicioDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

// ============================================================
// RutinaVistaPreviaDTO — una rutina tal como quedaría al seguir el programa (GP-074, 1.2.1)
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Una rutina del programa como quedaría con el tiempo y el perfil")
public class RutinaVistaPreviaDTO implements Serializable {
    @Schema(example = "GIM-TORSO-A")
    private String codigo;
    @Schema(example = "Torso A")
    private String nombre;
    @Schema(description = "Duración como quedaría", example = "45")
    private Integer duracionMinutos;
    @Schema(description = "Duración de la plantilla, sin ajustes", example = "55")
    private Integer duracionPlantilla;
    @Schema(description = "Los ejercicios que se quedan, con su pauta")
    private List<RutinaEjercicioDTO> ejercicios;
    @Schema(description = "Nombres de los ejercicios que se quitan, en el orden de la plantilla")
    private List<String> quitados;
    @Schema(description = "Series de los básicos, solo si cambian respecto a la plantilla", example = "2")
    private Integer seriesBasicos;
}
