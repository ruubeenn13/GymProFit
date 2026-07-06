package com.gymprofit.api.repository.jpa;

import com.gymprofit.api.entity.Notificacion;
import com.gymprofit.api.enums.TipoNotificacion;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

// ============================================================
// INotificacionRepository — repositorio JPA de la entidad Notificacion
// Acceso a datos de las notificaciones enviadas a los usuarios (leídas/no leídas, por tipo).
// ============================================================
@Hidden
@Repository
@RepositoryRestResource(exported = false)
public interface INotificacionRepository extends JpaRepository<Notificacion, Integer> {

    // Busca todas las notificaciones de un usuario.
    List<Notificacion> findByUsuarioId(Integer usuarioId);

    // Busca las notificaciones no leídas de un usuario.
    List<Notificacion> findByUsuarioIdAndLeidaFalse(Integer usuarioId);

    // Busca las notificaciones ya leídas de un usuario.
    List<Notificacion> findByUsuarioIdAndLeidaTrue(Integer usuarioId);

    // Busca las notificaciones de un usuario filtradas por tipo.
    List<Notificacion> findByUsuarioIdAndTipo(Integer usuarioId, TipoNotificacion tipo);

    // Busca las notificaciones de un usuario ordenadas de más reciente a más antigua.
    List<Notificacion> findByUsuarioIdOrderByFechaCreacionDesc(Integer usuarioId);

    // Cuenta el número total de notificaciones de un usuario.
    Long countByUsuarioId(Integer usuarioId);

    // Cuenta el número de notificaciones no leídas de un usuario.
    Long countByUsuarioIdAndLeidaFalse(Integer usuarioId);

    // Elimina todas las notificaciones de un usuario.
    void deleteByUsuarioId(Integer usuarioId);

    // Comprueba si un usuario tiene alguna notificación sin leer.
    boolean existsByUsuarioIdAndLeidaFalse(Integer usuarioId);

    // Notificaciones programadas vencidas y con push aún pendiente (las procesa el job).
    List<Notificacion> findByPushEnviadaFalseAndFechaProgramadaLessThanEqual(LocalDateTime ahora);

    // Anti-spam de los recordatorios recurrentes: comprueba si ya se envió al usuario
    // una notificación con el mismo título desde una fecha dada (el título actúa de clave).
    boolean existsByUsuarioIdAndTituloAndFechaCreacionAfter(Integer usuarioId, String titulo, LocalDateTime desde);
}
