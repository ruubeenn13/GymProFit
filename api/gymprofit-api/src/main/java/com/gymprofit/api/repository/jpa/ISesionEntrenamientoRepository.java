package com.gymprofit.api.repository.jpa;

import com.gymprofit.api.entity.SesionEntrenamiento;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

// ============================================================
// ISesionEntrenamientoRepository — repositorio JPA de sesiones de entrenamiento
// Acceso a las sesiones (entrenos) realizadas por los usuarios, con filtros
// por rutina, rango de fechas y estado de completado. No exportado como REST.
// ============================================================
@Hidden
@Repository
@RepositoryRestResource(exported = false)
public interface ISesionEntrenamientoRepository extends JpaRepository<SesionEntrenamiento, Integer> {

    // Sesiones de un usuario.
    List<SesionEntrenamiento> findByUsuarioId(Integer usuarioId);

    // Sesiones asociadas a una rutina.
    List<SesionEntrenamiento> findByRutinaId(Integer rutinaId);

    // Sesiones marcadas como completadas.
    List<SesionEntrenamiento> findByCompletadaTrue();

    // Sesiones marcadas como no completadas.
    List<SesionEntrenamiento> findByCompletadaFalse();

    // Sesiones completadas de un usuario.
    List<SesionEntrenamiento> findByUsuarioIdAndCompletadaTrue(Integer usuarioId);

    // Sesiones no completadas de un usuario.
    List<SesionEntrenamiento> findByUsuarioIdAndCompletadaFalse(Integer usuarioId);

    // Sesiones de un usuario cuya fecha de inicio está dentro de un rango.
    List<SesionEntrenamiento> findByUsuarioIdAndFechaInicioBetween(Integer usuarioId, LocalDateTime inicio, LocalDateTime fin);

    // Sesiones (de cualquier usuario) cuya fecha de inicio está dentro de un rango.
    List<SesionEntrenamiento> findByFechaInicioBetween(LocalDateTime inicio, LocalDateTime fin);

    // Sesiones de un usuario para una rutina concreta.
    List<SesionEntrenamiento> findByUsuarioIdAndRutinaId(Integer usuarioId, Integer rutinaId);

    // Número total de sesiones de un usuario.
    Long countByUsuarioId(Integer usuarioId);

    // Número de sesiones completadas de un usuario.
    Long countByUsuarioIdAndCompletadaTrue(Integer usuarioId);

    // Número de sesiones asociadas a una rutina.
    Long countByRutinaId(Integer rutinaId);

    // Consulta JPQL: sesiones de un usuario ordenadas de más reciente a más antigua.
    @Query("SELECT s " +
            "FROM SesionEntrenamiento s " +
            "WHERE s.usuario.id = :usuarioId " +
            "ORDER BY s.fechaInicio DESC ")
    List<SesionEntrenamiento> getSesionesByUsuarioOrderByFecha(@Param("usuarioId") Integer usuarioId);

    // Consulta JPQL: sesiones completadas de un usuario ordenadas de más reciente a más antigua.
    @Query("SELECT s " +
            "FROM SesionEntrenamiento s " +
            "WHERE s.usuario.id = :usuarioId AND s.completada = true " +
            "ORDER BY s.fechaInicio DESC ")
    List<SesionEntrenamiento> getSesionesCompletadasByUsuario(@Param("usuarioId") Integer usuarioId);
}
