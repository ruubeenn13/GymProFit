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
@Entity
@Table(name = "comidas")
public class Comida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column
    private LocalDateTime fecha;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_comida", nullable = false)
    private TipoComida tipoComida;

    @Column(name = "total_calorias")
    private Integer totalCalorias;

    @Column(name = "total_proteinas", precision = 5, scale = 2)
    private BigDecimal totalProteinas;

    @Column(name = "total_carbohidratos", precision = 5, scale = 2)
    private BigDecimal totalCarbohidratos;

    @Column(name = "total_grasas", precision = 5, scale = 2)
    private BigDecimal totalGrasas;

    @Column(columnDefinition = "TEXT")
    private String notas;
}
