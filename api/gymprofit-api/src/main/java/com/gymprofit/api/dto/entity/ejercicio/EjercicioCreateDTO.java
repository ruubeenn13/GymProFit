package com.gymprofit.api.dto.entity.ejercicio;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

// ============================================================
// EjercicioCreateDTO — datos necesarios para dar de alta un ejercicio
// Usado por administradores para crear nuevos ejercicios en el catálogo
// (nombre, grupo muscular, dificultad, instrucciones, etc.).
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EjercicioCreateDTO implements Serializable {
    // Nombre del ejercicio (obligatorio, máx. 100 caracteres)
    @NotBlank
    @Size(max = 100)
    private String nombre;

    // Descripción libre del ejercicio
    private String descripcion;

    // Grupo muscular principal trabajado (obligatorio)
    @NotBlank
    private String grupoMuscular;

    // Nivel de dificultad (obligatorio)
    @NotBlank
    private String dificultad;

    // URL de la imagen ilustrativa del ejercicio
    private String imagenUrl;
    // Instrucciones de ejecución
    private String instrucciones;

    // Calorías aproximadas quemadas realizando el ejercicio
    @PositiveOrZero
    private Integer caloriasQuemadas;

    // Equipo/material necesario para realizarlo
    private String equipoNecesario;
}
