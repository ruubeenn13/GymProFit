package com.gymprofit.api.repository.jpa;

import com.gymprofit.api.entity.ProgresoEjercicio;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Hidden
@Repository
@RepositoryRestResource(exported = false)
public interface IProgresoEjercicioRepository extends CrudRepository<ProgresoEjercicio, Integer> {

    List<ProgresoEjercicio> findByUsuarioId(Integer usuarioId);

    List<ProgresoEjercicio> findByEjercicioId(Integer ejercicioId);

    List<ProgresoEjercicio> findByUsuarioIdAndEjercicioId(Integer usuarioId, Integer ejercicioId);

    List<ProgresoEjercicio> findByUsuarioIdOrderByFechaDesc(Integer usuarioId);

    Optional<ProgresoEjercicio> findFirstByUsuarioIdAndEjercicioIdOrderByFechaDesc(Integer usuarioId, Integer ejercicioId);

    Long countByUsuarioId(Integer usarioId);

    Long countByEjercicioId(Integer ejercicioId);

    void deleteByUsuarioId(Integer usuarioId);

    void deleteByUsuarioIdAndEjercicioId(Integer usuarioId, Integer ejercicioId);

    boolean existsByUsuarioIdAndEjercicioId(Integer usuarioId, Integer ejercicioId);

    @Query("SELECT p " +
            "FROM ProgresoEjercicio p " +
            "WHERE p.usuario.id = :usuarioId " +
            "AND p.ejercicio.id = :ejercicioId " +
            "ORDER BY p.fecha DESC")
    List<ProgresoEjercicio> getProgresoByUsuarioAndEjercicio(@Param("usuarioId") Integer usuarioId, @Param("ejercicioId") Integer ejercicioId);
}
