package com.gymprofit.api.dto.entity.sesionentrenamiento;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// ============================================================
// VezRutinaDTO — una vez que se hizo una rutina: una barra de la tarjeta (GP-232).
//
// Las barras de una sesión y la «última vez» de una rutina son lo mismo: las sesiones
// terminadas de esa rutina, cada una con sus kilos (GP-011), sus series hechas y si batió
// algún récord y cuántos. Las series son la cifra de una vez a 0 kilos (peso corporal o por tiempo).
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Una sesión terminada de una rutina, para sus barras")
public class VezRutinaDTO implements Serializable {
    @Schema(description = "Id de la sesión", example = "412")
    private Integer sesionId;
    @Schema(description = "Inicio, en la hora local")
    private LocalDateTime fecha;
    @Schema(description = "Kilos movidos (GP-011)", example = "13640")
    private BigDecimal kilos;
    @Schema(description = "true si batió algún récord ese día")
    private Boolean record;
    @Schema(description = "Series hechas; la cifra de la tarjeta cuando los kilos son 0", example = "18")
    private Integer series;
    @Schema(description = "Cuántos récords batió ese día («La última vez, …, con 2 récords»)", example = "2")
    private Integer records;
}
