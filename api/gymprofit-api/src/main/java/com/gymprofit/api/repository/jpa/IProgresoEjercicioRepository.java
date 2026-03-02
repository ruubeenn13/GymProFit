package com.gymprofit.api.repository.jpa;

import com.gymprofit.api.entity.ProgresoEjercicio;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Hidden
@Repository
public interface IProgresoEjercicioRepository extends CrudRepository<ProgresoEjercicio, Integer> {

    List<ProgresoEjercicio> findByUsuarioId(Integer usuarioId);

    List<ProgresoEjercicio> findByEjercicioId(Integer ejercicioId);

    List<ProgresoEjercicio> findByUsuarioIdAndEjercicioId(Integer usuarioId, Integer ejercicioId);

    @Query("SELECT p " +
            "FROM ProgresoEjercicio p " +
            "WHERE p.usuario.id = :usuarioId " +
            "AND p.ejercicio.id = :ejercicioId " +
            "ORDER BY p.fecha DESC")
    List<ProgresoEjercicio> getProgresoByUsuarioAndEjercicio(@Param("usuarioId") Integer usuarioId, @Param("ejercicioId") Integer ejercicioId);
}
