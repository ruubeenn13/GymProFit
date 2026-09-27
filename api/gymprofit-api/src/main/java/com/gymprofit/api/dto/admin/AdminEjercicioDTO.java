package com.gymprofit.api.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

// ============================================================
// AdminEjercicioDTO — una fila de la lista de ejercicios de la web (GP-085)
// Los dos nombres, porque la lista sirve para traducir el catálogo.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminEjercicioDTO implements Serializable {

    private Integer id;
    private String nombre;
    private String nombreEn;
    private String grupoMuscular;
    private String equipamiento;
    private String dificultad;
    private boolean activo;
    private boolean nombreRevisado;
}
