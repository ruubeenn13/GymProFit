package com.gymprofit.api.dto.entity.alimentocomida;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

// ============================================================
// AlimentoComidaDTO — representación de un alimento dentro de una comida
// Incluye la cantidad consumida y los macros/calorías ya calculados en
// proporción a esa cantidad, además de datos denormalizados del alimento
// (nombre, categoría) para simplificar su uso en el cliente.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AlimentoComidaDTO implements Serializable {
    private Integer id;
    private Integer comidaId;
    private Integer alimentoId;
    private BigDecimal cantidadGramos;
    // Calorías totales ya calculadas según la cantidad en gramos
    private Integer caloriasTotales;
    private String nombreAlimento;
    private String categoriaAlimento;
    // Id del usuario propietario del alimento (null si es del catálogo global)
    private Integer usuarioIdAlimento;
    // Macros totales (proteínas, carbohidratos, grasas) ya calculados según la cantidad
    private BigDecimal proteinasTotales;
    private BigDecimal carbohidratosTotales;
    private BigDecimal grasasTotales;
}
