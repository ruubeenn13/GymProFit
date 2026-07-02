package com.gymprofit.api.dto.entity.alimento;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

// ============================================================
// AlimentoDTO — representación completa de un alimento para lectura
// Se usa como respuesta en las operaciones de consulta del catálogo
// de alimentos, incluyendo su información nutricional y de propiedad.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AlimentoDTO implements Serializable {
    private Integer id;
    private String nombre;
    private String categoria;
    private Integer calorias;
    private BigDecimal proteinas;
    private BigDecimal carbohidratos;
    private BigDecimal grasas;
    private BigDecimal fibra;
    private Integer porcionGramos;
    private String descripcion;
    // Indica si el alimento está activo (visible/usable) o dado de baja lógica
    private Boolean activo;
    // Id del usuario propietario si es un alimento personalizado (null si es global)
    private Integer usuarioId;
}
