package com.gymprofit.api.repository.jooq.ejercicio;

import com.gymprofit.api.dto.jooq.EjercicioJooqDTO;

import java.util.List;

// ============================================================
// IEjercicioJooqRepository — contrato de consultas jOOQ sobre ejercicios
// Define las búsquedas y filtros dinámicos sobre la tabla de ejercicios
// (grupo muscular, dificultad, calorías, etc.) usados por catálogo y admin.
// ============================================================
public interface IEjercicioJooqRepository {

    // Devuelve todos los ejercicios sin filtrar.
    List<EjercicioJooqDTO> findAll();

    // Devuelve solo los ejercicios activos, ordenados.
    List<EjercicioJooqDTO> findActivos();

    // Filtra ejercicios por grupo muscular y dificultad exactos.
    List<EjercicioJooqDTO> findByGrupoMuscularAndDificultad(String grupoMuscular, String dificultad);

    // Filtra ejercicios cuyo gasto calórico está entre min y max.
    List<EjercicioJooqDTO> findByCaloriasQuemadasBetween(Integer min, Integer max);

    // Búsqueda combinada con filtros opcionales para el catálogo de usuario.
    List<EjercicioJooqDTO> busquedaAvanzada(String nombre, String grupoMuscular, String dificultad, Integer caloriasMax);

    // Búsqueda combinada con filtros opcionales (incluye activo) para el panel admin.
    List<EjercicioJooqDTO> busquedaAdmin(String nombre, String grupoMuscular, String dificultad, Boolean activo);
}
