package com.gymprofit.api.dto.entity.sesionentrenamiento;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

// ============================================================
// SesionCompletaCreateDTO — la sesión ENTERA en una sola petición (GP-006).
//
// Antes la app mandaba un POST para la sesión y luego uno por cada ejercicio, con
// callbacks vacíos, y cerraba la pantalla sin esperar. Si fallaba uno de los de
// en medio, quedaba una sesión creada y a medias, y el usuario había visto un
// mensaje de éxito. Aquí viaja todo junto y se guarda todo o nada.
//
// El `usuarioId` NO está en este DTO a propósito (DEC-013): el dueño sale del
// token, nunca del cuerpo.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Sesión completa: la sesión, sus ejercicios y sus series")
public class SesionCompletaCreateDTO implements Serializable {

    /**
     * Clave única del INTENTO de guardado, no de la pulsación: el reintento tras
     * un fallo de red manda la misma, y por eso el servidor puede reconocerlo.
     * <p>
     * Es obligatoria. Sin ella este endpoint no aporta nada sobre el viejo: el
     * reintento que la pantalla necesita duplicaría entrenamientos.
     */
    @NotBlank(message = "La clave de idempotencia es obligatoria")
    @Size(max = 64, message = "La clave de idempotencia no puede pasar de 64 caracteres")
    @Schema(description = "Clave única del intento de guardado; el reintento reusa la misma",
            example = "9f1c1a7e-0b2f-4c4a-9d3e-6a5b8c7d0e1f")
    private String claveIdempotencia;

    @Schema(description = "Rutina seguida; nulo en entrenamiento libre", example = "7")
    private Integer rutinaId;

    @Schema(description = "Inicio de la sesión; si falta, el momento de guardar")
    private LocalDateTime fechaInicio;

    @PositiveOrZero(message = "La duración no puede ser negativa")
    @Schema(description = "Duración en minutos", example = "45")
    private Integer duracionMinutos;

    @Min(value = 1, message = "La valoración va de 1 a 5")
    @Max(value = 5, message = "La valoración va de 1 a 5")
    @Schema(description = "Valoración de 1 a 5; nula si no se valoró", example = "4")
    private Integer valoracion;

    @Schema(description = "Notas del usuario. Solo suyas: la valoración ya NO va aquí (GP-070)")
    private String notas;

    @Schema(description = "Si la sesión se da por completada; por defecto sí", example = "true")
    private Boolean completada;

    @Valid
    @Schema(description = "Ejercicios hechos, con sus series. Puede venir vacío en un entrenamiento libre sin detalle")
    private List<EjercicioSesionCreateDTO> ejercicios;
}
