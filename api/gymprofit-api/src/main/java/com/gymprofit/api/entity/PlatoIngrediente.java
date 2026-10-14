package com.gymprofit.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

// ============================================================
// PlatoIngrediente — un ingrediente de un plato propio (lote 1.6.6, DEC-047)
// Un alimento que la cuenta ve, con su cantidad como en una línea de comida: los gramos
// y, si se eligió, la ración y cuántas. Los valores del plato salen de aquí al guardarlo
// y no se vuelven a calcular hasta que se guarde otra vez.
// ============================================================
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Entity
@Table(name = "plato_ingredientes")
public class PlatoIngrediente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // El plato (un Alimento con plato_raciones).
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "plato_id")
    private Alimento plato;

    // El ingrediente: del catálogo o de la cuenta, nunca otro plato.
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "alimento_id")
    private Alimento alimento;

    // Mandan los gramos, como en una línea (DEC-042).
    @Column(name = "cantidad_gramos", nullable = false, precision = 6, scale = 2)
    private BigDecimal cantidadGramos;

    // La ración con la que se eligió, opcional; solo dice cómo enseñarlo.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "racion_id")
    private AlimentoRacion racion;

    @Column(name = "raciones", precision = 5, scale = 2)
    private BigDecimal raciones;

    // Posición en la lista del plato.
    @Column(nullable = false)
    private Integer orden;
}
