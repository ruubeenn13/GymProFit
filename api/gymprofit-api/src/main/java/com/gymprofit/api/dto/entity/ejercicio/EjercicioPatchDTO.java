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
    // NO HAY CAMPO DE CALORÍAS AQUÍ, Y ES A PROPÓSITO (DEC-004 / GP-010).
    // El gasto calórico de un entrenamiento no se puede estimar con los datos que
    // tiene la app, así que no se estima. La columna sigue en la base de datos hasta
    // la migración que la retire, pero ni se lee ni se escribe.
    // Nuevo equipo necesario (opcional)
    private String equipoNecesario;
    // Nuevo estado activo/inactivo (opcional)
    private Boolean activo;
}
