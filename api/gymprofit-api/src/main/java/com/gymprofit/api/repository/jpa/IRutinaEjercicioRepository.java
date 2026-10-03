package com.gymprofit.api.repository.jpa;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.gymprofit.api.entity.RutinaEjercicio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// ============================================================
// IRutinaEjercicioRepository — repositorio JPA de la relación rutina-ejercicio
// Gestiona la tabla intermedia que asocia ejercicios a rutinas, con su orden
// dentro de la rutina.
// ============================================================
@Repository
public interface IRutinaEjercicioRepository extends JpaRepository<RutinaEjercicio, Integer> {

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

    // Rutinas distintas que incluyen el ejercicio (una rutina puede repetirlo). GP-085.
    @Query(
            "SELECT COUNT(DISTINCT re.rutina.id) FROM RutinaEjercicio re WHERE re.ejercicio.id = :id")
    long contarRutinasConEjercicio(@Param("id") Integer ejercicioId);

    // Elimina todas las asociaciones de ejercicios de una rutina.
    void deleteByRutinaId(Integer rutinaId);

    // Elimina la asociación de un ejercicio concreto en una rutina.
    void deleteByRutinaIdAndEjercicioId(Integer rutinaId, Integer ejercicioId);

    // Comprueba si un ejercicio ya está asociado a una rutina.
    boolean existsByRutinaIdAndEjercicioId(Integer rutinaId, Integer ejercicioId);
}
