package com.gymprofit.api.service.sesionentrenamiento;

import com.gymprofit.api.dto.entity.sesionentrenamiento.SesionEntrenamientoCreateDTO;
import com.gymprofit.api.dto.entity.sesionentrenamiento.SesionEntrenamientoDTO;
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
    private final Logger logger = LoggerFactory.getLogger(SesionEntrenamientoService.class);


    @Override
    public List<SesionEntrenamientoDTO> findAll() {
        logger.info("Buscando todas las sesiones de entrenaminento");

        List<SesionEntrenamiento> sesiones = (List<SesionEntrenamiento>) sesionEntrenamientoRepository.findAll();

        return sesionEntrenamientoMapper.toDTOList(sesiones);
    }

    @Override
    public SesionEntrenamientoDTO findById(Integer id) {
        logger.info("Buscando sesión de entrenamiento por id: {}", id);

        SesionEntrenamiento sesion = sesionEntrenamientoRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("La sesión de entrenamiento con id " + id + " no existe"));

        return sesionEntrenamientoMapper.toDTO(sesion);
    }

    @Override
    public SesionEntrenamientoDTO save(SesionEntrenamientoCreateDTO sesionEntrenamientoCreateDTO) {
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

            sesion.setCompletada(false);

            SesionEntrenamiento sesionGuardada = sesionEntrenamientoRepository.save(sesion);

            return sesionEntrenamientoMapper.toDTO(sesionGuardada);
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

        try {
            Usuario usuario = usuarioRepository.findById(sesionEntrenamientoDTO.getUsuarioId())
                    .orElseThrow(() -> new NotFoundEntityException("El usuario con id " + sesionEntrenamientoDTO + " no existe"));

            sesion.setUsuario(usuario);

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

            return sesionEntrenamientoMapper.toDTO(sesionCompletada);
        } catch (Exception e) {
            throw new UpdateEntityException(SesionEntrenamiento.class.getSimpleName(), id, e);
        }
    }

    @Override
    public List<SesionEntrenamientoDTO> findByUsuarioId(Integer usuarioId) {
        logger.info("Buscando sesiones de entrenamiento por usuario id: {}", usuarioId);

        List<SesionEntrenamiento> sesiones = sesionEntrenamientoRepository.findByUsuarioId(usuarioId);

        return sesionEntrenamientoMapper.toDTOList(sesiones);
    }

    @Override
    public List<SesionEntrenamientoDTO> findByRutinaId(Integer rutinaId) {
        logger.info("Buscando sesiones de entrenamiento por rutina id: {}", rutinaId);

        List<SesionEntrenamiento> sesiones = sesionEntrenamientoRepository.findByRutinaId(rutinaId);

        return sesionEntrenamientoMapper.toDTOList(sesiones);
    }

    @Override
    public List<SesionEntrenamientoDTO> findCompletadas() {
        logger.info("Buscando sesiones de entrenamiento completadas");

        List<SesionEntrenamiento> sesiones = sesionEntrenamientoRepository.findByCompletadaTrue();

        return sesionEntrenamientoMapper.toDTOList(sesiones);
    }

    @Override
    public List<SesionEntrenamientoDTO> findPendientes() {
        logger.info("Buscando sesiones de entrenamiento pendientes");

        List<SesionEntrenamiento> sesiones = sesionEntrenamientoRepository.findByCompletadaFalse();

        return sesionEntrenamientoMapper.toDTOList(sesiones);
    }

    @Override
    public List<SesionEntrenamientoDTO> findByUsuarioIdAndCompletadas(Integer usuarioId) {
        logger.info("Buscando sesiones completadas del usuario id: {}", usuarioId);

        List<SesionEntrenamiento> sesiones = sesionEntrenamientoRepository.findByUsuarioIdAndCompletadaTrue(usuarioId);

        return sesionEntrenamientoMapper.toDTOList(sesiones);
    }

    @Override
    public List<SesionEntrenamientoDTO> findByUsuarioIdAndPendientes(Integer usuarioId) {
        logger.info("Buscando sesiones pendientes del usuario id: {}", usuarioId);

        List<SesionEntrenamiento> sesiones = sesionEntrenamientoRepository.findByUsuarioIdAndCompletadaFalse(usuarioId);

        return sesionEntrenamientoMapper.toDTOList(sesiones);
    }

    @Override
    public List<SesionEntrenamientoDTO> findByUsuarioIdAndFecha(Integer usuarioId, LocalDate fecha) {
        logger.info("Buscando sesiones del usuario {} en la fecha {}", usuarioId, fecha);

        LocalDateTime inicio = LocalDateTime.of(fecha.getYear(), fecha.getMonth(), fecha.getDayOfMonth(), 0, 0, 0);
        LocalDateTime fin = LocalDateTime.of(fecha.getYear(), fecha.getMonth(), fecha.getDayOfMonth(), 23, 59, 59);

        List<SesionEntrenamiento> sesiones = sesionEntrenamientoRepository.findByUsuarioIdAndFechaInicioBetween(usuarioId, inicio, fin);

        return sesionEntrenamientoMapper.toDTOList(sesiones);
    }

    @Override
    public List<SesionEntrenamientoDTO> findByFecha(LocalDate fecha) {
        logger.info("Buscando sesiones en la fecha {}", fecha);

        LocalDateTime inicio = LocalDateTime.of(fecha.getYear(), fecha.getMonth(), fecha.getDayOfMonth(), 0, 0, 0);
        LocalDateTime fin = LocalDateTime.of(fecha.getYear(), fecha.getMonth(), fecha.getDayOfMonth(), 23, 59, 59);

        List<SesionEntrenamiento> sesiones = sesionEntrenamientoRepository.findByFechaInicioBetween(inicio, fin);

        return sesionEntrenamientoMapper.toDTOList(sesiones);
    }

    @Override
    public List<SesionEntrenamientoDTO> findByUsuarioIdAndRutinaId(Integer usuarioId, Integer rutinaId) {
        logger.info("Buscando sesiones del usuario {} con rutina {}", usuarioId, rutinaId);

        List<SesionEntrenamiento> sesiones = sesionEntrenamientoRepository.findByUsuarioIdAndRutinaId(usuarioId, rutinaId);

        return sesionEntrenamientoMapper.toDTOList(sesiones);
    }

    @Override
    public Long countByUsuarioId(Integer usuarioId) {
        logger.info("Contando sesiones del usuario id: {}", usuarioId);

        return sesionEntrenamientoRepository.countByUsuarioId(usuarioId);
    }

    @Override
    public Long countCompletadasByUsuario(Integer usuarioId) {
        logger.info("Contando sesiones completadas del usuario id: {}", usuarioId);

        return sesionEntrenamientoRepository.countByUsuarioIdAndCompletadaTrue(usuarioId);
    }

    @Override
    public Long countByRutinaId(Integer rutinaId) {
        logger.info("Contando sesiones de la rutina id: {}", rutinaId);

        return sesionEntrenamientoRepository.countByRutinaId(rutinaId);
    }

    @Override
    public List<SesionEntrenamientoDTO> findByUsuarioIdOrderByFecha(Integer usuarioId) {
        logger.info("Buscando sesiones del usuario {} ordenadas por fecha", usuarioId);

        List<SesionEntrenamiento> sesiones = sesionEntrenamientoRepository.getSesionesByUsuarioOrderByFecha(usuarioId);

        return sesionEntrenamientoMapper.toDTOList(sesiones);
    }

    @Override
    public List<SesionEntrenamientoDTO> findCompletadasByUsuario(Integer usuarioId) {
        logger.info("Buscando sesiones completadas del usuario {} ordenadas por fecha", usuarioId);

        List<SesionEntrenamiento> sesiones = sesionEntrenamientoRepository.getSesionesCompletadasByUsuario(usuarioId);

        return sesionEntrenamientoMapper.toDTOList(sesiones);
    }
}