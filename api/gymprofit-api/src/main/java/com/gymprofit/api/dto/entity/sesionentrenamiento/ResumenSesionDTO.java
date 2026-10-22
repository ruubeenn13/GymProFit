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
// ResumenSesionDTO — el resumen del entreno (GP-210 paso 1, GP-023 y GP-011).
// Todo sale del servidor con la definición de volumen de GP-011, también para las
// sesiones del historial. Las comparaciones son nulas cuando no hay con qué comparar
// (la primera vez), y nunca miran una sesión posterior a la que se resume.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Kilos y series de una sesión, por ejercicio, y su comparación con la vez anterior")
public class ResumenSesionDTO implements Serializable {

    @Schema(description = "Sesión resumida", example = "412")
    private Integer sesionId;

    @Schema(description = "Kilos movidos en la sesión (misma definición que /sesiones/{id}/volumen)", example = "8420")
    private BigDecimal volumenKg;

    @Schema(description = "Series hechas en la sesión", example = "16")
    private Integer series;

    @Schema(description = "Sesión anterior de la misma rutina; nulo en un entrenamiento libre o la primera vez")
    private SesionAnterior anterior;

    @Schema(description = "Los ejercicios de la sesión, en el orden en que se hicieron")
    private List<EjercicioResumen> ejercicios;

    /** La sesión terminada inmediatamente anterior de la misma rutina. */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(name = "SesionAnteriorResumen", description = "La sesión anterior de la misma rutina")
    public static class SesionAnterior implements Serializable {
        @Schema(description = "Id de la sesión anterior")
        private Integer sesionId;
        @Schema(description = "Inicio de esa sesión")
        private LocalDateTime fecha;
        @Schema(description = "Kilos que movió", example = "7950")
        private BigDecimal volumenKg;
    }

    /** Un ejercicio de la sesión con sus kilos y series. */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(name = "EjercicioResumen", description = "Un ejercicio de la sesión resumida")
    public static class EjercicioResumen implements Serializable {
        @Schema(description = "Ejercicio del catálogo")
        private Integer ejercicioId;
        @Schema(description = "Nombre en el idioma de la petición")
        private String nombre;
        @Schema(description = "Kilos movidos en este ejercicio; 0 si es por tiempo", example = "2065")
        private BigDecimal volumenKg;
        @Schema(description = "Series hechas", example = "4")
        private Integer series;
        @Schema(description = "Todas sus series son por tiempo: no mueve kilos")
        private Boolean porTiempo;
        @Schema(description = "Lo mismo la última vez que se hizo antes de esta sesión; nulo si es la primera")
        private UltimaMarca anterior;
    }

    /** Los kilos de un ejercicio la última vez que se hizo antes de la sesión resumida. */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(name = "UltimaMarcaEjercicio", description = "Lo que movió en ese ejercicio la última vez")
    public static class UltimaMarca implements Serializable {
        @Schema(description = "Inicio de la sesión de la última vez")
        private LocalDateTime fecha;
        @Schema(description = "Kilos que movió en ese ejercicio", example = "1920")
        private BigDecimal volumenKg;
    }
}
