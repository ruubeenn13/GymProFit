package com.gymprofit.api.dto.entity.rutina;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

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
    private Boolean activa;
}
