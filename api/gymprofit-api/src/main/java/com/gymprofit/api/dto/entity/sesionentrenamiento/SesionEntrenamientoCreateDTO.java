package com.gymprofit.api.dto.entity.sesionentrenamiento;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

// ============================================================
// SesionEntrenamientoCreateDTO — DTO de entrada para registrar una nueva sesión de entrenamiento
// Valida el usuario obligatorio y las restricciones numéricas antes de persistir la sesión.
// Usado por el endpoint POST de sesiones de entrenamiento.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SesionEntrenamientoCreateDTO implements Serializable {
    // Id del usuario que realiza la sesión
    @NotNull
    private Integer usuarioId;

    // Id de la rutina seguida en la sesión (opcional, puede ser entrenamiento libre)
    private Integer rutinaId;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;

    @PositiveOrZero
    private Integer duracionMinutos;

    // NO HAY CAMPO DE CALORÍAS AQUÍ, Y ES A PROPÓSITO (DEC-004 / GP-010).
    // El gasto calórico de un entrenamiento no se puede estimar con los datos que
    // tiene la app, así que no se estima. La columna sigue en la base de datos hasta
    // la migración que la retire, pero ni se lee ni se escribe.

    // Valoración de la sesión, 1 a 5. Opcional: no valorar es un caso válido, y
    // por eso no lleva @NotNull. Lo que no vale es un 0 o un 7 (GP-070).
    @Min(1)
    @Max(5)
    private Integer valoracion;

    private String notas;
    // Indica si la sesión de entrenamiento se completó
    private Boolean completada;
}