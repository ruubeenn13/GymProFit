package com.gymprofit.api.service.progresoejercicio;

import com.gymprofit.api.dto.entity.progresoejercicio.ProgresoEjercicioCreateDTO;
import com.gymprofit.api.dto.entity.progresoejercicio.ProgresoEjercicioDTO;
import com.gymprofit.api.dto.entity.progresoejercicio.ProgresoEjercicioPatchDTO;
import com.gymprofit.api.config.security.SecurityUtils;
import com.gymprofit.api.entity.Ejercicio;
import com.gymprofit.api.entity.ProgresoEjercicio;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.exceptions.CreateEntityException;
import com.gymprofit.api.exceptions.DeleteEntityException;
import com.gymprofit.api.exceptions.NotFoundEntityException;
import com.gymprofit.api.exceptions.UpdateEntityException;
import com.gymprofit.api.mappers.ProgresoEjercicioMapper;
import com.gymprofit.api.repository.jpa.IEjercicioRepository;
import com.gymprofit.api.repository.jpa.IProgresoEjercicioRepository;
import com.gymprofit.api.repository.jpa.IUsuarioRepository;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

// ============================================================
// ProgresoEjercicioService — implementación del servicio de progreso en ejercicios
// Gestiona el registro y consulta del progreso (mejor peso, repeticiones,
// tiempo) de cada usuario por ejercicio, con comprobaciones de propiedad
// mediante SecurityUtils.
// ============================================================
@Service
@AllArgsConstructor
public class ProgresoEjercicioService implements IProgresoEjercicioService{

    private final IProgresoEjercicioRepository progresoEjercicioRepository;
    private final IUsuarioRepository usuarioRepository;
    private final IEjercicioRepository ejercicioRepository;
    private final ProgresoEjercicioMapper progresoEjercicioMapper;
    private final SecurityUtils securityUtils;
    private final Logger logger = LoggerFactory.getLogger(ProgresoEjercicioService.class);


    // Devuelve todos los progresos de ejercicio del sistema. Solo ADMIN.
    @Override
    public List<ProgresoEjercicioDTO> findAll() {
        logger.info("Buscando todos los progresos de ejercicios");
        securityUtils.requireAdmin();

        List<ProgresoEjercicio> progresoEjercicios = (List<ProgresoEjercicio>) progresoEjercicioRepository.findAll();

        return progresoEjercicioMapper.toDTOList(progresoEjercicios);
    }

    // Busca un progreso de ejercicio por id, verificando la propiedad del usuario.
    @Override
    public ProgresoEjercicioDTO findById(Integer id) {
        logger.info("Buscando progreso de ejercicio por id: {}", id);

        ProgresoEjercicio progresoEjercicio = progresoEjercicioRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("El progreso de ejercicio con id " + id + " no existe"));

        securityUtils.checkOwnership(progresoEjercicio.getUsuario().getId());

        return progresoEjercicioMapper.toDTO(progresoEjercicio);
    }

    // Crea un nuevo registro de progreso, resolviendo usuario y ejercicio y fijando la fecha actual.
    @Transactional
    @Override
    public ProgresoEjercicioDTO save(ProgresoEjercicioCreateDTO progresoEjercicioCreateDTO) {
        logger.info("Creando nuevo progreso de ejercicio para usuario id: {} y ejercicio id: {}", progresoEjercicioCreateDTO.getUsuarioId(), progresoEjercicioCreateDTO.getEjercicioId());

        if (!securityUtils.isAdmin()) progresoEjercicioCreateDTO.setUsuarioId(securityUtils.getCurrentUserId());

        Usuario usuario = usuarioRepository.findById(progresoEjercicioCreateDTO.getUsuarioId())
                .orElseThrow(() -> new NotFoundEntityException("El usuario con id: " + progresoEjercicioCreateDTO.getUsuarioId() + " no existe"));

        Ejercicio ejercicio = ejercicioRepository.findById(progresoEjercicioCreateDTO.getEjercicioId())
                .orElseThrow(() -> new NotFoundEntityException("El ejercicio con id " + progresoEjercicioCreateDTO.getEjercicioId() + " no existe"));

        try {
            ProgresoEjercicio progresoEjercicio = progresoEjercicioMapper.toEntity(progresoEjercicioCreateDTO);
            progresoEjercicio.setUsuario(usuario);
            progresoEjercicio.setEjercicio(ejercicio);
            progresoEjercicio.setFecha(LocalDateTime.now());

            ProgresoEjercicio progresoGuardado = progresoEjercicioRepository.save(progresoEjercicio);

            ProgresoEjercicio progresoRecargado = progresoEjercicioRepository.findById(progresoGuardado.getId())
                    .orElseThrow(() -> new NotFoundEntityException("Error al recuperar el progreso guardado"));

            return progresoEjercicioMapper.toDTO(progresoRecargado);
        } catch (NotFoundEntityException e) {
            throw e;
        } catch (Exception e) {
            throw new CreateEntityException(ProgresoEjercicio.class.getSimpleName(), progresoEjercicioCreateDTO, e);
        }
    }

    // Actualiza los valores de mejor marca (peso, repeticiones, tiempo) y notas de un progreso existente.
    @Override
    public ProgresoEjercicioDTO modify(ProgresoEjercicioDTO progresoEjercicioDTO) {
        logger.info("Modificando progreso de ejercicio con id: {}", progresoEjercicioDTO.getId());

        ProgresoEjercicio progresoEjercicio = progresoEjercicioRepository.findById(progresoEjercicioDTO.getId())
                .orElseThrow(() -> new NotFoundEntityException("El progreso de ejercicio con id " + progresoEjercicioDTO.getId() + " no existe"));

        securityUtils.checkOwnership(progresoEjercicio.getUsuario().getId());

        try {
            progresoEjercicio.setMejorPeso(progresoEjercicioDTO.getMejorPeso());
            progresoEjercicio.setMejorRepeticiones(progresoEjercicioDTO.getMejorRepeticiones());
            progresoEjercicio.setMejorTiempoSegundos(progresoEjercicioDTO.getMejorTiempoSegundos());
            progresoEjercicio.setNotas(progresoEjercicioDTO.getNotas());

            ProgresoEjercicio progresoActualizado = progresoEjercicioRepository.save(progresoEjercicio);

            return progresoEjercicioMapper.toDTO(progresoActualizado);
        } catch (Exception e) {
            throw new UpdateEntityException(ProgresoEjercicio.class.getSimpleName(), progresoEjercicioDTO, e);
        }
    }

    // Elimina definitivamente un registro de progreso tras comprobar la propiedad.
    @Transactional
    @Override
    public void deleteById(Integer id) {
        logger.info("Eliminando progreso de ejercicio con id: {}", id);

        ProgresoEjercicio progresoEjercicio = progresoEjercicioRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("El progreso de ejercicio con id " + id + " no existe"));

        securityUtils.checkOwnership(progresoEjercicio.getUsuario().getId());

        try {
            progresoEjercicioRepository.delete(progresoEjercicio);

            logger.info("Progreso de ejercicio con id {} eliminado correctamente", id);
        } catch (Exception e) {
            throw new DeleteEntityException(ProgresoEjercicio.class.getSimpleName(), id, e);
        }
    }

    // Lista todos los progresos registrados de un usuario.
    @Override
    public List<ProgresoEjercicioDTO> findByUsuarioId(Integer usuarioId) {
        logger.info("Buscando progresos del usuario id: {}", usuarioId);
        securityUtils.checkOwnership(usuarioId);

        List<ProgresoEjercicio> progresoEjercicios = progresoEjercicioRepository.findByUsuarioId(usuarioId);

        return progresoEjercicioMapper.toDTOList(progresoEjercicios);
    }

    // Lista todos los progresos registrados para un ejercicio concreto (todos los usuarios).
    @Override
    public List<ProgresoEjercicioDTO> findByEjercicioId(Integer ejercicioId) {
        logger.info("Buscando progresos del ejercicio id: {}", ejercicioId);

        List<ProgresoEjercicio> progresoEjercicios = progresoEjercicioRepository.findByEjercicioId(ejercicioId);

        return progresoEjercicioMapper.toDTOList(progresoEjercicios);
    }

    // Lista los progresos de un usuario para un ejercicio concreto.
    @Override
    public List<ProgresoEjercicioDTO> findByUsuarioIdAndEjercicioId(Integer usuarioId, Integer ejercicioId) {
        logger.info("Buscando progresos del usuario id: {} y ejercicio id: {}", usuarioId, ejercicioId);
        securityUtils.checkOwnership(usuarioId);

        List<ProgresoEjercicio> progresoEjercicios = progresoEjercicioRepository.findByUsuarioIdAndEjercicioId(usuarioId, ejercicioId);

        return progresoEjercicioMapper.toDTOList(progresoEjercicios);
    }

    // Lista los progresos de un usuario ordenados por fecha descendente.
    @Override
    public List<ProgresoEjercicioDTO> findByUsuarioIdOrdenado(Integer usuarioId) {
        logger.info("Buscando progresos del usuario id: {} ordenados por fecha", usuarioId);
        securityUtils.checkOwnership(usuarioId);

        List<ProgresoEjercicio> progresoEjercicios = progresoEjercicioRepository.findByUsuarioIdOrderByFechaDesc(usuarioId);

        return progresoEjercicioMapper.toDTOList(progresoEjercicios);
    }

    // Obtiene el histórico de progreso de un usuario en un ejercicio mediante consulta específica del repositorio.
    @Override
    public List<ProgresoEjercicioDTO> getProgresoByUsuarioAndEjercicio(Integer usuarioId, Integer ejercicioId) {
        logger.info("Buscando progreso del usuario id: {} en ejercicio id: {}", usuarioId, ejercicioId);
        securityUtils.checkOwnership(usuarioId);

        List<ProgresoEjercicio> progresoEjercicios = progresoEjercicioRepository.getProgresoByUsuarioAndEjercicio(usuarioId, ejercicioId);

        return progresoEjercicioMapper.toDTOList(progresoEjercicios);
    }

    // Obtiene el registro de progreso más reciente de un usuario en un ejercicio.
    @Override
    public ProgresoEjercicioDTO getUltimoProgresoByUsuarioAndEjercicio(Integer usuarioId, Integer ejercicioId) {
        logger.info("Buscando último progreso del usuario id: {} en ejercicio id: {}", usuarioId, ejercicioId);
        securityUtils.checkOwnership(usuarioId);

        ProgresoEjercicio progreso = progresoEjercicioRepository.findFirstByUsuarioIdAndEjercicioIdOrderByFechaDesc(usuarioId, ejercicioId)
                .orElseThrow(() -> new NotFoundEntityException("No existe progreso para el usuario " + usuarioId + " en el ejercicio " + ejercicioId));

        return progresoEjercicioMapper.toDTO(progreso);
    }

    // Cuenta el total de progresos registrados de un usuario.
    @Override
    public Long countByUsuarioId(Integer usuarioId) {
        logger.info("Contando progresos del usuario id: {}", usuarioId);
        securityUtils.checkOwnership(usuarioId);

        return progresoEjercicioRepository.countByUsuarioId(usuarioId);
    }

    // Cuenta el total de progresos registrados para un ejercicio (público, sin restricción de propiedad).
    @Override
    public Long countByEjercicioId(Integer ejercicioId) {
        logger.info("Contando progresos del ejercicio id: {}", ejercicioId);

        return progresoEjercicioRepository.countByEjercicioId(ejercicioId);
    }

    // Elimina todos los progresos de un usuario (p.ej. al borrar la cuenta).
    @Transactional
    @Override
    public void deleteByUsuarioId(Integer usuarioId) {
        logger.info("Eliminando progresos del usuario id: {}", usuarioId);
        securityUtils.checkOwnership(usuarioId);

        try {
            progresoEjercicioRepository.deleteByUsuarioId(usuarioId);
        } catch (Exception e) {
            throw new DeleteEntityException(ProgresoEjercicio.class.getSimpleName(), usuarioId, e);
        }
    }

    // Elimina los progresos de un usuario para un ejercicio concreto.
    @Transactional
    @Override
    public void deleteByUsuarioIdAndEjercicioId(Integer usuarioId, Integer ejercicioId) {
        logger.info("Eliminando progresos del usuario id: {} en ejercicio id: {}", usuarioId, ejercicioId);
        securityUtils.checkOwnership(usuarioId);

        try {
            progresoEjercicioRepository.deleteByUsuarioIdAndEjercicioId(usuarioId, ejercicioId);
        } catch (Exception e) {
            throw new DeleteEntityException(ProgresoEjercicio.class.getSimpleName(), usuarioId);
        }
    }

    // Indica si un usuario tiene algún progreso registrado para un ejercicio concreto.
    @Override
    public boolean existsByUsuarioIdAndEjercicioId(Integer usuarioId, Integer ejercicioId) {
        logger.info("Verificando si existe progreso del usuario id: {} en ejercicio id: {}", usuarioId, ejercicioId);
        securityUtils.checkOwnership(usuarioId);

        return progresoEjercicioRepository.existsByUsuarioIdAndEjercicioId(usuarioId, ejercicioId);
    }

    // Actualiza parcialmente un progreso de ejercicio (solo los campos no nulos del patchDTO).
    @Transactional
    @Override
    public ProgresoEjercicioDTO patch(Integer id, ProgresoEjercicioPatchDTO patchDTO) {
        logger.info("Aplicando patch a progreso de ejercicio con id: {}", id);

        ProgresoEjercicio progreso = progresoEjercicioRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("El progreso de ejercicio con id " + id + " no existe"));

        securityUtils.checkOwnership(progreso.getUsuario().getId());

        try {
            if (patchDTO.getFecha() != null) progreso.setFecha(patchDTO.getFecha());
            if (patchDTO.getMejorPeso() != null) progreso.setMejorPeso(patchDTO.getMejorPeso());
            if (patchDTO.getMejorRepeticiones() != null) progreso.setMejorRepeticiones(patchDTO.getMejorRepeticiones());
            if (patchDTO.getMejorTiempoSegundos() != null) progreso.setMejorTiempoSegundos(patchDTO.getMejorTiempoSegundos());
            if (patchDTO.getNotas() != null) progreso.setNotas(patchDTO.getNotas());

            return progresoEjercicioMapper.toDTO(progresoEjercicioRepository.save(progreso));
        } catch (Exception e) {
            throw new UpdateEntityException(ProgresoEjercicio.class.getSimpleName(), id, e);
        }
    }
}
