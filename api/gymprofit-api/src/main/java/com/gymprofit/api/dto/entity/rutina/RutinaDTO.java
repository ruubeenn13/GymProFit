package com.gymprofit.api.dto.entity.rutina;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

// ============================================================
// RutinaDTO — DTO de salida que representa una rutina de entrenamiento completa
// Expone los datos de la rutina, incluyendo campos calculados como el número de
// ejercicios y las calorías aproximadas, sin exponer la entidad JPA directamente.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RutinaDTO implements Serializable {
    private Integer id;
    private String usuarioId;
    private String nombre;
    private String descripcion;
    private Integer duracionMinutos;
    private String nivel;
    private Boolean esPredefinida;
    private String categoria;
    private String diasSemana;
    private LocalDateTime fechaCreacion;
    // Indica si la rutina está activa (visible/usable) o desactivada
    private Boolean activa;
    // Número de ejercicios que componen la rutina (calculado)
    private Integer numEjercicios;
    // NO HAY CAMPO DE CALORÍAS AQUÍ, Y ES A PROPÓSITO (DEC-004 / GP-010).
    // El gasto calórico de un entrenamiento no se puede estimar con los datos que
    // tiene la app, así que no se estima. La columna sigue en la base de datos hasta
    // la migración que la retire, pero ni se lee ni se escribe.
}
