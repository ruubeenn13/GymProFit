package com.gymprofit.api.entity;

import com.gymprofit.api.enums.TipoComida;
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
// ============================================================
// Comida — registro de una comida del usuario (desayuno, comida, cena...)
// Agrupa uno o varios AlimentoComida y almacena los totales nutricionales
// acumulados de esa comida, dentro del sistema de seguimiento nutricional
// tipo MyFitnessPal de GymProFit.
// ============================================================
@Entity
@Table(name = "comidas")
public class Comida {

    // Identificador autogenerado de la comida.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Fecha y hora en que se registró la comida.
    @Column
    private LocalDateTime fecha;

    // Tipo de comida (desayuno, almuerzo, cena, snack...).
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_comida", nullable = false)
    private TipoComida tipoComida;

    // Calorías totales acumuladas de los alimentos de esta comida.
    @Column(name = "total_calorias")
    private Integer totalCalorias;

    // Proteínas totales acumuladas (g).
    @Column(name = "total_proteinas", precision = 5, scale = 2)
    private BigDecimal totalProteinas;

    // Carbohidratos totales acumulados (g).
    @Column(name = "total_carbohidratos", precision = 5, scale = 2)
    private BigDecimal totalCarbohidratos;

    // Grasas totales acumuladas (g).
    @Column(name = "total_grasas", precision = 5, scale = 2)
    private BigDecimal totalGrasas;

    // Notas u observaciones libres del usuario sobre la comida.
    @Column(columnDefinition = "TEXT")
    private String notas;

    // Usuario propietario de la comida.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;
}
