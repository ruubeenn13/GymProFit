package com.gymprofit.api.dto.entity.sesionentrenamiento;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// ============================================================
// UltimaVezDTO — lo que hizo el usuario la última vez en un ejercicio (GP-014).
// Las series de la sesión terminada más reciente que tenga series de ese
// ejercicio. La sesión en vivo lo enseña como «Anterior» en cada fila y como
// pista en los campos vacíos; por eso viaja por número de serie y no agregado.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Series de la última vez que el usuario hizo un ejercicio")
public class UltimaVezDTO implements Serializable {

    @Schema(description = "Ejercicio del catálogo", example = "12")
    private Integer ejercicioId;

    @Schema(description = "Inicio de la sesión de la que salen las series")
    private LocalDateTime fecha;

    @Schema(description = "Series marcadas de esa sesión, por número")
    private List<Serie> series;

    /** Una serie de la última vez: peso y repeticiones, o segundos si es por tiempo. */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(name = "SerieUltimaVez", description = "Una serie de la última vez")
    public static class Serie implements Serializable {

        @Schema(description = "Número de la serie, desde 1", example = "1")
        private Integer numero;

        @Schema(description = "Peso usado; nulo en peso corporal", example = "57.50")
        private BigDecimal peso;

        @Schema(description = "Repeticiones; 0 en una serie por tiempo", example = "12")
        private Integer repeticiones;

        @Schema(description = "Segundos de una serie por tiempo; nulo en una de repeticiones", example = "40")
        private Integer segundos;
    }
}
