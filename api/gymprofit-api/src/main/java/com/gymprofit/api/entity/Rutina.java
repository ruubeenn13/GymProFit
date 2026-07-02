package com.gymprofit.api.entity;

import com.gymprofit.api.enums.Nivel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
// ============================================================
// Rutina — plan de entrenamiento compuesto por varios ejercicios
// Representa una rutina de entrenamiento (predefinida por la app o creada
// por un usuario), con su nivel de dificultad, días de la semana asignados
// y campos calculados (número de ejercicios y calorías aproximadas) para
// mostrar resúmenes en GymProFit.
// ============================================================
@Table(name = "rutinas")
public class Rutina {

    // Identificador autogenerado de la rutina.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Nombre de la rutina.
    @Column(nullable = false, length = 100)
    private String nombre;

    // Descripción detallada de la rutina.
    @Column(columnDefinition = "TEXT")
    private String descripcion;

    // Duración estimada de la rutina en minutos.
    @Column(name = "duracion_minutos")
    private Integer duracionMinutos;

    // Nivel de dificultad de la rutina.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Nivel nivel;

    // Indica si es una rutina predefinida por la app (no creada por un usuario).
    @Column(name = "es_predefinida", columnDefinition = "TINYINT(1) DEFAULT 0")
    private Boolean esPredefinida;

    // Categoría de la rutina (fuerza, cardio, etc.).
    @Column(length = 50)
    private String categoria;

    // Días de la semana en los que se recomienda realizar la rutina.
    @Column(name = "dias_semana", length = 100)
    private String diasSemana;

    // Fecha de creación de la rutina.
    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    // Indica si la rutina está activa (visible/usable) o desactivada.
    @Column(columnDefinition = "TINYINT(1) DEFAULT 1")
    private Boolean activa;

    // Usuario propietario de la rutina (null si es predefinida del sistema).
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    // Campo calculado (no persistido): número de ejercicios asociados a la rutina.
    @org.hibernate.annotations.Formula("(SELECT COUNT(*) FROM rutina_ejercicio re WHERE re.rutina_id = id)")
    private Integer numEjercicios;

    // Campo calculado (no persistido): calorías aproximadas que quema la rutina completa.
    @org.hibernate.annotations.Formula("(SELECT COALESCE(SUM(re.series * re.repeticiones * e.calorias_quemadas), 0) FROM rutina_ejercicio re JOIN ejercicios e ON e.id = re.ejercicio_id WHERE re.rutina_id = id)")
    private Integer caloriasAproximadas;
}