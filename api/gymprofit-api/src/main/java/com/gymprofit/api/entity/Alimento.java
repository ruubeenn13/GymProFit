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
@Entity
@Table(name = "alimentos")
public class Alimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 50)
    private String categoria;

    @Column(nullable = false)
    private Integer calorias;

    @Column(precision = 5, scale = 2)
    private Integer proteinas;

    @Column(precision = 5, scale = 2)
    private BigDecimal carbohidratos;

    @Column(precision = 5, scale = 2)
    private BigDecimal grasas;

    @Column(precision = 5, scale = 2)
    private BigDecimal fibra;

    @Column(name = "porcion_gramos")
    private Integer porcionGramos;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(columnDefinition = "TINYINT(1) DEFAULT 1")
    private Boolean activo;
}
