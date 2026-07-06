package com.gymprofit.api.entity;

import com.gymprofit.api.enums.TipoLogro;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
// ============================================================
// Logro — definición de un logro/insignia desbloqueable por el usuario
// Catálogo de logros de gamificación de la app (ej. constancia, fuerza...),
// clasificados por TipoLogro, que se asocian a los usuarios que los
// consiguen.
// ============================================================
@Entity
@Table(name = "logros")
public class Logro {

    // Identificador autogenerado del logro.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Nombre del logro.
    @Column(nullable = false, length = 100)
    private String nombre;

    // Descripción de cómo se consigue el logro.
    @Column(nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    // Tipo/categoría del logro.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoLogro tipo;

    // Traducción EN del nombre (null = sin traducción, se sirve el ES).
    @Column(name = "nombre_en", length = 100)
    private String nombreEn;

    // Traducción EN de la descripción.
    @Column(name = "descripcion_en", columnDefinition = "TEXT")
    private String descripcionEn;
}
