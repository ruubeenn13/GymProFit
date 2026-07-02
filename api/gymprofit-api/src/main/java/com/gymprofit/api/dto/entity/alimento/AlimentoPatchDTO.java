package com.gymprofit.api.dto.entity.alimento;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

// ============================================================
// AlimentoPatchDTO — datos de entrada para actualización parcial de un alimento
// Todos los campos son opcionales (PATCH): solo se modifican los que
// llegan informados en la petición, dejando el resto sin cambios.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AlimentoPatchDTO implements Serializable {
    private String nombre;
    private String categoria;
    private Integer calorias;
    private BigDecimal proteinas;
    private BigDecimal carbohidratos;
    private BigDecimal grasas;
    private BigDecimal fibra;
    private Integer porcionGramos;
    private String descripcion;
    private Boolean activo;
}
