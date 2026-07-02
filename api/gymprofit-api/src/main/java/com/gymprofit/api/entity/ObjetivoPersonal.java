package com.gymprofit.api.entity;

import com.gymprofit.api.enums.TipoObjetivo;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
// ============================================================
// ObjetivoPersonal — meta fitness que un usuario se marca y sigue en el tiempo
// Define un objetivo (perder peso, ganar masa muscular, etc.) con un valor
// inicial y uno a alcanzar, plazo límite y estado de cumplimiento, para que
// el usuario pueda hacer seguimiento de su progreso en GymProFit.
// ============================================================
@Table(name = "objetivos_personales")
public class ObjetivoPersonal {

    // Identificador autogenerado del objetivo.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Tipo de objetivo (peso, grasa corporal, fuerza, etc.).
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_objetivo", nullable = false)
    private TipoObjetivo tipoObjetivo;

    // Descripción textual del objetivo introducida por el usuario.
    @Column(nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    // Valor actual medido para el objetivo (se va actualizando con el progreso).
    @Column(name = "valor_actual", precision = 10, scale = 2)
    private BigDecimal valorActual;

    // Valor que se pretende alcanzar.
    @Column(name = "valor_objetivo", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorObjetivo;

    // Unidad de medida del valor (kg, %, reps, etc.).
    @Column(length = 20)
    private String unidad;

    // Fecha en la que se estableció el objetivo.
    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    // Fecha límite para alcanzar el objetivo (opcional).
    @Column(name = "fecha_limite")
    private LocalDate fechaLimite;

    // Indica si el objetivo ya ha sido completado.
    @Column(columnDefinition = "TINYINT(1) DEFAULT 0")
    private Boolean completado;

    // Fecha y hora en la que se marcó como completado.
    @Column(name = "fecha_completado")
    private LocalDateTime fechaCompletado;

    // Usuario propietario del objetivo.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;
}