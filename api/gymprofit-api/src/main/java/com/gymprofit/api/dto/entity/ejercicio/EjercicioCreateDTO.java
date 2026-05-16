package com.gymprofit.api.dto.entity.ejercicio;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EjercicioCreateDTO implements Serializable {
    @NotBlank
    @Size(max = 100)
    private String nombre;

    private String descripcion;

    @NotBlank
    private String grupoMuscular;

    @NotBlank
    private String dificultad;

    private String imagenUrl;
    private String instrucciones;

    @PositiveOrZero
    private Integer caloriasQuemadas;

    private String equipoNecesario;
}
