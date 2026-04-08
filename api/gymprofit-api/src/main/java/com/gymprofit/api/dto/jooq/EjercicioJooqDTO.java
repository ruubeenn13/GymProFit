package com.gymprofit.api.dto.jooq;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EjercicioJooqDTO implements Serializable {
    private Integer id;
    private String nombre;
    private String descripcion;
    private String grupoMuscular;
    private String dificultad;
    private Integer caloriasQuemadas;
    private String equipoNecesario;
    private Byte activo;
}
