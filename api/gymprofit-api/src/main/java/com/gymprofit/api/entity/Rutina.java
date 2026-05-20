package com.gymprofit.api.entity;

import com.gymprofit.api.enums.Nivel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "rutinas")
public class Rutina {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "duracion_minutos")
    private Integer duracionMinutos;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Nivel nivel;

    @Column(name = "es_predefinida", columnDefinition = "TINYINT(1) DEFAULT 0")
    private Boolean esPredefinida;

    @Column(length = 50)
    private String categoria;

    @Column(name = "dias_semana", length = 100)
    private String diasSemana;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @Column(columnDefinition = "TINYINT(1) DEFAULT 1")
    private Boolean activa;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @org.hibernate.annotations.Formula("(SELECT COUNT(*) FROM rutina_ejercicio re WHERE re.rutina_id = id)")
    private Integer numEjercicios;

    @org.hibernate.annotations.Formula("(SELECT COALESCE(SUM(e.calorias_quemadas), 0) FROM rutina_ejercicio re JOIN ejercicios e ON e.id = re.ejercicio_id WHERE re.rutina_id = id)")
    private Integer caloriasAproximadas;
}