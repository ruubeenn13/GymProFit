package com.gymprofit.api.dto.entity.sesionentrenamiento;

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

    @PositiveOrZero
    private Integer caloriasQuemadas;

    private String notas;
    // Indica si la sesión de entrenamiento se completó
    private Boolean completada;
}