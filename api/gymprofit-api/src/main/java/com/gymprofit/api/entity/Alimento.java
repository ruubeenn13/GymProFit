package com.gymprofit.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
// ============================================================
// Alimento — alimento del catálogo de nutrición (mapa a tabla alimentos)
// Representa un alimento con sus macronutrientes por porción. Puede ser
// del catálogo global (usuario null) o creado por un usuario concreto,
// y se usa como referencia dentro de AlimentoComida.
// ============================================================
@Entity
@Table(name = "alimentos")
public class Alimento {

    // Identificador autogenerado del alimento.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Nombre del alimento.
    @Column(nullable = false, length = 100)
    private String nombre;

    // Categoría del alimento (ej. lácteos, frutas...).
    @Column(length = 50)
    private String categoria;

    // Calorías por porción.
    @Column(nullable = false)
    private Integer calorias;

    // Gramos de proteínas por porción.
    @Column(precision = 5, scale = 2)
    private BigDecimal proteinas;

    // Gramos de carbohidratos por porción.
    @Column(precision = 5, scale = 2)
    private BigDecimal carbohidratos;

    // Gramos de grasas por porción.
    @Column(precision = 5, scale = 2)
    private BigDecimal grasas;

    // Gramos de fibra por porción.
    @Column(precision = 5, scale = 2)
    private BigDecimal fibra;

    // Tamaño de la porción de referencia en gramos.
    @Column(name = "porcion_gramos")
    private Integer porcionGramos;

    // Descripción libre del alimento.
    @Column(columnDefinition = "TEXT")
    private String descripcion;

    // Indica si el alimento está activo/visible (borrado lógico).
    @Column(columnDefinition = "TINYINT(1) DEFAULT 1")
    private Boolean activo;

    // Usuario propietario si el alimento fue creado por un usuario (null = catálogo global).
    @ManyToOne(optional = true)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    // Código de barras. Único por dueño (GP-160, DEC-040): en el catálogo, el de un
    // producto; en un alimento de usuario, el que él le puso.
    @Column(length = 32)
    private String barcode;

    // Marca/fabricante del producto (Open Food Facts).
    @Column(length = 100)
    private String marca;

    // Traducción EN del nombre (null = sin traducción, se sirve el ES).
    @Column(name = "nombre_en", length = 100)
    private String nombreEn;

    // Traducción EN de la categoría.
    @Column(name = "categoria_en", length = 50)
    private String categoriaEn;

    // Traducción EN de la descripción.
    @Column(name = "descripcion_en", columnDefinition = "TEXT")
    private String descripcionEn;

    // De dónde salen los datos (GP-127): CIQUAL, USDA, OFF; null si se hizo a mano.
    @Column(length = 16)
    private String fuente;

    // Código del alimento en su fuente; con la fuente, identifica la fila.
    @Column(name = "codigo_origen", length = 32)
    private String codigoOrigen;

    // true en los básicos, curados uno a uno; false en el resto.
    @Column(nullable = false)
    private boolean revisado;

    // Raciones con nombre y peso (GP-127). Por lotes, para que un listado no haga una
    // consulta por alimento.
    @OneToMany(mappedBy = "alimento", fetch = FetchType.LAZY)
    @OrderBy("orden ASC")
    @org.hibernate.annotations.BatchSize(size = 100)
    private List<AlimentoRacion> raciones = new ArrayList<>();
}
