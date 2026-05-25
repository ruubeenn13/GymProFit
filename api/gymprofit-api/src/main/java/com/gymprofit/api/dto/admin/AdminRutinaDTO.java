package com.gymprofit.api.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

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
    private Boolean esPredefinida;
    private Integer numEjercicios;
    private LocalDateTime fechaCreacion;
}
