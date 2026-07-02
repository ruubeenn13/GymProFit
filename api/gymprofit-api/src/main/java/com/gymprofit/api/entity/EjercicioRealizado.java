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
// EjercicioRealizado — registro real de un ejercicio dentro de una sesión
// Guarda el desempeño concreto (series, repeticiones, peso, tiempo) de un
// Ejercicio ejecutado en una SesionEntrenamiento, permitiendo el
// seguimiento de progreso del usuario.
// ============================================================
@Entity
@Table(name = "ejercicios_realizados")
public class EjercicioRealizado {

    // Identificador autogenerado del registro.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Número de series completadas.
    @Column(name = "series_completadas")
    private Integer seriesCompletadas;

    // Repeticiones reales realizadas.
    @Column(name = "repeticiones_rutinas")
    private Integer repeticionesReales;

    // Peso utilizado en el ejercicio.
    @Column(name = "peso_usado", precision = 5, scale = 2)
    private BigDecimal pesoUsado;

    // Tiempo empleado en segundos.
    @Column(name = "tiempo_segundos")
    private Integer tiempoSegundos;

    // Notas u observaciones del usuario sobre la ejecución.
    @Column(columnDefinition = "TEXT")
    private String notas;

    // Sesión de entrenamiento a la que pertenece este registro.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sesion_id", nullable = false)
    private SesionEntrenamiento sesion;

    // Ejercicio del catálogo realizado.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ejercicio_id", nullable = false)
    private Ejercicio ejercicio;
}
