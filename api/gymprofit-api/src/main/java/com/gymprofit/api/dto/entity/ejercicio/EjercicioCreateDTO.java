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

    // NO HAY CAMPO DE CALORÍAS AQUÍ, Y ES A PROPÓSITO (DEC-004 / GP-010).
    // El gasto calórico de un entrenamiento no se puede estimar con los datos que
    // tiene la app, así que no se estima. La columna sigue en la base de datos hasta
    // la migración que la retire, pero ni se lee ni se escribe.

    // Equipo/material necesario para realizarlo
    private String equipoNecesario;
}
