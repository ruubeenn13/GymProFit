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
// MedicionCorporal — registro puntual de las métricas corporales de un usuario
// Almacena peso, altura, IMC y otras medidas antropométricas (grasa corporal,
// masa muscular, perímetros) tomadas en una fecha concreta, para llevar el
// seguimiento de la evolución física del usuario en GymProFit.
// ============================================================
@Table(name = "mediciones_corporales")
public class MedicionCorporal {

    // Identificador autogenerado de la medición.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Fecha y hora en la que se tomó la medición.
    @Column(nullable = false)
    private LocalDateTime fecha;

    // Peso corporal en kg.
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal peso;

    // Altura en cm.
    @Column(precision = 5, scale = 2)
    private BigDecimal altura;

    // Índice de masa corporal calculado.
    @Column(precision = 4, scale = 2)
    private BigDecimal imc;

    // Porcentaje de grasa corporal.
    @Column(name = "grasa_corporal", precision = 4, scale = 2)
    private BigDecimal grasaCorporal;

    // Porcentaje de masa muscular.
    @Column(name = "masa_muscular", precision = 4, scale = 2)
    private BigDecimal masaMuscular;

    // Perímetro de cintura en cm.
    @Column(precision = 5, scale = 2)
    private BigDecimal cintura;

    // Perímetro de pecho en cm.
    @Column(precision = 5, scale = 2)
    private BigDecimal pecho;

    // Perímetro de brazos en cm.
    @Column(precision = 5, scale = 2)
    private BigDecimal brazos;

    // Perímetro de piernas en cm.
    @Column(precision = 5, scale = 2)
    private BigDecimal piernas;

    // Notas u observaciones adicionales del usuario sobre la medición.
    @Column(columnDefinition = "TEXT")
    private String notas;

    // Usuario al que pertenece esta medición.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;
}