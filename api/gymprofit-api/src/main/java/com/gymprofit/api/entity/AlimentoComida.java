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
// AlimentoComida — línea de detalle que asocia un Alimento a una Comida
// Entidad intermedia (many-to-one x2) que representa la cantidad concreta
// de un alimento consumida dentro de una comida, junto con las calorías
// totales calculadas para esa cantidad.
// ============================================================
@Entity
@Table(name = "alimentos_comida")
public class AlimentoComida {

    // Identificador autogenerado de la línea.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Cantidad consumida del alimento, en gramos.
    @Column(name = "cantidad_gramos", nullable = false, precision = 6, scale = 2)
    private BigDecimal cantidadGramos;

    // Calorías totales resultantes para la cantidad indicada.
    @Column(name = "calorias_totales")
    private Integer caloriasTotales;

    // Comida a la que pertenece esta línea.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "comida_id", nullable = false)
    private Comida comida;

    // Alimento de referencia consumido.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "alimento_id", nullable = false)
    private Alimento alimento;
}
