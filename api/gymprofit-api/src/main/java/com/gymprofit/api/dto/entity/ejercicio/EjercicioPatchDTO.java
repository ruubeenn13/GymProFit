package com.gymprofit.api.dto.entity.ejercicio;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

// ============================================================
// EjercicioPatchDTO — datos para actualización parcial (PATCH) de un ejercicio
// Todos los campos son opcionales; solo se actualizan los informados
// (no nulos) sobre el ejercicio existente identificado por su id.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EjercicioPatchDTO implements Serializable {
    // Nuevo nombre (opcional)
    private String nombre;
    // Nueva descripción (opcional)
    private String descripcion;
    // Nuevo grupo muscular (opcional)
    private String grupoMuscular;
    // Nueva dificultad (opcional)
    private String dificultad;
    // Nueva URL de imagen (opcional)
    private String imagenUrl;
    // Nuevas instrucciones (opcional)
    private String instrucciones;
    // Nuevas calorías quemadas (opcional)
    private Integer caloriasQuemadas;
    // Nuevo equipo necesario (opcional)
    private String equipoNecesario;
    // Nuevo estado activo/inactivo (opcional)
    private Boolean activo;
}
