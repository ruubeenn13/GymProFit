package com.gymprofit.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Entity
// ============================================================
// ProgresoEjercicio — mejores marcas alcanzadas por un usuario en un ejercicio
// Guarda el récord personal (peso, repeticiones o tiempo) conseguido por el
// usuario en un ejercicio concreto, permitiendo mostrar la evolución de
// rendimiento a lo largo del tiempo en GymProFit.
// ============================================================
@Table(name = "progreso_ejercicios")
public class ProgresoEjercicio {

    // Identificador autogenerado del registro de progreso.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Fecha en la que se logró esta marca.
    @Column
    private LocalDateTime fecha;

    // Mejor peso levantado registrado (kg).
    @Column(name = "mejor_peso", precision = 5, scale = 2)
    private BigDecimal mejorPeso;

    // Mayor número de repeticiones registrado.
    @Column(name = "mejor_repeticiones")
    private Integer mejorRepeticiones;

    // Mejor tiempo registrado, en segundos (para ejercicios cronometrados).
    @Column(name = "mejor_tiempo_segundos")
    private Integer mejorTiempoSegundos;

    // Notas adicionales sobre la marca conseguida.
    @Column(columnDefinition = "TEXT")
    private String notas;

    // Usuario que ha conseguido esta marca.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    // Ejercicio al que corresponde el progreso.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ejercicio_id", nullable = false)
    private Ejercicio ejercicio;
}
