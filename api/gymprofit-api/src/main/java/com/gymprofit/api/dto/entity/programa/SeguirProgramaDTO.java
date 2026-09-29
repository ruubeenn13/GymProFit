package com.gymprofit.api.dto.entity.programa;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

// ============================================================
// SeguirProgramaDTO — cuerpo de POST /programas/{codigo}/seguir (GP-074)
// Opcional: sin cuerpo o sin minutos, 60.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Con qué tiempo por sesión se sigue un programa")
public class SeguirProgramaDTO implements Serializable {
    @Schema(description = "Minutos por sesión: 30, 45, 60 o 75. Si no llegan, 60", example = "45")
    private Integer minutos;
}
