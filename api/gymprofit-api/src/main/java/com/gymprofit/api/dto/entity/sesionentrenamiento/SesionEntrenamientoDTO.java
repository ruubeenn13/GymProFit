package com.gymprofit.api.dto.entity.sesionentrenamiento;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

// ============================================================
// SesionEntrenamientoDTO — DTO de una sesión de entrenamiento completa
// Representa una sesión de entrenamiento de un usuario (fechas, duración,
// calorías, notas y estado) tal como se expone en la API. Se usa tanto
// para lectura como para creación de sesiones desde la app Android.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SesionEntrenamientoDTO implements Serializable {
    private Integer id;
    private Integer usuarioId;
    private Integer rutinaId;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private Integer duracionMinutos;
    // NO HAY CAMPO DE CALORÍAS AQUÍ, Y ES A PROPÓSITO (DEC-004 / GP-010).
    // El gasto calórico de un entrenamiento no se puede estimar con los datos que
    // tiene la app, así que no se estima. La columna sigue en la base de datos hasta
    // la migración que la retire, pero ni se lee ni se escribe.

    // Valoración de 1 a 5, o null si no se valoró (GP-070).
    private Integer valoracion;

    private String notas;
    private Boolean completada;

    // Lista de logros/insignias nuevos desbloqueados al completar la sesión.
    // Solo se incluye en la respuesta JSON si no es null.
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<String> nuevosLogros;
}
