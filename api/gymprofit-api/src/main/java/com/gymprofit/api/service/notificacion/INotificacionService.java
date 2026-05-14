package com.gymprofit.api.service.notificacion;

import com.gymprofit.api.dto.entity.notificacion.NotificacionCreateDTO;
import com.gymprofit.api.dto.entity.notificacion.NotificacionDTO;

import java.util.List;

public interface INotificacionService {

    List<NotificacionDTO> findAll();

    NotificacionDTO findById(Integer id);
    NotificacionDTO save(NotificacionCreateDTO notificacionCreateDTO);

    void deleteById(Integer id);

    List<NotificacionDTO> findByUsuarioId(Integer usuarioId);
    List<NotificacionDTO> findByUsuarioIdOrdenadas(Integer usuarioId);
    List<NotificacionDTO> findNoLeidasByUsuarioId(Integer usuarioId);
    List<NotificacionDTO> findLeidasByUsuarioId(Integer usuarioId);
    List<NotificacionDTO> findByUsuarioIdAndTipo(Integer usuarioId, String tipo);

    NotificacionDTO marcarComoLeida(Integer id);

    void marcarTodasComoLeidas(Integer usuarioId);
    void deleteByUsuarioId(Integer usuarioId);

    Long countByUsuarioId(Integer usuarioId);
    Long countNoLeidasByUsuarioId(Integer usuarioId);

    boolean existenNoLeidasByUsuarioId(Integer usuarioId);

    NotificacionDTO patch(Integer id, com.gymprofit.api.dto.entity.notificacion.NotificacionPatchDTO patchDTO);
}
