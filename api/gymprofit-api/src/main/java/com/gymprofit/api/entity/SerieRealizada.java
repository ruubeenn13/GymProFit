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
// SerieRealizada — una serie concreta de un ejercicio dentro de una sesión.
//
// Es el dato que faltaba para que la app sirva de algo: antes solo se guardaba
// UN peso por ejercicio, así que un 4x8 subiendo carga no se podía registrar y
// no había forma de ver progresión. Cada fila de aquí es una serie real, con
// sus repeticiones y su peso.
//
// Ver la migración V202609182100__Series_realizadas.sql.
// ============================================================
@Entity
@Table(name = "series_realizadas")
public class SerieRealizada {

    // Identificador autogenerado de la serie.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Orden dentro del ejercicio, empezando en 1. Se guarda y no se deduce del
    // id porque es lo que el usuario ve como "serie 1, serie 2…".
    @Column(nullable = false)
    private Integer numero;

    // Repeticiones REALES. No tienen por qué coincidir con las que pedía la
    // rutina: fallar la última serie es información, no un error.
    @Column(nullable = false)
    private Integer repeticiones;

    // Peso de esta serie. Nulo en ejercicios de peso corporal.
    @Column(precision = 5, scale = 2)
    private BigDecimal peso;

    // Si el usuario llegó a marcarla. Distingue "no la hice" de "no la apunté".
    @Column(nullable = false)
    private Boolean completada = Boolean.TRUE;

    // Ejercicio de la sesión al que pertenece.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ejercicio_realizado_id", nullable = false)
    private EjercicioRealizado ejercicioRealizado;
}
