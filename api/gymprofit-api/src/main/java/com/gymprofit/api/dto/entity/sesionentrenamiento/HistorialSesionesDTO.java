package com.gymprofit.api.dto.entity.sesionentrenamiento;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// ============================================================
// HistorialSesionesDTO — una página del historial, por semanas (GP-232).
//
// Lo que necesitan las tarjetas de sesión del Historial, de Inicio y de Progreso, en una
// sola petición por página: hasta 8 semanas con sesiones, sin partir ninguna, y el cursor
// de la siguiente. Los kilos usan la definición de GP-011 (la de /sesiones/{id}/volumen) y
// los récords son los que batió cada sesión ese día, aunque otro los superara después.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Una página del historial: semanas con sus sesiones, de la más nueva a la más vieja")
public class HistorialSesionesDTO implements Serializable {

    @Schema(description = "Semanas con alguna sesión terminada, de la más nueva a la más vieja (hasta 8)")
    private List<Semana> semanas;

    @Schema(description = "Cursor de la página siguiente (pásalo como ?antes=); nulo si no hay más",
            example = "2026-09-14")
    private LocalDate siguiente;

    /** Una semana del historial, de lunes a domingo en la hora local. */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(name = "SemanaHistorial", description = "Una semana con sus sesiones")
    public static class Semana implements Serializable {
        @Schema(description = "Lunes de la semana, en la hora local", example = "2026-11-02")
        private LocalDate lunes;
        @Schema(description = "Sesiones terminadas de la semana", example = "2")
        private Integer entrenos;
        @Schema(description = "Kilos de la semana: la suma de los de sus sesiones", example = "22050")
        private BigDecimal kilos;
        @Schema(description = "Sus sesiones, de la más nueva a la más vieja")
        private List<Sesion> sesiones;
    }

    /** Una sesión terminada con lo que pinta su tarjeta. */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(name = "SesionHistorial", description = "Una sesión terminada del historial")
    public static class Sesion implements Serializable {
        @Schema(description = "Id de la sesión", example = "412")
        private Integer id;
        @Schema(description = "Rutina; nulo en un entrenamiento libre")
        private Integer rutinaId;
        @Schema(description = "Nombre de la rutina, también si está desactivada; nulo en un entrenamiento libre")
        private String rutinaNombre;
        @Schema(description = "Inicio, en la hora local")
        private LocalDateTime fechaInicio;
        @Schema(description = "Minutos", example = "58")
        private Integer duracionMinutos;
        @Schema(description = "Kilos movidos (GP-011)", example = "13640")
        private BigDecimal kilos;
        @Schema(description = "Series hechas", example = "18")
        private Integer series;
        @Schema(description = "Ejercicios apuntados; 0 si no se apuntó ninguno", example = "6")
        private Integer ejercicios;
        @Schema(description = "Récords que batió ese día, sin primeras marcas", example = "1")
        private Integer records;
        @Schema(description = "Kilos de la anterior de la misma rutina; nulo la primera vez o sin rutina",
                example = "13250")
        private BigDecimal kilosAnterior;
        @Schema(description = "Series por clave de músculo (Musculos.normalizar), de más a menos")
        private List<SeriesMusculoDTO> musculos;
        @Schema(description = "Sus últimas 6 sesiones con la misma rutina, de la más vieja a ella; "
                + "vacío en un entrenamiento libre")
        private List<VezRutinaDTO> barras;
    }
}
