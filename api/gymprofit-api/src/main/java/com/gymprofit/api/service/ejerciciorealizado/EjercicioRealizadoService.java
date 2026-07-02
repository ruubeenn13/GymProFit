package com.gymprofit.api.service.ejerciciorealizado;

import com.gymprofit.api.dto.entity.ejerciciorealizado.EjercicioRealizadoCreateDTO;
import com.gymprofit.api.dto.entity.ejerciciorealizado.EjercicioRealizadoDTO;
import com.gymprofit.api.dto.entity.ejerciciorealizado.EjercicioRealizadoPatchDTO;
import com.gymprofit.api.entity.Ejercicio;
import com.gymprofit.api.entity.EjercicioRealizado;
import com.gymprofit.api.entity.SesionEntrenamiento;
import com.gymprofit.api.config.security.SecurityUtils;
import com.gymprofit.api.exceptions.CreateEntityException;
import com.gymprofit.api.exceptions.DeleteEntityException;
import com.gymprofit.api.exceptions.NotFoundEntityException;
import com.gymprofit.api.exceptions.UpdateEntityException;
import com.gymprofit.api.mappers.EjercicioRealizadoMapper;
import com.gymprofit.api.repository.jpa.IEjercicioRealizadoRepository;
import com.gymprofit.api.repository.jpa.IEjercicioRepository;
import com.gymprofit.api.repository.jpa.ISesionEntrenamientoRepository;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@AllArgsConstructor
public class EjercicioRealizadoService implements IEjercicioRealizadoService{

    private final IEjercicioRealizadoRepository ejercicioRealizadoRepository;
    private final IEjercicioRepository ejercicioRepository;
    private final ISesionEntrenamientoRepository sesionEntrenamientoRepository;
    private final EjercicioRealizadoMapper ejercicioRealizadoMapper;
    private final SecurityUtils securityUtils;
    private final Logger logger = LoggerFactory.getLogger(EjercicioRealizadoService.class);


    @Override
    public List<EjercicioRealizadoDTO> findAll() {
        logger.info("Buscando todos los ejercicios realizados");

        securityUtils.requireAdmin();

        List<EjercicioRealizado> ejerciciosRealizados = (List<EjercicioRealizado>) ejercicioRealizadoRepository.findAll();

        return ejercicioRealizadoMapper.toDTOList(ejerciciosRealizados);
    }

    @Override
    public EjercicioRealizadoDTO findById(Integer id) {
        logger.info("Buscando ejercicio realizado por id: {}", id);

        EjercicioRealizado ejercicioRealizado = ejercicioRealizadoRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("El ejercicio realizado con id " + id + " no existe"));

        securityUtils.checkOwnership(ejercicioRealizado.getSesion().getUsuario().getId());

        return ejercicioRealizadoMapper.toDTO(ejercicioRealizado);
    }

    @Transactional
    @Override
    public EjercicioRealizadoDTO save(EjercicioRealizadoCreateDTO ejercicioRealizadoCreateDTO) {
        logger.info("Creando nuevo ejercicio realizado para sesión id: {}", ejercicioRealizadoCreateDTO.getSesionId());

        SesionEntrenamiento sesion = sesionEntrenamientoRepository.findById(ejercicioRealizadoCreateDTO.getSesionId())
                .orElseThrow(() -> new NotFoundEntityException("La sesión con id " + ejercicioRealizadoCreateDTO.getSesionId() + " no existe"));

        securityUtils.checkOwnership(sesion.getUsuario().getId());

        Ejercicio ejercicio = ejercicioRepository.findById(ejercicioRealizadoCreateDTO.getEjercicioId())
                .orElseThrow(() -> new NotFoundEntityException("El ejercicio con id " + ejercicioRealizadoCreateDTO.getEjercicioId() + " no existe"));

        try {
            EjercicioRealizado ejercicioRealizado = ejercicioRealizadoMapper.toEntity(ejercicioRealizadoCreateDTO);
            ejercicioRealizado.setSesion(sesion);
            ejercicioRealizado.setEjercicio(ejercicio);

            EjercicioRealizado ejercicioGuardado = ejercicioRealizadoRepository.save(ejercicioRealizado);

            return ejercicioRealizadoMapper.toDTO(ejercicioGuardado);
        } catch (Exception e) {
            throw new CreateEntityException(EjercicioRealizado.class.getSimpleName(), ejercicioRealizadoCreateDTO, e);
        }
    }

    @Override
    public EjercicioRealizadoDTO modify(EjercicioRealizadoDTO ejercicioRealizadoDTO) {
        logger.info("Modificando ejercicio realizado con id: {}", ejercicioRealizadoDTO.getId());

        EjercicioRealizado ejercicioRealizado = ejercicioRealizadoRepository.findById(ejercicioRealizadoDTO.getId())
                .orElseThrow(() -> new NotFoundEntityException("El ejercicio realizado con id " + ejercicioRealizadoDTO.getId() + " no existe"));

        securityUtils.checkOwnership(ejercicioRealizado.getSesion().getUsuario().getId());

        try {
            ejercicioRealizado.setSeriesCompletadas(ejercicioRealizadoDTO.getSeriesCompletadas());
            ejercicioRealizado.setRepeticionesReales(ejercicioRealizadoDTO.getRepeticionesReales());
            ejercicioRealizado.setPesoUsado(ejercicioRealizadoDTO.getPesoUsado());
            ejercicioRealizado.setTiempoSegundos(ejercicioRealizadoDTO.getTiempoSegundos());
            ejercicioRealizado.setNotas(ejercicioRealizadoDTO.getNotas());

            EjercicioRealizado actualizado = ejercicioRealizadoRepository.save(ejercicioRealizado);

            return ejercicioRealizadoMapper.toDTO(actualizado);
        } catch (Exception e) {
            throw new UpdateEntityException(EjercicioRealizado.class.getSimpleName(), ejercicioRealizadoDTO, e);
        }
    }

    @Transactional
    @Override
    public void deleteById(Integer id) {
        logger.info("Eliminando ejercicio realizado con id: {}", id);

        EjercicioRealizado ejercicioRealizado = ejercicioRealizadoRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("El ejercicio realizado con id " + id + " no existe"));

        securityUtils.checkOwnership(ejercicioRealizado.getSesion().getUsuario().getId());

        try {
            ejercicioRealizadoRepository.delete(ejercicioRealizado);

            logger.info("Ejercicio realizado con id {} eliminado correctamente", id);
        } catch (Exception e) {
            throw new DeleteEntityException(EjercicioRealizado.class.getSimpleName(), id, e);
        }
    }

    @Override
    public List<EjercicioRealizadoDTO> findBySesionId(Integer sesionId) {
        logger.info("Buscando ejercicios realizados por sesión id: {}", sesionId);

        SesionEntrenamiento sesion = sesionEntrenamientoRepository.findById(sesionId)
                .orElseThrow(() -> new NotFoundEntityException("La sesión con id " + sesionId + " no existe"));

        securityUtils.checkOwnership(sesion.getUsuario().getId());

        List<EjercicioRealizado> ejerciciosRealizados = ejercicioRealizadoRepository.findBySesionId(sesionId);

        return ejercicioRealizadoMapper.toDTOList(ejerciciosRealizados);
    }

    @Override
    public List<EjercicioRealizadoDTO> findByEjercicioId(Integer ejercicioId) {
        logger.info("Buscando ejercicios realizados por ejercicio di: {}", ejercicioId);

        List<EjercicioRealizado> ejerciciosRealizados = ejercicioRealizadoRepository.findByEjercicioId(ejercicioId);

        return ejercicioRealizadoMapper.toDTOList(ejerciciosRealizados);
    }

    @Override
    public List<EjercicioRealizadoDTO> findBySesionIdAndEjercicioId(Integer sesionId, Integer ejercicioId) {
        logger.info("Buscando ejercicios realizados por sesión id: {} y ejercicio id: {}", sesionId, ejercicioId);

        SesionEntrenamiento sesion = sesionEntrenamientoRepository.findById(sesionId)
                .orElseThrow(() -> new NotFoundEntityException("La sesión con id " + sesionId + " no existe"));

        securityUtils.checkOwnership(sesion.getUsuario().getId());

        List<EjercicioRealizado> ejerciciosRealizados = ejercicioRealizadoRepository.findBySesionIdAndEjercicioId(sesionId, ejercicioId);

        return ejercicioRealizadoMapper.toDTOList(ejerciciosRealizados);
    }

    @Override
    public Long countBySesionId(Integer sesionId) {
        logger.info("Contando ejercicios realizados por sesión id: {}", sesionId);

        return ejercicioRealizadoRepository.countBySesionId(sesionId);
    }

    @Override
    public Long countByEjercicioId(Integer ejercicioId) {
        logger.info("Contando ejercicios realizados por ejercicio id: {}", ejercicioId);

        return ejercicioRealizadoRepository.countByEjercicioId(ejercicioId);
    }

    @Transactional
    @Override
    public void deleteBySesionId(Integer sesionId) {
        logger.info("Eliminando ejercicios realizados por sesión id: {}", sesionId);

        SesionEntrenamiento sesion = sesionEntrenamientoRepository.findById(sesionId)
                .orElseThrow(() -> new NotFoundEntityException("La sesión con id " + sesionId + " no existe"));

        securityUtils.checkOwnership(sesion.getUsuario().getId());

        try {
            ejercicioRealizadoRepository.deleteBySesionId(sesionId);

            logger.info("Ejercicios realizados de la sesión {} eliminados correctamente", sesionId);
        } catch (Exception e) {
            throw new DeleteEntityException(EjercicioRealizado.class.getSimpleName(), sesionId, e);
        }
    }

    @Transactional
    @Override
    public void deleteBySesionIdAndEjercicioId(Integer sesionId, Integer ejercicioId) {
        logger.info("Eliminando ejercicios realizados por sesión id: {} y ejercicio id: {}", sesionId, ejercicioId);

        SesionEntrenamiento sesion = sesionEntrenamientoRepository.findById(sesionId)
                .orElseThrow(() -> new NotFoundEntityException("La sesión con id " + sesionId + " no existe"));

        securityUtils.checkOwnership(sesion.getUsuario().getId());

        try {
            ejercicioRealizadoRepository.deleteBySesionIdAndEjercicioId(sesionId, ejercicioId);

            logger.info("Ejercicios realizados de sesión {} y ejercicio {} eliminados correctamente", sesionId, ejercicioId);
        } catch (Exception e) {
            throw new DeleteEntityException(EjercicioRealizado.class.getSimpleName(), sesionId, e);
        }
    }

    @Override
    public boolean existsBySesionIdAndEjercicioId(Integer sesionId, Integer ejercicioId) {
        logger.info("Verificando si existe ejercicio realizado para sesión id: {} y ejercicio id: {}", sesionId, ejercicioId);

        return ejercicioRealizadoRepository.existsBySesionIdAndEjercicioId(sesionId, ejercicioId);
    }

    @Transactional
    @Override
    public EjercicioRealizadoDTO patch(Integer id, EjercicioRealizadoPatchDTO patchDTO) {
        logger.info("Aplicando patch a ejercicio realizado con id: {}", id);

        EjercicioRealizado ejercicioRealizado = ejercicioRealizadoRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("El ejercicio realizado con id " + id + " no existe"));

        securityUtils.checkOwnership(ejercicioRealizado.getSesion().getUsuario().getId());

        try {
            if (patchDTO.getSeriesCompletadas() != null) ejercicioRealizado.setSeriesCompletadas(patchDTO.getSeriesCompletadas());
            if (patchDTO.getRepeticionesReales() != null) ejercicioRealizado.setRepeticionesReales(patchDTO.getRepeticionesReales());
            if (patchDTO.getPesoUsado() != null) ejercicioRealizado.setPesoUsado(patchDTO.getPesoUsado());
            if (patchDTO.getTiempoSegundos() != null) ejercicioRealizado.setTiempoSegundos(patchDTO.getTiempoSegundos());
            if (patchDTO.getNotas() != null) ejercicioRealizado.setNotas(patchDTO.getNotas());

            return ejercicioRealizadoMapper.toDTO(ejercicioRealizadoRepository.save(ejercicioRealizado));
        } catch (Exception e) {
            throw new UpdateEntityException(EjercicioRealizado.class.getSimpleName(), id, e);
        }
    }
}
