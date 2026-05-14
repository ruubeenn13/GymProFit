package com.gymprofit.api.dto.entity.ejercicio;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EjercicioPatchDTO implements Serializable {
    private String nombre;
    private String descripcion;
    private String grupoMuscular;
    private String dificultad;
    private String imagenUrl;
    private String instrucciones;
    private Integer caloriasQuemadas;
    private String equipoNecesario;
    private Boolean activo;
}
