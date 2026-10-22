package com.gymprofit.api.dto.entity.diario;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

// ============================================================
// AlimentoApuntadoDTO — un alimento y cuántas veces lo apuntó el usuario en una
// semana. Es «Lo que más has comido» de «Tu semana de nutrición» (tablero 12).
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Un alimento y las veces que se apuntó en el rango")
public class AlimentoApuntadoDTO implements Serializable {

    @Schema(description = "Alimento del catálogo", example = "88")
    private Integer alimentoId;

    @Schema(description = "Nombre en el idioma de la petición", example = "Pechuga de pollo a la plancha")
    private String nombre;

    @Schema(description = "Veces que se apuntó en el rango", example = "6")
    private Integer veces;
}
