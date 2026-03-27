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
@Table(name = "objetivos_personales")
public class ObjetivoPersonal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_objetivo", nullable = false)
    private TipoObjetivo tipoObjetivo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "valor_actual", precision = 10, scale = 2)
    private BigDecimal valorActual;

    @Column(name = "valor_objetivo", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorObjetivo;

    @Column(length = 20)
    private String unidad;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_limite")
    private LocalDate fechaLimite;

    @Column(columnDefinition = "TINYINT(1) DEFAULT 0")
    private Boolean completado;

    @Column(name = "fecha_completado")
    private LocalDateTime fechaCompletado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;
}