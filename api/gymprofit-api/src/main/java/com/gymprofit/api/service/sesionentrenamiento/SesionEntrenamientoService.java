package com.gymprofit.api.service.sesionentrenamiento;

import com.gymprofit.api.dto.entity.sesionentrenamiento.SesionEntrenamientoCreateDTO;
import com.gymprofit.api.dto.entity.sesionentrenamiento.SesionEntrenamientoDTO;
import com.gymprofit.api.dto.entity.sesionentrenamiento.SesionEntrenamientoPatchDTO;
import com.gymprofit.api.config.security.SecurityUtils;
import com.gymprofit.api.entity.Rutina;
import com.gymprofit.api.entity.SesionEntrenamiento;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.exceptions.CreateEntityException;
import com.gymprofit.api.exceptions.DeleteEntityException;
import com.gymprofit.api.exceptions.NotFoundEntityException;
import com.gymprofit.api.exceptions.UpdateEntityException;
import com.gymprofit.api.mappers.SesionEntrenamientoMapper;
import com.gymprofit.api.repository.jpa.IRutinaRepository;
import com.gymprofit.api.repository.jpa.ISesionEntrenamientoRepository;
import com.gymprofit.api.repository.jpa.IUsuarioRepository;
import com.gymprofit.api.service.logro.ILogroService;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
public class SesionEntrenamientoService implements ISesionEntrenamientoService{

    private final ISesionEntrenamientoRepository sesionEntrenamientoRepository;
    private final IUsuarioRepository usuarioRepository;
    private final IRutinaRepository rutinaRepository;
    private final SesionEntrenamientoMapper sesionEntrenamientoMapper;
    private final ILogroService logroService;
    private final SecurityUtils securityUtils;
    private final Logger logger = LoggerFactory.getLogger(SesionEntrenamientoService.class);


    @Override
    public List<SesionEntrenamientoDTO> findAll() {
        securityUtils.requireAdmin();

        logger.info("Buscando todas las sesiones de entrenaminento");

        List<SesionEntrenamiento> sesiones = (List<SesionEntrenamiento>) sesionEntrenamientoRepository.findAll();

        return sesionEntrenamientoMapper.toDTOList(sesiones);
    }

    @Override
    public SesionEntrenamientoDTO findById(Integer id) {
        logger.info("Buscando sesión de entrenamiento por id: {}", id);

        SesionEntrenamiento sesion = sesionEntrenamientoRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("La sesión de entrenamiento con id " + id + " no existe"));

        securityUtils.checkOwnership(sesion.getUsuario().getId());

        return sesionEntrenamientoMapper.toDTO(sesion);
    }

    @Override
    public SesionEntrenamientoDTO save(SesionEntrenamientoCreateDTO sesionEntrenamientoCreateDTO) {
        if (!securityUtils.isAdmin()) {
            sesionEntrenamientoCreateDTO.setUsuarioId(securityUtils.getCurrentUserId());
        }

        logger.info("Creando nueva sesión de entrenamiento para usuario id: {}", sesionEntrenamientoCreateDTO.getUsuarioId());

        try {
            Usuario usuario = usuarioRepository.findById(sesionEntrenamientoCreateDTO.getUsuarioId())
                    .orElseThrow(() -> new NotFoundEntityException("El usuario con id " + sesionEntrenamientoCreateDTO.getUsuarioId() + " no existe"));

            SesionEntrenamiento sesion = sesionEntrenamientoMapper.toEntity(sesionEntrenamientoCreateDTO);
            sesion.setUsuario(usuario);

            if (sesionEntrenamientoCreateDTO.getRutinaId() != null) {
                Rutina rutina = rutinaRepository.findById(sesionEntrenamientoCreateDTO.getRutinaId())
                        .orElseThrow(() -> new NotFoundEntityException("La rutina con id " + sesionEntrenamientoCreateDTO.getRutinaId() + " no existe"));

                sesion.setRutina(rutina);
            }

            if (sesion.getFechaInicio() == null) {
                sesion.setFechaInicio(LocalDateTime.now());
            }

            if (sesion.getFechaFin() == null) {
                int minutos = sesionEntrenamientoCreateDTO.getDuracionMinutos() != null
                        ? sesionEntrenamientoCreateDTO.getDuracionMinutos() : 0;
                sesion.setFechaFin(sesion.getFechaInicio().plusMinutes(minutos));
            }

            if (sesionEntrenamientoCreateDTO.getCompletada() != null) {
                sesion.setCompletada(sesionEntrenamientoCreateDTO.getCompletada());
            }

            SesionEntrenamiento sesionGuardada = sesionEntrenamientoRepository.save(sesion);

            SesionEntrenamientoDTO dto = sesionEntrenamientoMapper.toDTO(sesionGuardada);
            if (Boolean.TRUE.equals(sesionGuardada.getCompletada())) {
                List<String> nuevos = logroService.evaluarLogros(sesionGuardada.getUsuario().getId());
                if (!nuevos.isEmpty()) dto.setNuevosLogros(nuevos);
            }
            return dto;
        } catch (NotFoundEntityException e) {
            throw e;
        } catch (Exception e) {
            throw new CreateEntityException(SesionEntrenamiento.class.getSimpleName(), sesionEntrenamientoCreateDTO, e);
        }
    }

    @Override
    public SesionEntrenamientoDTO modify(SesionEntrenamientoDTO sesionEntrenamientoDTO) {
        logger.info("Modificando sesión de entrenamiento con id: {}", sesionEntrenamientoDTO.getId());

        SesionEntrenamiento sesion = sesionEntrenamientoRepository.findById(sesionEntrenamientoDTO.getId())
                .orElseThrow(() -> new NotFoundEntityException("La sesión de entrenamiento con id " + sesionEntrenamientoDTO.getId() + " no existe"));

        securityUtils.checkOwnership(sesion.getUsuario().getId());

        try {
            // El propietario nunca se reasigna desde el body: se mantiene el usuario original de la sesión.

            if (sesionEntrenamientoDTO.getRutinaId() != null) {
                Rutina rutina = rutinaRepository.findById(sesionEntrenamientoDTO.getRutinaId())
                        .orElseThrow(() -> new NotFoundEntityException("La rutina con id " + sesionEntrenamientoDTO.getRutinaId() + " no existe"));

                sesion.setRutina(rutina);
            } else {
                sesion.setRutina(null);
            }

            sesion.setFechaInicio(sesionEntrenamientoDTO.getFechaInicio());
            sesion.setFechaFin(sesionEntrenamientoDTO.getFechaFin());
            sesion.setDuracionMinutos(sesionEntrenamientoDTO.getDuracionMinutos());
            sesion.setCaloriasQuemadas(sesionEntrenamientoDTO.getCaloriasQuemadas());
            sesion.setNotas(sesionEntrenamientoDTO.getNotas());
            sesion.setCompletada(sesionEntrenamientoDTO.getCompletada());

            SesionEntrenamiento sesionActualizada = sesionEntrenamientoRepository.save(sesion);

            return sesionEntrenamientoMapper.toDTO(sesionActualizada);
        } catch (NotFoundEntityException e) {
            throw  e;
        } catch (Exception e) {
            throw new UpdateEntityException(SesionEntrenamiento.class.getSimpleName(), sesionEntrenamientoDTO, e);
        }
    }

    @Transactional
    @Override
    public void deleteById(Integer id) {
        logger.info("Eliminando sesión de entrenamiento con id: {}", id);

        SesionEntrenamiento sesion = sesionEntrenamientoRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("La sesión de entrenamiento con id " + id + " no existe"));

        securityUtils.checkOwnership(sesion.getUsuario().getId());

        try {
            sesionEntrenamientoRepository.delete(sesion);

            logger.info("Sesión de entrenamiento con id {} eliminada correctamente", id);
        } catch (Exception e) {
            throw new DeleteEntityException(SesionEntrenamiento.class.getSimpleName(), id, e);
        }
    }

    @Transactional
    @Override
    public SesionEntrenamientoDTO completarSesion(Integer id, Integer caloriasQuemadas, String notas) {
        logger.info("Completando sesión de entrenamiento con id: {}", id);

        SesionEntrenamiento sesion = sesionEntrenamientoRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("La sesión de entrenamiento con id " + id + " no existe"));

        securityUtils.checkOwnership(sesion.getUsuario().getId());

        try {
            sesion.setFechaFin(LocalDateTime.now());
            sesion.setCompletada(true);

            if (caloriasQuemadas != null) {
                sesion.setCaloriasQuemadas(caloriasQuemadas);
            }

            if (notas != null){
                sesion.setNotas(notas);
            }

            SesionEntrenamiento sesionCompletada = sesionEntrenamientoRepository.save(sesion);

            logger.info("Sesión {} completada", sesionCompletada);

            List<String> nuevos = logroService.evaluarLogros(sesionCompletada.getUsuario().getId());
            SesionEntrenamientoDTO dto = sesionEntrenamientoMapper.toDTO(sesionCompletada);
            if (!nuevos.isEmpty()) dto.setNuevosLogros(nuevos);
            return dto;
        } catch (Exception e) {
            throw new UpdateEntityException(SesionEntrenamiento.class.getSimpleName(), id, e);
        }
    }

    @Override
    public List<SesionEntrenamientoDTO> findByUsuarioId(Integer usuarioId) {
        securityUtils.checkOwnership(usuarioId);

        logger.info("Buscando sesiones de entrenamiento por usuario id: {}", usuarioId);

        List<SesionEntrenamiento> sesiones = sesionEntrenamientoRepository.findByUsuarioId(usuarioId);

        return sesionEntrenamientoMapper.toDTOList(sesiones);
    }

    @Override
    public List<SesionEntrenamientoDTO> findByRutinaId(Integer rutinaId) {
        securityUtils.requireAdmin();

        logger.info("Buscando sesiones de entrenamiento por rutina id: {}", rutinaId);

        List<SesionEntrenamiento> sesiones = sesionEntrenamientoRepository.findByRutinaId(rutinaId);

        return sesionEntrenamientoMapper.toDTOList(sesiones);
    }

    @Override
    public List<SesionEntrenamientoDTO> findCompletadas() {
        securityUtils.requireAdmin();

        logger.info("Buscando sesiones de entrenamiento completadas");

        List<SesionEntrenamiento> sesiones = sesionEntrenamientoRepository.findByCompletadaTrue();

        return sesionEntrenamientoMapper.toDTOList(sesiones);
    }

    @Override
    public List<SesionEntrenamientoDTO> findPendientes() {
        securityUtils.requireAdmin();

        logger.info("Buscando sesiones de entrenamiento pendientes");

        List<SesionEntrenamiento> sesiones = sesionEntrenamientoRepository.findByCompletadaFalse();

        return sesionEntrenamientoMapper.toDTOList(sesiones);
    }

    @Override
    public List<SesionEntrenamientoDTO> findByUsuarioIdAndCompletadas(Integer usuarioId) {
        securityUtils.checkOwnership(usuarioId);

        logger.info("Buscando sesiones completadas del usuario id: {}", usuarioId);

        List<SesionEntrenamiento> sesiones = sesionEntrenamientoRepository.findByUsuarioIdAndCompletadaTrue(usuarioId);

        return sesionEntrenamientoMapper.toDTOList(sesiones);
    }

    @Override
    public List<SesionEntrenamientoDTO> findByUsuarioIdAndPendientes(Integer usuarioId) {
        securityUtils.checkOwnership(usuarioId);

        logger.info("Buscando sesiones pendientes del usuario id: {}", usuarioId);

        List<SesionEntrenamiento> sesiones = sesionEntrenamientoRepository.findByUsuarioIdAndCompletadaFalse(usuarioId);

        return sesionEntrenamientoMapper.toDTOList(sesiones);
    }

    @Override
    public List<SesionEntrenamientoDTO> findByUsuarioIdAndFecha(Integer usuarioId, LocalDate fecha) {
        securityUtils.checkOwnership(usuarioId);

        logger.info("Buscando sesiones del usuario {} en la fecha {}", usuarioId, fecha);

        LocalDateTime inicio = LocalDateTime.of(fecha.getYear(), fecha.getMonth(), fecha.getDayOfMonth(), 0, 0, 0);
        LocalDateTime fin = LocalDateTime.of(fecha.getYear(), fecha.getMonth(), fecha.getDayOfMonth(), 23, 59, 59);

        List<SesionEntrenamiento> sesiones = sesionEntrenamientoRepository.findByUsuarioIdAndFechaInicioBetween(usuarioId, inicio, fin);

        return sesionEntrenamientoMapper.toDTOList(sesiones);
    }

    @Override
    public List<SesionEntrenamientoDTO> findByFecha(LocalDate fecha) {
        securityUtils.requireAdmin();

        logger.info("Buscando sesiones en la fecha {}", fecha);

        LocalDateTime inicio = LocalDateTime.of(fecha.getYear(), fecha.getMonth(), fecha.getDayOfMonth(), 0, 0, 0);
        LocalDateTime fin = LocalDateTime.of(fecha.getYear(), fecha.getMonth(), fecha.getDayOfMonth(), 23, 59, 59);

        List<SesionEntrenamiento> sesiones = sesionEntrenamientoRepository.findByFechaInicioBetween(inicio, fin);

        return sesionEntrenamientoMapper.toDTOList(sesiones);
    }

    @Override
    public List<SesionEntrenamientoDTO> findByUsuarioIdAndRutinaId(Integer usuarioId, Integer rutinaId) {
        securityUtils.checkOwnership(usuarioId);

        logger.info("Buscando sesiones del usuario {} con rutina {}", usuarioId, rutinaId);

        List<SesionEntrenamiento> sesiones = sesionEntrenamientoRepository.findByUsuarioIdAndRutinaId(usuarioId, rutinaId);

        return sesionEntrenamientoMapper.toDTOList(sesiones);
    }

    @Override
    public Long countByUsuarioId(Integer usuarioId) {
        securityUtils.checkOwnership(usuarioId);

        logger.info("Contando sesiones del usuario id: {}", usuarioId);

        return sesionEntrenamientoRepository.countByUsuarioId(usuarioId);
    }

    @Override
    public Long countCompletadasByUsuario(Integer usuarioId) {
        securityUtils.checkOwnership(usuarioId);

        logger.info("Contando sesiones completadas del usuario id: {}", usuarioId);

        return sesionEntrenamientoRepository.countByUsuarioIdAndCompletadaTrue(usuarioId);
    }

    @Override
    public Long countByRutinaId(Integer rutinaId) {
        securityUtils.requireAdmin();

        logger.info("Contando sesiones de la rutina id: {}", rutinaId);

        return sesionEntrenamientoRepository.countByRutinaId(rutinaId);
    }

    @Override
    public List<SesionEntrenamientoDTO> findByUsuarioIdOrderByFecha(Integer usuarioId) {
        securityUtils.checkOwnership(usuarioId);

        logger.info("Buscando sesiones del usuario {} ordenadas por fecha", usuarioId);

        List<SesionEntrenamiento> sesiones = sesionEntrenamientoRepository.getSesionesByUsuarioOrderByFecha(usuarioId);

        return sesionEntrenamientoMapper.toDTOList(sesiones);
    }

    @Override
    public List<SesionEntrenamientoDTO> findCompletadasByUsuario(Integer usuarioId) {
        securityUtils.checkOwnership(usuarioId);

        logger.info("Buscando sesiones completadas del usuario {} ordenadas por fecha", usuarioId);

        List<SesionEntrenamiento> sesiones = sesionEntrenamientoRepository.getSesionesCompletadasByUsuario(usuarioId);

        return sesionEntrenamientoMapper.toDTOList(sesiones);
    }

    @Transactional
    @Override
    public SesionEntrenamientoDTO patch(Integer id, SesionEntrenamientoPatchDTO patchDTO) {
        logger.info("Aplicando patch a sesión de entrenamiento con id: {}", id);

        SesionEntrenamiento sesion = sesionEntrenamientoRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("La sesión de entrenamiento con id " + id + " no existe"));

        securityUtils.checkOwnership(sesion.getUsuario().getId());

        try {
            if (patchDTO.getFechaInicio() != null) sesion.setFechaInicio(patchDTO.getFechaInicio());
            if (patchDTO.getFechaFin() != null) sesion.setFechaFin(patchDTO.getFechaFin());
            if (patchDTO.getDuracionMinutos() != null) sesion.setDuracionMinutos(patchDTO.getDuracionMinutos());
            if (patchDTO.getCaloriasQuemadas() != null) sesion.setCaloriasQuemadas(patchDTO.getCaloriasQuemadas());
            if (patchDTO.getNotas() != null) sesion.setNotas(patchDTO.getNotas());
            if (patchDTO.getCompletada() != null) sesion.setCompletada(patchDTO.getCompletada());

            return sesionEntrenamientoMapper.toDTO(sesionEntrenamientoRepository.save(sesion));
        } catch (Exception e) {
            throw new UpdateEntityException(SesionEntrenamiento.class.getSimpleName(), id, e);
        }
    }
}