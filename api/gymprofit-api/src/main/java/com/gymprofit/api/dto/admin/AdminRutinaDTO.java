package com.gymprofit.api.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

// ============================================================
// AdminRutinaDTO — vista de rutina para el panel de administración
// Representación resumida de una rutina con datos adicionales (nº de
// ejercicios, si es predefinida) pensada para listados administrativos,
// no para el flujo normal de usuario.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminRutinaDTO implements Serializable {
    private Integer id;
    private String nombre;
    private String descripcion;
    private String nivel;
    private String categoria;
    private String diasSemana;
    private Integer duracionMinutos;
    private Boolean activa;
    // Indica si la rutina viene predefinida por la app (no creada por un usuario)
    private Boolean esPredefinida;
    // Cantidad de ejercicios que componen la rutina
    private Integer numEjercicios;
    private LocalDateTime fechaCreacion;
}
