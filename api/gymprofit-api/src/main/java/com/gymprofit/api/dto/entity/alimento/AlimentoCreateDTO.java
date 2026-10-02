package com.gymprofit.api.dto.entity.alimento;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

// ============================================================
// AlimentoCreateDTO — datos de entrada para crear un alimento
// Recoge la información nutricional básica (calorías, macros) que se
// usa al dar de alta un alimento en la base de datos, ya sea del
// catálogo global o personalizado por un usuario.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AlimentoCreateDTO implements Serializable {
    // Nombre del alimento, obligatorio
    @NotBlank
    @Size(max = 100)
    private String nombre;

    private String categoria;

    // Calorías por porción, obligatorio y no negativo
    @NotNull
    @Min(0)
    private Integer calorias;

    @PositiveOrZero
    private BigDecimal proteinas;

    @PositiveOrZero
    private BigDecimal carbohidratos;

    @PositiveOrZero
    private BigDecimal grasas;

    @PositiveOrZero
    private BigDecimal fibra;

    // Tamaño de la porción en gramos, mínimo 1
    @Min(1)
    private Integer porcionGramos;

    private String descripcion;
    // Id del usuario propietario si es un alimento personalizado (null si es global)
    private Integer usuarioId;

    // Código de barras, opcional (lote 1.6.1): «Créalo» desde el escáner o la búsqueda lo
    // trae puesto. Solo cifras, como en GET /alimentos/codigo; único por dueño (DEC-040).
    @jakarta.validation.constraints.Pattern(regexp = "[0-9]{1,32}")
    private String barcode;
}
