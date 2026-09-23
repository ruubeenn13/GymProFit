package com.gymprofit.api.dto.entity.sesionentrenamiento;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

// ============================================================
// SesionEntrenamientoPatchDTO — DTO para actualización parcial (PATCH) de una sesión
// Contiene únicamente los campos modificables de una sesión de entrenamiento
// ya existente. Los campos null indican que no se deben actualizar,
// permitiendo actualizaciones parciales desde la app Android.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SesionEntrenamientoPatchDTO implements Serializable {
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private Integer duracionMinutos;
    // NO HAY CAMPO DE CALORÍAS AQUÍ, Y ES A PROPÓSITO (DEC-004 / GP-010).
    // El gasto calórico de un entrenamiento no se puede estimar con los datos que
    // tiene la app, así que no se estima. La columna sigue en la base de datos hasta
    // la migración que la retire, pero ni se lee ni se escribe.
    private String notas;
    private Boolean completada;
}
