package com.gymprofit.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Entity
// ============================================================
// SesionEntrenamiento — registro de una sesión de entrenamiento realizada
// Guarda cuándo un usuario ha realizado (o está realizando) un entrenamiento,
// opcionalmente ligado a una Rutina, con duración, calorías quemadas y
// estado de finalización, para el historial de actividad en GymProFit.
// ============================================================
@Table(name = "sesiones_entrenamiento")
public class SesionEntrenamiento {

    // Identificador autogenerado de la sesión.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Fecha y hora de inicio de la sesión.
    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    // Fecha y hora de fin de la sesión.
    @Column(name = "fecha_fin", nullable = false)
    private LocalDateTime fechaFin;

    // Duración total de la sesión en minutos.
    @Column(name = "duracion_minutos")
    private Integer duracionMinutos;

    // Calorías estimadas quemadas durante la sesión.
    @Column(name = "calorias_quemadas")
    private Integer caloriasQuemadas;

    // Notas adicionales sobre la sesión.
    @Column(columnDefinition = "TEXT")
    private String notas;

    // Indica si la sesión se completó por entero.
    @Column(columnDefinition = "TINYINT(1) DEFAULT 0")
    private Boolean completada;

    // Usuario que ha realizado la sesión.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    // Rutina seguida en esta sesión (opcional, puede ser entrenamiento libre).
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "rutina_id")
    private Rutina rutina;
}
