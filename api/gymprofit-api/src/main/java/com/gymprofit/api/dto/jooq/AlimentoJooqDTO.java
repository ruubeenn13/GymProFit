package com.gymprofit.api.dto.jooq;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

// ============================================================
// AlimentoJooqDTO — DTO de proyección jOOQ para alimentos
// Representa el resultado de consultas jOOQ sobre la tabla de alimentos
// (búsquedas/filtrados complejos de la base de datos de nutrición),
// con la información nutricional básica de cada alimento.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AlimentoJooqDTO implements Serializable {
    private Integer id;
    private String nombre;
    private String categoria;
    private Integer calorias;
    private BigDecimal proteinas;
    private BigDecimal carbohidratos;
    private BigDecimal grasas;
    private Byte activo;
}
