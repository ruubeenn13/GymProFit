package com.gymprofit.api.dto.entity.rutinaejercicio;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

// ============================================================
// RutinaEjercicioDTO — DTO de salida que representa un ejercicio dentro de una rutina
// Incluye datos propios de la relación (series, repeticiones, orden) junto con datos
// enriquecidos del ejercicio (nombre, calorías) para evitar consultas adicionales en el cliente.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RutinaEjercicioDTO implements Serializable {
    private Integer id;
    private Integer rutinaId;
    private Integer ejercicioId;
    private Integer repeticiones;
    private Integer series;
    private BigDecimal pesoRecomendado;
    private Integer tiempoDescanso;
    private Integer orden;
    private String notas;
    // NO HAY CAMPO DE CALORÍAS AQUÍ, Y ES A PROPÓSITO (DEC-004 / GP-010).
    // El gasto calórico de un entrenamiento no se puede estimar con los datos que
    // tiene la app, así que no se estima. La columna sigue en la base de datos hasta
    // la migración que la retire, pero ni se lee ni se escribe.
    // Nombre del ejercicio (dato enriquecido desde el catálogo de ejercicios)
    private String nombreEjercicio;
}
