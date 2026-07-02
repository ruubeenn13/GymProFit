package com.gymprofit.api.dto.entity.rutina;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

// ============================================================
// RutinaCreateDTO — DTO de entrada para la creación de una nueva rutina de entrenamiento
// Valida los campos obligatorios (nombre, esPredefinida) antes de persistir la rutina.
// Usado por el endpoint POST de rutinas.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RutinaCreateDTO implements Serializable {
    // Id del usuario propietario de la rutina (null si es predefinida del sistema)
    private Integer usuarioId;

    @NotBlank
    @Size(max = 100)
    private String nombre;

    private String descripcion;

    @Positive
    private Integer duracionMinutos;

    // Nivel de dificultad de la rutina (ej. principiante, intermedio, avanzado)
    private String nivel;

    // Indica si la rutina es predefinida por el sistema o creada por el usuario
    @NotNull
    private Boolean esPredefinida;

    private String categoria;
    // Días de la semana en los que se realiza la rutina
    private String diasSemana;
}
