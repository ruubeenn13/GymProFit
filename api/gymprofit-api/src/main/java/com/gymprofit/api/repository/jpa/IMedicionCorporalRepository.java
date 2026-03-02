package com.gymprofit.api.repository.jpa;

import com.gymprofit.api.entity.MedicionCorporal;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Hidden
@Repository
public interface IMedicionCorporalRepository extends CrudRepository<MedicionCorporal, Integer> {

    List<MedicionCorporal> findByUsuarioId(Integer usuarioId);

    List<MedicionCorporal> findByUsuarioIdOrderByFechaDesc(Integer usuarioId);

    List<MedicionCorporal> findByUsuarioIdAndFechaBetween(Integer usuarioId, LocalDateTime inicio, LocalDateTime fin);

    @Query("SELECT m " +
            "FROM MedicionCorporal m " +
            "WHERE m.usuario.id = :usuarioId " +
            "ORDER BY m.fecha DESC")
    List<MedicionCorporal> getUltimasMediciones(@Param("usuarioId") Integer usuarioId);
}
