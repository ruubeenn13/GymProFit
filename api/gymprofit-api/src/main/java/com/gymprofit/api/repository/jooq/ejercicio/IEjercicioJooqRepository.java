package com.gymprofit.api.repository.jooq.ejercicio;

import com.gymprofit.api.dto.jooq.EjercicioJooqDTO;

import java.util.List;

public interface IEjercicioJooqRepository {

    List<EjercicioJooqDTO> findAll();

    List<EjercicioJooqDTO> findActivos();

    List<EjercicioJooqDTO> findByGrupoMuscularAndDificultad(String grupoMuscular, String dificultad);

    List<EjercicioJooqDTO> findByCaloriasQuemadasBetween(Integer min, Integer max);

    List<EjercicioJooqDTO> busquedaAvanzada(String nombre, String grupoMuscular, String dificultad, Integer caloriasMax);
}
