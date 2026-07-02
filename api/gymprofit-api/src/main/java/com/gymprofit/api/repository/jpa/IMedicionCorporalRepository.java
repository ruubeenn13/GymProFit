package com.gymprofit.api.repository.jpa;

import com.gymprofit.api.entity.MedicionCorporal;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// ============================================================
// IMedicionCorporalRepository — repositorio JPA de la entidad MedicionCorporal
// Acceso a datos del histórico de mediciones corporales (peso, medidas, etc.) del usuario.
// Usado para mostrar la evolución física en gráficos y estadísticas.
// ============================================================
@Hidden
@Repository
@RepositoryRestResource(exported = false)
public interface IMedicionCorporalRepository extends JpaRepository<MedicionCorporal, Integer> {

    // Busca todas las mediciones de un usuario.
    List<MedicionCorporal> findByUsuarioId(Integer usuarioId);

    // Busca las mediciones de un usuario ordenadas de más reciente a más antigua.
    List<MedicionCorporal> findByUsuarioIdOrderByFechaDesc(Integer usuarioId);

    // Busca las mediciones de un usuario dentro de un rango de fechas.
    List<MedicionCorporal> findByUsuarioIdAndFechaBetween(Integer usuarioId, LocalDateTime inicio, LocalDateTime fin);

    // Obtiene la medición más reciente de un usuario.
    Optional<MedicionCorporal> findFirstByUsuarioIdOrderByFechaDesc(Integer usuarioId);

    // Cuenta el número de mediciones registradas por un usuario.
    Long countByUsuarioId(Integer usuarioId);

    // Elimina todas las mediciones de un usuario.
    void deleteByUsuarioId(Integer usuarioId);

    // Comprueba si un usuario tiene alguna medición registrada.
    boolean existsByUsuarioId(Integer usuarioId);

    // Consulta JPQL que devuelve las mediciones de un usuario ordenadas de más reciente a más antigua.
    @Query("SELECT m " +
            "FROM MedicionCorporal m " +
            "WHERE m.usuario.id = :usuarioId " +
            "ORDER BY m.fecha DESC")
    List<MedicionCorporal> getUltimasMediciones(@Param("usuarioId") Integer usuarioId);
}
