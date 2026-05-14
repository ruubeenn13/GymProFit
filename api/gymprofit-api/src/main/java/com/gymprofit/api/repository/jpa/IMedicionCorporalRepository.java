package com.gymprofit.api.repository.jpa;

import com.gymprofit.api.entity.MedicionCorporal;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Hidden
@Repository
@RepositoryRestResource(exported = false)
public interface IMedicionCorporalRepository extends CrudRepository<MedicionCorporal, Integer> {

    List<MedicionCorporal> findByUsuarioId(Integer usuarioId);

    List<MedicionCorporal> findByUsuarioIdOrderByFechaDesc(Integer usuarioId);

    List<MedicionCorporal> findByUsuarioIdAndFechaBetween(Integer usuarioId, LocalDateTime inicio, LocalDateTime fin);

    Optional<MedicionCorporal> findFirstByUsuarioIdOrderByFechaDesc(Integer usuarioId);

    Long countByUsuarioId(Integer usuarioId);

    void deleteByUsuarioId(Integer usuarioId);

    boolean existsByUsuarioId(Integer usuarioId);

    @Query("SELECT m " +
            "FROM MedicionCorporal m " +
            "WHERE m.usuario.id = :usuarioId " +
            "ORDER BY m.fecha DESC")
    List<MedicionCorporal> getUltimasMediciones(@Param("usuarioId") Integer usuarioId);
}
