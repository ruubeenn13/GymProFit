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
// ============================================================
// RutinaEjercicio — asociación entre una Rutina y un Ejercicio con sus parámetros
// Tabla intermedia que define cómo se ejecuta un ejercicio dentro de una
// rutina concreta: series, repeticiones, peso recomendado, descanso y orden
// de ejecución, dentro del módulo de rutinas de GymProFit.
// ============================================================
@Table(name = "rutina_ejercicio")
public class RutinaEjercicio {

    // Identificador autogenerado de la relación rutina-ejercicio.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Número de series a realizar.
    @Column(nullable = false, columnDefinition = "INT DEFAULT 3")
    private Integer series;

    // Número de repeticiones por serie.
    @Column(nullable = false, columnDefinition = "INT DEFAULT 10")
    private Integer repeticiones;

    // Peso recomendado para el ejercicio (kg).
    @Column(name = "peso_recomendado", precision = 5, scale = 2)
    private BigDecimal pesoRecomendado;

    // Tiempo de descanso entre series, en segundos.
    @Column(name = "tiempo_descanso")
    private Integer tiempoDescanso;

    // Posición/orden del ejercicio dentro de la rutina.
    @Column(nullable = false, columnDefinition = "INT DEFAULT 1")
    private Integer orden;

    // Notas adicionales sobre la ejecución del ejercicio en esta rutina.
    @Column(columnDefinition = "TEXT")
    private String notas;

    // Rutina a la que pertenece este ejercicio.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rutina_id", nullable = false)
    private Rutina rutina;

    // Ejercicio asociado.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ejercicio_id", nullable = false)
    private Ejercicio ejercicio;
}
