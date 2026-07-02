package com.gymprofit.api.repository.jpa;

import com.gymprofit.api.entity.RutinaEjercicio;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// ============================================================
// IRutinaEjercicioRepository — repositorio JPA de la relación rutina-ejercicio
// Gestiona la tabla intermedia que asocia ejercicios a rutinas, con su orden
// dentro de la rutina. No exportado como recurso REST.
// ============================================================
@Hidden
@Repository
@RepositoryRestResource(exported = false)
public interface IRutinaEjercicioRepository extends CrudRepository<RutinaEjercicio, Integer> {

    // Ejercicios asociados a una rutina.
    List<RutinaEjercicio> findByRutinaId(Integer rutinaId);

    // Rutinas en las que aparece un ejercicio concreto.
    List<RutinaEjercicio> findByEjercicioId(Integer ejercicioId);

    // Ejercicios de una rutina ordenados según el campo "orden".
    List<RutinaEjercicio> findByRutinaIdOrderByOrdenAsc(Integer rutinaId);

    // Relación concreta entre una rutina y un ejercicio.
    Optional<RutinaEjercicio> findByRutinaIdAndEjercicioId(Integer rutinaId, Integer ejercicioId);

    // Número de ejercicios que tiene una rutina.
    Long countByRutinaId(Integer rutinaId);

    // Número de rutinas en las que aparece un ejercicio.
    Long countByEjercicioId(Integer ejercicioId);

    // Elimina todas las asociaciones de ejercicios de una rutina.
    void deleteByRutinaId(Integer rutinaId);

    // Elimina la asociación de un ejercicio concreto en una rutina.
    void deleteByRutinaIdAndEjercicioId(Integer rutinaId, Integer ejercicioId);

    // Comprueba si un ejercicio ya está asociado a una rutina.
    boolean existsByRutinaIdAndEjercicioId(Integer rutinaId, Integer ejercicioId);
}
