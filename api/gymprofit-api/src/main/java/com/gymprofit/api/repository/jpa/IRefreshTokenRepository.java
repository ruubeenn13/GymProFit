package com.gymprofit.api.repository.jpa;

import com.gymprofit.api.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

// ============================================================
// IRefreshTokenRepository — repositorio JPA de refresh tokens
// Acceso a los refresh tokens opacos persistidos: búsqueda por valor y
// revocación masiva de los de un usuario.
// ============================================================
@Repository
public interface IRefreshTokenRepository extends JpaRepository<RefreshToken, Integer> {

    // Busca un refresh token por su valor opaco.
    Optional<RefreshToken> findByToken(String token);

    // Revoca (marca como revocados) todos los refresh tokens activos de un usuario.
    @Modifying
    @Query("UPDATE RefreshToken r SET r.revocado = true WHERE r.usuario.id = :usuarioId AND r.revocado = false")
    void revocarTodosDeUsuario(@Param("usuarioId") Integer usuarioId);

    /**
     * Revoca un refresh solo si sigue vigente, en una sola sentencia (GP-236): la
     * comprobación y la escritura no pueden separarse. Lo usa la rotación.
     *
     * @return 1 si lo revocó esta llamada; 0 si ya estaba revocado.
     */
    @Modifying
    @Query("UPDATE RefreshToken r SET r.revocado = true WHERE r.id = :id AND r.revocado = false")
    int revocarSiVigente(@Param("id") Integer id);

    // Borra los refresh tokens ya inútiles (revocados o expirados). Lo llama la tarea
    // programada de limpieza para que la tabla no crezca sin límite. Devuelve el nº borrado.
    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.revocado = true OR r.fechaExpiracion < :ahora")
    int borrarRevocadosOExpirados(@Param("ahora") LocalDateTime ahora);
}
