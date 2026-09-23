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
    // NO HAY CAMPO DE CALORÍAS AQUÍ, Y ES A PROPÓSITO (DEC-004 / GP-010).
    // El gasto calórico de un entrenamiento no se puede estimar con los datos que
    // tiene la app, así que no se estima. La columna sigue en la base de datos hasta
    // la migración que la retire, pero ni se lee ni se escribe.
    private String equipoNecesario;
    private Byte activo;
}
