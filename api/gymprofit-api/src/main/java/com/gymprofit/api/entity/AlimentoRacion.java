package com.gymprofit.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

// ============================================================
// AlimentoRacion — una ración con nombre de un alimento (GP-127)
// «1 unidad mediana» de 118 g. Los valores del alimento siguen siendo por 100 g:
// la ración solo dice cuántos gramos son, y de dónde sale ese peso.
// ============================================================
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Entity
@Table(name = "alimento_raciones")
public class AlimentoRacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "alimento_id")
    private Alimento alimento;

    // Nombre de la ración en español, sin los gramos («1 rebanada»).
    @Column(nullable = false, length = 60)
    private String nombre;

    // Nombre en inglés.
    @Column(name = "nombre_en", nullable = false, length = 60)
    private String nombreEn;

    // Peso de la ración en gramos.
    @Column(nullable = false, precision = 6, scale = 1)
    private BigDecimal gramos;

    // De dónde sale el peso (p. ej. «USDA FDC 173944: 1 medium»).
    @Column(nullable = false, length = 255)
    private String fuente;

    // Posición en la lista del alimento.
    @Column(nullable = false)
    private Integer orden;
}
