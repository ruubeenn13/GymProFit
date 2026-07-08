package com.gymprofit.api.repository.jpa;

import com.gymprofit.api.entity.Ejercicio;
import com.gymprofit.api.enums.Dificultad;
import com.gymprofit.api.enums.GrupoMuscular;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

// ============================================================
// IEjercicioRepository — repositorio JPA de la entidad Ejercicio
// Acceso al catálogo de ejercicios disponibles (por grupo muscular y dificultad) usados en las rutinas.
// ============================================================
@Hidden
@Repository
@RepositoryRestResource(exported = false)
public interface IEjercicioRepository extends JpaRepository<Ejercicio, Integer> {

    // Busca un ejercicio por su id en wger (clave de upsert del import externo legado).
    java.util.Optional<Ejercicio> findByWgerId(Integer wgerId);

    // Busca un ejercicio por su id de free-exercise-db (clave de upsert del import base).
    java.util.Optional<Ejercicio> findByFedId(String fedId);

    // Desactiva los ejercicios activos que ya NO están en el import de free-exercise-db
    // (incluye las filas viejas de wger sin fed_id): el catálogo queda solo con los vistos.
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE Ejercicio e SET e.activo = false " +
            "WHERE e.activo = true AND (e.fedId IS NULL OR e.fedId NOT IN :vistos)")
    int desactivarFedNoVistos(@Param("vistos") java.util.Collection<String> vistos);

    // Busca ejercicios que trabajen un grupo muscular concreto.
    List<Ejercicio> findByGrupoMuscular(GrupoMuscular grupoMuscular);

    // Busca ejercicios por nivel de dificultad.
    List<Ejercicio> findByDificultad(Dificultad dificultad);

    // Busca ejercicios que combinen grupo muscular y dificultad.
    List<Ejercicio> findByGrupoMuscularAndDificultad(GrupoMuscular grupoMuscular, Dificultad dificultad);

    // Busca los ejercicios marcados como activos.
    List<Ejercicio> findByActivoTrue();

    // Busca ejercicios cuyo nombre contenga el texto dado, sin distinguir mayúsculas/minúsculas.
    List<Ejercicio> findByNombreContainingIgnoreCase(String nombre);

    // Consulta JPQL que devuelve los ejercicios activos de un grupo muscular concreto.
    @Query("SELECT e " +
            "FROM Ejercicio e " +
            "WHERE e.grupoMuscular = :grupo " +
            "AND e.activo = true")
    List<Ejercicio> getEjerciciosActivosPorGrupo(@Param("grupo") GrupoMuscular grupo);

    // Búsqueda paginada del catálogo de ejercicios activos. Filtra opcionalmente
    // por texto en el nombre (ES o EN, sin mayúsculas), grupo muscular y dificultad.
    @Query("SELECT e FROM Ejercicio e " +
            "WHERE e.activo = true " +
            "AND (:q IS NULL OR LOWER(e.nombre) LIKE LOWER(CONCAT('%', :q, '%')) " +
            "     OR LOWER(e.nombreEn) LIKE LOWER(CONCAT('%', :q, '%'))) " +
            "AND (:grupo IS NULL OR e.grupoMuscular = :grupo) " +
            "AND (:dificultad IS NULL OR e.dificultad = :dificultad) " +
            "ORDER BY e.nombre")
    Page<Ejercicio> buscarCatalogo(@Param("q") String q,
                                   @Param("grupo") GrupoMuscular grupo,
                                   @Param("dificultad") Dificultad dificultad,
                                   Pageable pageable);
}
