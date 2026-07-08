package com.gymprofit.api.entity;

import com.gymprofit.api.enums.Dificultad;
import com.gymprofit.api.enums.GrupoMuscular;
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
// Ejercicio — catálogo de ejercicios disponibles en la app
// Define un ejercicio físico (nombre, grupo muscular, dificultad,
// instrucciones...) que puede añadirse a rutinas (RutinaEjercicio) y
// registrarse como realizado en una sesión (EjercicioRealizado).
// ============================================================
@Entity
@Table(name = "ejercicios")
public class Ejercicio {

    // Identificador autogenerado del ejercicio.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Nombre del ejercicio.
    @Column(nullable = false, length = 100)
    private String nombre;

    // Descripción general del ejercicio.
    @Column(columnDefinition = "TEXT")
    private String descripcion;

    // Grupo muscular principal trabajado (enum grueso, para el chip de filtro).
    @Enumerated(EnumType.STRING)
    @Column(name = "grupo_muscular", nullable = false)
    private GrupoMuscular grupoMuscular;

    // Músculo primario REAL y preciso (free-exercise-db), ej. "Aductores",
    // "Cuádriceps". Se muestra en el detalle; null = solo se conoce el grupo.
    @Column(name = "musculo_primario", length = 60)
    private String musculoPrimario;

    // Traducción EN del músculo primario.
    @Column(name = "musculo_primario_en", length = 60)
    private String musculoPrimarioEn;

    // Nivel de dificultad del ejercicio.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Dificultad dificultad;

    // URL de la imagen ilustrativa del ejercicio (fotograma 1).
    @Column(name = "imagen_url")
    private String imagenUrl;

    // Fotograma 2 de la demostración (free-exercise-db); la app alterna
    // ambos para animar el ejercicio. NULL = solo imagen estática.
    @Column(name = "imagen_url_2")
    private String imagenUrl2;

    // Instrucciones detalladas de ejecución.
    @Column(columnDefinition = "TEXT")
    private String instrucciones;

    // Calorías estimadas quemadas al realizar el ejercicio.
    @Column(name = "calorias_quemadas")
    private Integer caloriasQuemadas;

    // Equipo/material necesario para realizarlo.
    @Column(name = "equipo_necesario")
    private String equipoNecesario;

    // Indica si el ejercicio está activo/visible (borrado lógico).
    @Column(columnDefinition = "TINYINT(1) DEFAULT 1")
    private Boolean activo;

    // Id del ejercicio en la API de wger (null = creado a mano). Clave de
    // upsert idempotente del import del catálogo (índice único).
    @Column(name = "wger_id", unique = true)
    private Integer wgerId;

    // Identificador estable de free-exercise-db (slug); clave de upsert del catálogo.
    @Column(name = "fed_id", length = 120, unique = true)
    private String fedId;

    // Traducción EN del nombre (null = sin traducción, se sirve el ES).
    @Column(name = "nombre_en", length = 100)
    private String nombreEn;

    // Traducción EN de la descripción.
    @Column(name = "descripcion_en", columnDefinition = "TEXT")
    private String descripcionEn;

    // Traducción EN de las instrucciones de ejecución.
    @Column(name = "instrucciones_en", columnDefinition = "TEXT")
    private String instruccionesEn;

    // Traducción EN del equipo necesario.
    @Column(name = "equipo_necesario_en")
    private String equipoNecesarioEn;
}