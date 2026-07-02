package com.gymprofit.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
// ============================================================
// Alimento — alimento del catálogo de nutrición (mapa a tabla alimentos)
// Representa un alimento con sus macronutrientes por porción. Puede ser
// del catálogo global (usuario null) o creado por un usuario concreto,
// y se usa como referencia dentro de AlimentoComida.
// ============================================================
@Entity
@Table(name = "alimentos")
public class Alimento {

    // Identificador autogenerado del alimento.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Nombre del alimento.
    @Column(nullable = false, length = 100)
    private String nombre;

    // Categoría del alimento (ej. lácteos, frutas...).
    @Column(length = 50)
    private String categoria;

    // Calorías por porción.
    @Column(nullable = false)
    private Integer calorias;

    // Gramos de proteínas por porción.
    @Column(precision = 5, scale = 2)
    private BigDecimal proteinas;

    // Gramos de carbohidratos por porción.
    @Column(precision = 5, scale = 2)
    private BigDecimal carbohidratos;

    // Gramos de grasas por porción.
    @Column(precision = 5, scale = 2)
    private BigDecimal grasas;

    // Gramos de fibra por porción.
    @Column(precision = 5, scale = 2)
    private BigDecimal fibra;

    // Tamaño de la porción de referencia en gramos.
    @Column(name = "porcion_gramos")
    private Integer porcionGramos;

    // Descripción libre del alimento.
    @Column(columnDefinition = "TEXT")
    private String descripcion;

    // Indica si el alimento está activo/visible (borrado lógico).
    @Column(columnDefinition = "TINYINT(1) DEFAULT 1")
    private Boolean activo;

    // Usuario propietario si el alimento fue creado por un usuario (null = catálogo global).
    @ManyToOne(optional = true)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;
}
