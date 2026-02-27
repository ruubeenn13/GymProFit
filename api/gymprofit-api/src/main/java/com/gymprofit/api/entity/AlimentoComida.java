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
@Table(name = "alimentos_comida")
public class AlimentoComida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "cantidad_gramos", nullable = false, precision = 6, scale = 2)
    private BigDecimal cantidadGramos;

    @Column(name = "calorias_totales")
    private Integer caloriasTotales;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comida_id", nullable = false)
    private Comida comida;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alimento_id", nullable = false)
    private Alimento alimento;
}
