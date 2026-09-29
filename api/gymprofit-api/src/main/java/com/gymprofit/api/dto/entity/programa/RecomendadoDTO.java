package com.gymprofit.api.dto.entity.programa;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

// ============================================================
// RecomendadoDTO — el programa que toca según la tabla del catálogo (GP-074, 1.2.1)
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "El programa recomendado para el perfil, el equipamiento y los días")
public class RecomendadoDTO implements Serializable {
    private ProgramaDTO programa;
    @Schema(description = "Nivel usado: el del perfil, o PRINCIPIANTE si no tiene", example = "INTERMEDIO")
    private String nivel;
    @Schema(description = "Si el perfil tiene nivel; si no, se usa el de principiante", example = "true")
    private Boolean nivelEnPerfil;
    @Schema(description = "Por qué, si no es el obvio; en el idioma de la petición. Nulo si lo es")
    private String motivo;
}
