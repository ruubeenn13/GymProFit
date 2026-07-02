package com.gymprofit.api.repository.jpa;

import com.gymprofit.api.entity.ProgresoEjercicio;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// ============================================================
// IProgresoEjercicioRepository — repositorio JPA del progreso de ejercicio
// Da acceso a los registros de progreso (peso, repeticiones, etc.) que un
// usuario ha ido guardando para cada ejercicio a lo largo del tiempo.
// No exportado como recurso REST (exported = false); solo uso interno vía Service.
// ============================================================
@Hidden
@Repository
@RepositoryRestResource(exported = false)
public interface IProgresoEjercicioRepository extends JpaRepository<ProgresoEjercicio, Integer> {

    // Todos los registros de progreso de un usuario.
    List<ProgresoEjercicio> findByUsuarioId(Integer usuarioId);

    // Todos los registros de progreso de un ejercicio concreto.
    List<ProgresoEjercicio> findByEjercicioId(Integer ejercicioId);

    // Registros de progreso de un usuario para un ejercicio concreto.
    List<ProgresoEjercicio> findByUsuarioIdAndEjercicioId(Integer usuarioId, Integer ejercicioId);

    // Progreso de un usuario ordenado del más reciente al más antiguo.
    List<ProgresoEjercicio> findByUsuarioIdOrderByFechaDesc(Integer usuarioId);

    // Último registro de progreso de un usuario para un ejercicio (el más reciente por fecha).
    Optional<ProgresoEjercicio> findFirstByUsuarioIdAndEjercicioIdOrderByFechaDesc(Integer usuarioId, Integer ejercicioId);

    // Número de registros de progreso de un usuario.
    Long countByUsuarioId(Integer usarioId);

    // Número de registros de progreso de un ejercicio.
    Long countByEjercicioId(Integer ejercicioId);

    // Borra todo el progreso registrado de un usuario.
    void deleteByUsuarioId(Integer usuarioId);

    // Borra el progreso de un usuario para un ejercicio concreto.
    void deleteByUsuarioIdAndEjercicioId(Integer usuarioId, Integer ejercicioId);

    // Comprueba si existe progreso registrado de un usuario para un ejercicio.
    boolean existsByUsuarioIdAndEjercicioId(Integer usuarioId, Integer ejercicioId);

    // Consulta JPQL: progreso de un usuario en un ejercicio ordenado por fecha descendente.
    @Query("SELECT p " +
            "FROM ProgresoEjercicio p " +
            "WHERE p.usuario.id = :usuarioId " +
            "AND p.ejercicio.id = :ejercicioId " +
            "ORDER BY p.fecha DESC")
    List<ProgresoEjercicio> getProgresoByUsuarioAndEjercicio(@Param("usuarioId") Integer usuarioId, @Param("ejercicioId") Integer ejercicioId);
}
