package com.gymprofit.api.repository.jpa;

import com.gymprofit.api.entity.VerificacionCorreoCodigo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

// ============================================================
// IVerificacionCorreoCodigoRepository — los códigos de verificación del correo (GP-045)
// Las mismas tres operaciones que IPasswordResetCodigoRepository: el último código
// vivo, matar los vivos de un usuario y purgar los inútiles.
// ============================================================
@Repository
public interface IVerificacionCorreoCodigoRepository extends JpaRepository<VerificacionCorreoCodigo, Integer> {

    /**
     * Último código emitido a un usuario que sigue vivo: ni usado, ni caducado.
     * Por id descendente, porque dos emisiones pueden caer en el mismo segundo.
     */
    Optional<VerificacionCorreoCodigo> findFirstByUsuarioIdAndUsadoFalseAndFechaExpiracionAfterOrderByIdDesc(
            Integer usuarioId, LocalDateTime ahora);

    /**
     * Marca como usados todos los códigos vivos de un usuario: al emitir uno nuevo, al
     * verificar y al cambiar el correo.
     */
    @Modifying
    @Query("UPDATE VerificacionCorreoCodigo c SET c.usado = true WHERE c.usuario.id = :usuarioId AND c.usado = false")
    void invalidarTodosDeUsuario(@Param("usuarioId") Integer usuarioId);

    /**
     * Borra los códigos usados o caducados. Lo llama la purga nocturna.
     *
     * @return número de filas borradas.
     */
    @Modifying
    @Query("DELETE FROM VerificacionCorreoCodigo c WHERE c.usado = true OR c.fechaExpiracion < :ahora")
    int borrarUsadosOExpirados(@Param("ahora") LocalDateTime ahora);
}
