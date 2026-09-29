package com.gymprofit.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
// ============================================================
// ProgramaRutina — un día de la semana de un programa (GP-074)
// La clave natural es (programa, posición) y no (programa, rutina): el programa de
// 6 días lleva Empuje y Tirón dos veces.
// ============================================================
@Table(name = "programa_rutina")
public class ProgramaRutina {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "programa_id", nullable = false)
    private Programa programa;

    // Rutina plantilla que toca ese día.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rutina_id", nullable = false)
    private Rutina rutina;

    // Orden dentro de la semana, empezando en 1.
    @Column(nullable = false)
    private Integer posicion;
}
