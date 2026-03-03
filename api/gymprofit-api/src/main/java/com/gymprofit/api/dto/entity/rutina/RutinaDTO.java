package com.gymprofit.api.dto.entity.rutina;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

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
    private Boolean activa;
}
