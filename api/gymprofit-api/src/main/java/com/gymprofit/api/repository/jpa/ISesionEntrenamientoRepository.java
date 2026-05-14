package com.gymprofit.api.repository.jpa;

import com.gymprofit.api.entity.SesionEntrenamiento;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Hidden
@Repository
@RepositoryRestResource(exported = false)
public interface ISesionEntrenamientoRepository extends CrudRepository<SesionEntrenamiento, Integer> {

    List<SesionEntrenamiento> findByUsuarioId(Integer usuarioId);

    List<SesionEntrenamiento> findByRutinaId(Integer rutinaId);

    List<SesionEntrenamiento> findByCompletadaTrue();

    List<SesionEntrenamiento> findByCompletadaFalse();

    List<SesionEntrenamiento> findByUsuarioIdAndCompletadaTrue(Integer usuarioId);

    List<SesionEntrenamiento> findByUsuarioIdAndCompletadaFalse(Integer usuarioId);

    List<SesionEntrenamiento> findByUsuarioIdAndFechaInicioBetween(Integer usuarioId, LocalDateTime inicio, LocalDateTime fin);

    List<SesionEntrenamiento> findByFechaInicioBetween(LocalDateTime inicio, LocalDateTime fin);

    List<SesionEntrenamiento> findByUsuarioIdAndRutinaId(Integer usuarioId, Integer rutinaId);

    Long countByUsuarioId(Integer usuarioId);

    Long countByUsuarioIdAndCompletadaTrue(Integer usuarioId);

    Long countByRutinaId(Integer rutinaId);

    @Query("SELECT s " +
            "FROM SesionEntrenamiento s " +
            "WHERE s.usuario.id = :usuarioId " +
            "ORDER BY s.fechaInicio DESC ")
    List<SesionEntrenamiento> getSesionesByUsuarioOrderByFecha(@Param("usuarioId") Integer usuarioId);

    @Query("SELECT s " +
            "FROM SesionEntrenamiento s " +
            "WHERE s.usuario.id = :usuarioId AND s.completada = true " +
            "ORDER BY s.fechaInicio DESC ")
    List<SesionEntrenamiento> getSesionesCompletadasByUsuario(@Param("usuarioId") Integer usuarioId);
}
