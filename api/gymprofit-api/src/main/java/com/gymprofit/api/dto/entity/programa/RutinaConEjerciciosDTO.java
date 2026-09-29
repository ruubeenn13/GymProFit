package com.gymprofit.api.dto.entity.programa;

import com.gymprofit.api.dto.entity.rutina.RutinaDTO;
import com.gymprofit.api.dto.entity.rutinaejercicio.RutinaEjercicioDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

// ============================================================
// RutinaConEjerciciosDTO — una rutina con sus ejercicios, en una sola respuesta (GP-074)
// La usan el detalle de un programa (plantillas) y el alta al seguirlo (copias).
// ============================================================
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Schema(description = "Una rutina con sus ejercicios en orden")
public class RutinaConEjerciciosDTO extends RutinaDTO {
    @Schema(description = "Código de la plantilla; nulo en una copia (esa lleva plantillaCodigo)", example = "GIM-TORSO-A")
    private String codigo;
    private List<RutinaEjercicioDTO> ejercicios;
}
