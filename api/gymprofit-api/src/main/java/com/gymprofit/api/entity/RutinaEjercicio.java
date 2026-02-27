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
@Table(name = "rutina_ejercicio")
public class RutinaEjercicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, columnDefinition = "INT DEFAULT 3")
    private Integer series;

    @Column(nullable = false, columnDefinition = "INT DEFAULT 10")
    private Integer repeticiones;

    @Column(name = "peso_recomendado", precision = 5, scale = 2)
    private BigDecimal pesoRecomendado;

    @Column(name = "tiempo_descanso")
    private Integer tiempoDescanso;

    @Column(nullable = false, columnDefinition = "INT DEFAULT 1")
    private Integer orden;

    @Column(columnDefinition = "TEXT")
    private String notas;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rutina_id", nullable = false)
    private Rutina rutina;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ejercicio_id", nullable = false)
    private Ejercicio ejercicio;
}
