package com.gymprofit.api.dto.entity.rutina;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

// ============================================================
// RutinaPatchDTO — DTO para actualización parcial (PATCH) de una rutina
// Todos los campos son opcionales; solo se actualizan los que llegan no nulos.
// Usado en el endpoint PATCH de rutinas.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RutinaPatchDTO implements Serializable {
    private String nombre;
    private String descripcion;
    private Integer duracionMinutos;
    private Integer caloriasAproximadas;
    private String nivel;
    private String categoria;
    private String diasSemana;
    // Permite activar o desactivar la rutina
    private Boolean activa;
}
