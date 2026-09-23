package com.gymprofit.api.dto.entity.sesionentrenamiento;

import com.gymprofit.api.dto.entity.serierealizada.SerieRealizadaCreateDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

// ============================================================
// EjercicioSesionCreateDTO — un ejercicio DENTRO del guardado completo de una sesión.
//
// Es el mismo contenido que EjercicioRealizadoCreateDTO menos el `sesionId`: aquí
// la sesión todavía no existe, se está creando en la misma llamada. Ese es justo
// el punto de GP-006: antes había que crear la sesión primero para tener un id
// que poner en cada ejercicio, y por eso el guardado no podía ser atómico.
//
// Tampoco lleva `seriesCompletadas` ni `pesoUsado`: los deduce el servidor de las
// series, para que no puedan contradecirse (misma regla que en el alta suelta).
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Un ejercicio de la sesión, con sus series")
public class EjercicioSesionCreateDTO implements Serializable {

    @NotNull(message = "El ejercicio es obligatorio")
    @Schema(description = "Id del ejercicio del catálogo", example = "42")
    private Integer ejercicioId;

    @Min(value = 0, message = "Las repeticiones no pueden ser negativas")
    @Schema(description = "Repeticiones que pedía la rutina, como referencia", example = "10")
    private Integer repeticionesReales;

    @Schema(description = "Notas de este ejercicio concreto")
    private String notas;

    @Valid
    @Schema(description = "Series realmente hechas, en orden")
    private List<SerieRealizadaCreateDTO> series;
}
