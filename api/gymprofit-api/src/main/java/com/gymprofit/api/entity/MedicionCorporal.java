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
@Table(name = "mediciones_corporales")
public class MedicionCorporal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal peso;

    @Column(precision = 3, scale = 2)
    private BigDecimal altura;

    @Column(precision = 4, scale = 2)
    private BigDecimal imc;

    @Column(name = "grasa_corporal", precision = 4, scale = 2)
    private BigDecimal grasaCorporal;

    @Column(name = "masa_muscular", precision = 4, scale = 2)
    private BigDecimal masaMuscular;

    @Column(precision = 5, scale = 2)
    private BigDecimal cintura;

    @Column(precision = 5, scale = 2)
    private BigDecimal pecho;

    @Column(precision = 5, scale = 2)
    private BigDecimal brazos;

    @Column(precision = 5, scale = 2)
    private BigDecimal piernas;

    @Column(columnDefinition = "TEXT")
    private String notas;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;
}