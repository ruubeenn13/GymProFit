package com.gymprofit.api.entity;

import com.gymprofit.api.enums.MedidaSerie;
import com.gymprofit.api.enums.PorLado;
import com.gymprofit.api.enums.TipoEjercicioRutina;
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

    // --- Opcionales del catálogo de plantillas (GP-074). Nulos en las rutinas de antes. ---

    // Rango de repeticiones (o de segundos, si la medida es SEGUNDOS). repeticiones lleva
    // el máximo, para que una build vieja enseñe algo con sentido.
    @Column(name = "repeticiones_min")
    private Integer repeticionesMin;

    @Column(name = "repeticiones_max")
    private Integer repeticionesMax;

    @Enumerated(EnumType.STRING)
    @Column(length = 15)
    private MedidaSerie medida;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private TipoEjercicioRutina tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "por_lado", length = 10)
    private PorLado porLado;

    // Nota en inglés (solo las plantillas; una copia lleva la nota en el idioma en que se hizo).
    @Column(name = "notas_en", columnDefinition = "TEXT")
    private String notasEn;

    // Rutina a la que pertenece este ejercicio.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rutina_id", nullable = false)
    private Rutina rutina;

    // Ejercicio asociado.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ejercicio_id", nullable = false)
    private Ejercicio ejercicio;
}
