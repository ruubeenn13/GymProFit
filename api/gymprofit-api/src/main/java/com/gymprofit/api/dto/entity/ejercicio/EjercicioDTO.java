package com.gymprofit.api.dto.entity.ejercicio;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

// ============================================================
// EjercicioDTO — representación completa de un ejercicio del catálogo
// Se devuelve al consultar el catálogo de ejercicios disponibles para
// componer rutinas y registrar entrenamientos.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EjercicioDTO implements Serializable {
    // Identificador del ejercicio
    private Integer id;
    // Nombre del ejercicio
    private String nombre;
    // Descripción libre
    private String descripcion;
    // Grupo muscular principal trabajado (grupo grueso, para el chip de filtro)
    private String grupoMuscular;
    // Músculo primario preciso (ej. "Aductores"); se muestra en el detalle
    private String musculoPrimario;
    // Nivel de dificultad
    private String dificultad;
    // URL de la imagen ilustrativa (fotograma 1)
    private String imagenUrl;
    // Fotograma 2 de la demostración (la app alterna ambos para animarla)
    private String imagenUrl2;
    // Instrucciones de ejecución
    private String instrucciones;
    // Calorías aproximadas quemadas
    private Integer caloriasQuemadas;
    // Equipo/material necesario
    private String equipoNecesario;
    // Indica si el ejercicio está activo (visible/usable) o dado de baja lógica
    private Boolean activo;
}
