package com.gymprofit.api.service.notificacion;

import com.gymprofit.api.dto.entity.notificacion.NotificacionCreateDTO;
import com.gymprofit.api.dto.entity.notificacion.NotificacionDTO;

import java.util.List;

// ============================================================
// INotificacionService — contrato del servicio de notificaciones de usuario
// Define la gestión de las notificaciones enviadas a los usuarios
// (creación, consulta filtrada por estado leído/no leído y por tipo,
// marcado como leídas y borrado), usadas para avisos dentro de la app.
// ============================================================
public interface INotificacionService {

    // Devuelve todas las notificaciones (uso administrativo).
    List<NotificacionDTO> findAll();

    // Busca una notificación por su id.
    NotificacionDTO findById(Integer id);
    // Crea una nueva notificación para un usuario.
    NotificacionDTO save(NotificacionCreateDTO notificacionCreateDTO);

    // Elimina una notificación por su id.
    void deleteById(Integer id);

    // Lista las notificaciones de un usuario.
    List<NotificacionDTO> findByUsuarioId(Integer usuarioId);
    // Lista las notificaciones de un usuario ordenadas por fecha.
    List<NotificacionDTO> findByUsuarioIdOrdenadas(Integer usuarioId);
    // Lista las notificaciones no leídas de un usuario.
    List<NotificacionDTO> findNoLeidasByUsuarioId(Integer usuarioId);
    // Lista las notificaciones leídas de un usuario.
    List<NotificacionDTO> findLeidasByUsuarioId(Integer usuarioId);
    // Filtra las notificaciones de un usuario por tipo.
    List<NotificacionDTO> findByUsuarioIdAndTipo(Integer usuarioId, String tipo);

    // Marca una notificación concreta como leída.
    NotificacionDTO marcarComoLeida(Integer id);

    // Marca todas las notificaciones de un usuario como leídas.
    void marcarTodasComoLeidas(Integer usuarioId);
    // Elimina todas las notificaciones de un usuario.
    void deleteByUsuarioId(Integer usuarioId);

    // Cuenta el total de notificaciones de un usuario.
    Long countByUsuarioId(Integer usuarioId);
    // Cuenta las notificaciones no leídas de un usuario.
    Long countNoLeidasByUsuarioId(Integer usuarioId);

    // Comprueba si el usuario tiene notificaciones pendientes de leer.
    boolean existenNoLeidasByUsuarioId(Integer usuarioId);

    // Actualiza parcialmente una notificación (solo campos no nulos).
    NotificacionDTO patch(Integer id, com.gymprofit.api.dto.entity.notificacion.NotificacionPatchDTO patchDTO);
}
