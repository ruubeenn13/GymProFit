package com.gymprofit.api.entity;

import com.gymprofit.api.enums.EquipamientoPrograma;
import com.gymprofit.api.enums.Nivel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
// ============================================================
// Programa — plan de entrenamiento de varios días del catálogo (GP-074)
// Su semana es la secuencia ordenada de rutinas plantilla, que puede repetir alguna.
// No tiene días fijos por rutina: Para empezar y Cuerpo completo van alternando su
// ciclo. Lo siembra la migración V202609292002 desde el catálogo v1.
// ============================================================
@Table(name = "programas")
public class Programa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Código estable del catálogo (GIM-CC, MAN-TP…): es el id público del programa.
    @Column(nullable = false, unique = true, length = 20)
    private String codigo;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(name = "nombre_en", length = 100)
    private String nombreEn;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "descripcion_en", columnDefinition = "TEXT")
    private String descripcionEn;

    // Nivel del programa: PRINCIPIANTE o INTERMEDIO. El avanzado usa los de intermedio.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Nivel nivel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EquipamientoPrograma equipamiento;

    @Column(name = "dias_min", nullable = false)
    private Integer diasMin;

    @Column(name = "dias_max", nullable = false)
    private Integer diasMax;

    @Column(nullable = false)
    private Boolean activo = Boolean.TRUE;

    // La semana, en orden.
    @OneToMany(mappedBy = "programa", fetch = FetchType.LAZY)
    @OrderBy("posicion ASC")
    private List<ProgramaRutina> semana = new ArrayList<>();
}
