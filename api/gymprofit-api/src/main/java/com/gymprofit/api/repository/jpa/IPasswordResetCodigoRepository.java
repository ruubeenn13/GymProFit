package com.gymprofit.api.repository.jpa;

import com.gymprofit.api.entity.PasswordResetCodigo;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

// ============================================================
// IPasswordResetCodigoRepository — repositorio JPA de los códigos de recuperación
// Acceso a los códigos de un solo uso emitidos para restablecer contraseñas:
// último código vivo de un usuario, invalidación masiva al emitir uno nuevo o al
// consumirlo, y limpieza de los ya inútiles. No exportado como recurso REST.
// ============================================================
@Hidden
@Repository
@RepositoryRestResource(exported = false)
public interface IPasswordResetCodigoRepository extends JpaRepository<PasswordResetCodigo, Integer> {

    /**
     * Último código emitido a un usuario que sigue vivo: ni usado, ni caducado.
     * <p>
     * Se ordena por id descendente porque dos solicitudes seguidas pueden caer en el
     * mismo segundo y la fecha no las distinguiría.
     */
    Optional<PasswordResetCodigo> findFirstByUsuarioIdAndUsadoFalseAndFechaExpiracionAfterOrderByIdDesc(
            Integer usuarioId, LocalDateTime ahora);

    /**
     * Marca como usados todos los códigos vivos de un usuario.
     * <p>
     * Se llama al emitir uno nuevo (el anterior deja de valer al instante) y al
     * consumir uno con éxito, para que no queden códigos sueltos tras el cambio.
     */
    @Modifying
    @Query("UPDATE PasswordResetCodigo c SET c.usado = true WHERE c.usuario.id = :usuarioId AND c.usado = false")
    void invalidarTodosDeUsuario(@Param("usuarioId") Integer usuarioId);

    /**
     * Borra los códigos ya inútiles (usados o caducados) para que la tabla no crezca
     * sin límite. Lo llama la tarea programada de limpieza.
     *
     * @return número de filas borradas.
     */
    @Modifying
    @Query("DELETE FROM PasswordResetCodigo c WHERE c.usado = true OR c.fechaExpiracion < :ahora")
    int borrarUsadosOExpirados(@Param("ahora") LocalDateTime ahora);
}
