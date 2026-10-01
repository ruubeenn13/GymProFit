package com.gymprofit.api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// ============================================================
// ProductoOff — producto de Open Food Facts que se vende en España (GP-164)
// Copia tal cual de la exportación de Open Food Facts (ODbL), valores por 100 g.
// No es un alimento: lo es cuando alguien lo elige y se materializa en `alimentos`.
// La escribe solo la importación (DEC-041); la API la lee para buscar y materializar.
// ============================================================
@NoArgsConstructor
@Setter
@Getter
@Entity
@Table(name = "productos_off")
public class ProductoOff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Código de barras (EAN/UPC), único.
    @Column(nullable = false, length = 32, unique = true)
    private String codigo;

    @Column(nullable = false, length = 200)
    private String nombre;

    @Column(length = 100)
    private String marca;

    @Column(nullable = false, precision = 5, scale = 1)
    private BigDecimal kcal;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal proteinas;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal carbohidratos;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal grasas;

    @Column(precision = 5, scale = 2)
    private BigDecimal fibra;

    // Ración que declara el producto, si la trae.
    @Column(name = "racion_gramos", precision = 6, scale = 1)
    private BigDecimal racionGramos;

    @Column(name = "racion_texto", length = 60)
    private String racionTexto;

    // Contenido del envase tal como lo escribe Open Food Facts («300 g»).
    @Column(length = 60)
    private String envase;

    // Cuántas personas distintas lo han escaneado en Open Food Facts: su popularidad.
    @Column(nullable = false)
    private int escaneos;

    @Column(nullable = false)
    private LocalDateTime actualizado;
}
