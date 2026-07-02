package com.gymprofit.api.repository.jpa;

import com.gymprofit.api.entity.UsuarioLogro;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import java.util.List;

// ============================================================
// IUsuarioLogroRepository — repositorio JPA de logros obtenidos por usuario
// Gestiona la tabla intermedia que registra qué logros/insignias ha
// desbloqueado cada usuario. No exportado como recurso REST.
// ============================================================
@Hidden
@Repository
@RepositoryRestResource(exported = false)
public interface IUsuarioLogroRepository extends JpaRepository<UsuarioLogro, Integer> {

    // Logros obtenidos por un usuario.
    List<UsuarioLogro> findByUsuarioId(Integer usuarioId);

    // Consulta JPQL: ids de los logros obtenidos por un usuario.
    @Query("SELECT ul.logro.id FROM UsuarioLogro ul WHERE ul.usuario.id = :usuarioId")
    List<Integer> findLogroIdsByUsuarioId(@Param("usuarioId") Integer usuarioId);
}
