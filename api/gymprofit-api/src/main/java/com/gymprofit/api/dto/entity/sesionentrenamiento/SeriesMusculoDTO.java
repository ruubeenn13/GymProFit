package com.gymprofit.api.dto.entity.sesionentrenamiento;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

// ============================================================
// SeriesMusculoDTO — cuántas series cayeron en un músculo (GP-232).
//
// La clave es la de Musculos.normalizar, la misma de la silueta y los récords; la app la
// pasa a zona (Zonas) y a frase («Pierna y abdomen»). En una sesión son las series
// hechas; en una rutina, las que tiene pautadas.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Series de un músculo")
public class SeriesMusculoDTO implements Serializable {
    @Schema(description = "Clave de músculo", example = "cuadriceps")
    private String musculo;
    @Schema(description = "Series", example = "6")
    private Integer series;
}
