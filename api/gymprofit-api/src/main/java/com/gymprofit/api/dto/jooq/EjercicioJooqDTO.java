package com.gymprofit.api.dto.jooq;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

// ============================================================
// EjercicioJooqDTO — proyección plana de Ejercicio para consultas jOOQ
// DTO usado en consultas complejas/joins con jOOQ donde no se necesita
// la entidad JPA completa. Sus campos (enums como String) reflejan
// directamente las columnas seleccionadas en la query.
// ============================================================
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
