package com.gymprofit.api.repository.jpa;

import com.gymprofit.api.entity.RefreshToken;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// ============================================================
// IRefreshTokenRepository — repositorio JPA de refresh tokens
// Acceso a los refresh tokens opacos persistidos: búsqueda por valor y
// revocación masiva de los de un usuario. No exportado como recurso REST.
// ============================================================
@Hidden
@Repository
@RepositoryRestResource(exported = false)
public interface IRefreshTokenRepository extends JpaRepository<RefreshToken, Integer> {

    // Busca un refresh token por su valor opaco.
    Optional<RefreshToken> findByToken(String token);

    // Revoca (marca como revocados) todos los refresh tokens activos de un usuario.
    @Modifying
    @Query("UPDATE RefreshToken r SET r.revocado = true WHERE r.usuario.id = :usuarioId AND r.revocado = false")
    void revocarTodosDeUsuario(@Param("usuarioId") Integer usuarioId);
}
