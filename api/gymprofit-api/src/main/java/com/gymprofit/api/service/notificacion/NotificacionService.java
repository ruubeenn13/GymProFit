package com.gymprofit.api.service.notificacion;

import com.gymprofit.api.dto.entity.notificacion.NotificacionCreateDTO;
import com.gymprofit.api.dto.entity.notificacion.NotificacionDTO;
import com.gymprofit.api.dto.entity.notificacion.NotificacionPatchDTO;
import com.gymprofit.api.config.security.SecurityUtils;
import com.gymprofit.api.entity.Notificacion;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.enums.TipoNotificacion;
import com.gymprofit.api.exceptions.*;
import com.gymprofit.api.mappers.NotificacionMapper;
import com.gymprofit.api.repository.jpa.INotificacionRepository;
import com.gymprofit.api.repository.jpa.IUsuarioRepository;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
public class NotificacionService implements INotificacionService {

    private final INotificacionRepository notificacionRepository;
    private final IUsuarioRepository usuarioRepository;
    private final NotificacionMapper notificacionMapper;
    private final SecurityUtils securityUtils;
    private final Logger logger = LoggerFactory.getLogger(NotificacionService.class);


    @Override
    public List<NotificacionDTO> findAll() {
        logger.info("Buscando todas las notificaciones");

        securityUtils.requireAdmin();

        List<Notificacion> notificaciones = (List<Notificacion>) notificacionRepository.findAll();

        return notificacionMapper.toDTOList(notificaciones);
    }

    @Override
    public NotificacionDTO findById(Integer id) {
        logger.info("Buscando notificacion por id: {}", id);

        Notificacion notificacion = notificacionRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("La notificación con id " + id + " no existe"));

        securityUtils.checkOwnership(notificacion.getUsuario().getId());

        return notificacionMapper.toDTO(notificacion);
    }

    @Transactional
    @Override
    public NotificacionDTO save(NotificacionCreateDTO notificacionCreateDTO) {
        logger.info("Creando notificación para usuario id: {}", notificacionCreateDTO.getUsuarioId());

        if (!securityUtils.isAdmin()) {
            notificacionCreateDTO.setUsuarioId(securityUtils.getCurrentUserId());
        }

        Usuario usuario = usuarioRepository.findById(notificacionCreateDTO.getUsuarioId())
                .orElseThrow(() -> new NotFoundEntityException("El usuario con id " + notificacionCreateDTO.getUsuarioId() + " no existe"));

        TipoNotificacion tipoNotificacion;

        try {
            tipoNotificacion = TipoNotificacion.valueOf(notificacionCreateDTO.getTipo().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidDataException("Tipo de notificación inválido: " + notificacionCreateDTO.getTipo() +
                    ". Valores válidos: RECORDATORIO, LOGRO, OBJETIVO, SISTEMA");
        }

        try {
            Notificacion notificacion = notificacionMapper.toEntity(notificacionCreateDTO);
            notificacion.setUsuario(usuario);
            notificacion.setTipo(tipoNotificacion);
            notificacion.setFechaCreacion(LocalDateTime.now());
            notificacion.setLeida(false);

            Notificacion notificacionGuardada = notificacionRepository.save(notificacion);

            Notificacion notificacionRecargada = notificacionRepository.findById(notificacionGuardada.getId())
                    .orElseThrow(() -> new NotFoundEntityException("Error al recuperar la notificación guardada"));

            return notificacionMapper.toDTO(notificacionRecargada);
        } catch (NotFoundEntityException | InvalidDataException e) {
            throw e;
        } catch (Exception e) {
            throw new CreateEntityException(Notificacion.class.getSimpleName(), notificacionCreateDTO, e);
        }
    }

    @Transactional
    @Override
    public void deleteById(Integer id) {
        logger.info("Eliminando notifiación con id: {}", id);

        Notificacion notificacion = notificacionRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("La notificación con id " + id + " no existe"));

        securityUtils.checkOwnership(notificacion.getUsuario().getId());

        try {
            notificacionRepository.delete(notificacion);

            logger.info("Notificación con id {} eliminada correctamente", id);
        } catch (Exception e) {
            throw new DeleteEntityException(Notificacion.class.getSimpleName(), id, e);
        }
    }

    @Override
    public List<NotificacionDTO> findByUsuarioId(Integer usuarioId) {
        logger.info("Buscando notificaciones del usuario id: {}", usuarioId);

        securityUtils.checkOwnership(usuarioId);

        List<Notificacion> notificaciones = notificacionRepository.findByUsuarioId(usuarioId);

        return notificacionMapper.toDTOList(notificaciones);
    }

    @Override
    public List<NotificacionDTO> findByUsuarioIdOrdenadas(Integer usuarioId) {
        logger.info("Buscando notificaciones del usuario id: {} ordenadas por fecha", usuarioId);

        securityUtils.checkOwnership(usuarioId);

        List<Notificacion> notificaciones = notificacionRepository.findByUsuarioIdOrderByFechaCreacionDesc(usuarioId);

        return notificacionMapper.toDTOList(notificaciones);
    }

    @Override
    public List<NotificacionDTO> findNoLeidasByUsuarioId(Integer usuarioId) {
        logger.info("Buscando notifiacaciones no leídas del usuario id: {}", usuarioId);

        securityUtils.checkOwnership(usuarioId);

        List<Notificacion> notificaciones = notificacionRepository.findByUsuarioIdAndLeidaFalse(usuarioId);

        return notificacionMapper.toDTOList(notificaciones);
    }

    @Override
    public List<NotificacionDTO> findLeidasByUsuarioId(Integer usuarioId) {
        logger.info("Buscando notificaciones leídas del usuario id: {}", usuarioId);

        securityUtils.checkOwnership(usuarioId);

        List<Notificacion> notificaciones = notificacionRepository.findByUsuarioIdAndLeidaTrue(usuarioId);

        return notificacionMapper.toDTOList(notificaciones);
    }

    @Override
    public List<NotificacionDTO> findByUsuarioIdAndTipo(Integer usuarioId, String tipo) {
        logger.info("Buscando notifiacaiones del usuario id: {} de tipo: {}", usuarioId, tipo);

        securityUtils.checkOwnership(usuarioId);

        TipoNotificacion tipoNotificacion;

        try {
            tipoNotificacion = TipoNotificacion.valueOf(tipo.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidDataException("Tipo de notifiación inválido: " + tipo);
        }

        List<Notificacion> notificaciones = notificacionRepository.findByUsuarioIdAndTipo(usuarioId, tipoNotificacion);

        return notificacionMapper.toDTOList(notificaciones);
    }

    @Transactional
    @Override
    public NotificacionDTO marcarComoLeida(Integer id) {
        logger.info("Marcando notificación id: {} como leída", id);

        Notificacion notificacion = notificacionRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("La notificación con id " + id + " no existe"));

        securityUtils.checkOwnership(notificacion.getUsuario().getId());

        notificacion.setLeida(true);

        Notificacion notificacionGuardada = notificacionRepository.save(notificacion);

        return notificacionMapper.toDTO(notificacionGuardada);
    }

    @Transactional
    @Override
    public void marcarTodasComoLeidas(Integer usuarioId) {
        logger.info("Marcando todas las notifiacaiones del usuario id: {} como leídas", usuarioId);

        securityUtils.checkOwnership(usuarioId);

        List<Notificacion> noLeidas = notificacionRepository.findByUsuarioIdAndLeidaFalse(usuarioId);

        noLeidas.forEach(n -> n.setLeida(true));

        notificacionRepository.saveAll(noLeidas);
    }

    @Transactional
    @Override
    public void deleteByUsuarioId(Integer usuarioId) {
        logger.info("Eliminando todas las notificaciones del usuario id: {}", usuarioId);

        securityUtils.checkOwnership(usuarioId);

        try {
            notificacionRepository.deleteByUsuarioId(usuarioId);
        } catch (Exception e) {
            throw new DeleteEntityException(Notificacion.class.getSimpleName(), usuarioId, e);
        }
    }

    @Override
    public Long countByUsuarioId(Integer usuarioId) {
        logger.info("Contando notificaciones del usuario id: {}", usuarioId);

        securityUtils.checkOwnership(usuarioId);

        return notificacionRepository.countByUsuarioId(usuarioId);
    }

    @Override
    public Long countNoLeidasByUsuarioId(Integer usuarioId) {
        logger.info("Contando notifiacaiones no leídas del usuario id: {}", usuarioId);

        securityUtils.checkOwnership(usuarioId);

        return notificacionRepository.countByUsuarioIdAndLeidaFalse(usuarioId);
    }

    @Override
    public boolean existenNoLeidasByUsuarioId(Integer usuarioId) {
        logger.info("Verificando si existen notificaciones no leídas del usuario id: {}", usuarioId);

        securityUtils.checkOwnership(usuarioId);

        return notificacionRepository.existsByUsuarioIdAndLeidaFalse(usuarioId);
    }

    @Transactional
    @Override
    public NotificacionDTO patch(Integer id, NotificacionPatchDTO patchDTO) {
        logger.info("Aplicando patch a notificación con id: {}", id);

        Notificacion notificacion = notificacionRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("La notificación con id " + id + " no existe"));

        securityUtils.checkOwnership(notificacion.getUsuario().getId());

        try {
            if (patchDTO.getTitulo() != null) notificacion.setTitulo(patchDTO.getTitulo());
            if (patchDTO.getMensaje() != null) notificacion.setMensaje(patchDTO.getMensaje());
            if (patchDTO.getTipo() != null)
                notificacion.setTipo(TipoNotificacion.valueOf(patchDTO.getTipo().toUpperCase()));
            if (patchDTO.getFechaProgramada() != null) notificacion.setFechaProgramada(patchDTO.getFechaProgramada());
            if (patchDTO.getLeida() != null) notificacion.setLeida(patchDTO.getLeida());

            return notificacionMapper.toDTO(notificacionRepository.save(notificacion));
        } catch (Exception e) {
            throw new UpdateEntityException(Notificacion.class.getSimpleName(), id, e);
        }
    }
}
