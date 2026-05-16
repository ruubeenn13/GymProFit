package com.gymprofit.api.service.objetivopersonal;

import com.gymprofit.api.dto.entity.objetivopersonal.ObjetivoPersonalCreateDTO;
import com.gymprofit.api.dto.entity.objetivopersonal.ObjetivoPersonalDTO;
import com.gymprofit.api.dto.entity.objetivopersonal.ObjetivoPersonalPatchDTO;
import com.gymprofit.api.dto.entity.objetivopersonal.ObjetivoPersonalUpdateDTO;
import com.gymprofit.api.entity.ObjetivoPersonal;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.enums.TipoObjetivo;
import com.gymprofit.api.exceptions.*;
import com.gymprofit.api.mappers.ObjetivoPersonalMapper;
import com.gymprofit.api.repository.jpa.IObjetivoPersonalRepository;
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
public class ObjetivoPersonalService implements IObjetivoPersonalService{

    private final IObjetivoPersonalRepository objetivoPersonalRepository;
    private final IUsuarioRepository usuarioRepository;
    private final ObjetivoPersonalMapper objetivoPersonalMapper;
    private final ILogroService logroService;
    private final Logger logger = LoggerFactory.getLogger(ObjetivoPersonalService.class);


    @Override
    public List<ObjetivoPersonalDTO> findAll() {
        logger.info("Buscando todos los objetivos personales");

        List<ObjetivoPersonal> objetivosPersonales = (List<ObjetivoPersonal>) objetivoPersonalRepository.findAll();

        return objetivoPersonalMapper.toDTOList(objetivosPersonales);
    }

    @Override
    public ObjetivoPersonalDTO findById(Integer id) {
        logger.info("Buscando objetivo personal por id: {}", id);

        ObjetivoPersonal objetivoPersonal = objetivoPersonalRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("El objetivo personal con id " + id + " no existe"));

        return objetivoPersonalMapper.toDTO(objetivoPersonal);
    }

    @Transactional
    @Override
    public ObjetivoPersonalDTO save(ObjetivoPersonalCreateDTO objetivoPersonalCreateDTO) {
        logger.info("Creando un nuevo objetivo personal para usuario id: {}", objetivoPersonalCreateDTO.getUsuarioId());

        Usuario usuario = usuarioRepository.findById(objetivoPersonalCreateDTO.getUsuarioId())
                .orElseThrow(() -> new NotFoundEntityException("El usuario con id " + objetivoPersonalCreateDTO.getUsuarioId() + " no existe"));

        try {
            ObjetivoPersonal objetivoPersonal = objetivoPersonalMapper.toEntity(objetivoPersonalCreateDTO);
            objetivoPersonal.setUsuario(usuario);
            objetivoPersonal.setFechaInicio(LocalDate.now());
            objetivoPersonal.setCompletado(false);

            ObjetivoPersonal objetivoGuardado = objetivoPersonalRepository.save(objetivoPersonal);

            ObjetivoPersonal objetivoRecargado = objetivoPersonalRepository.findById(objetivoGuardado.getId())
                    .orElseThrow(() -> new NotFoundEntityException("Error al recuperar el objetivo personal guardado"));

            return objetivoPersonalMapper.toDTO(objetivoRecargado);
        } catch (NotFoundEntityException e) {
            throw  e;
        } catch (Exception e) {
            throw new CreateEntityException(ObjetivoPersonal.class.getSimpleName(), objetivoPersonalCreateDTO, e);
        }
    }

    @Override
    public ObjetivoPersonalDTO update(ObjetivoPersonalUpdateDTO objetivoPersonalUpdateDTO) {
        logger.info("Actualizando el objetivo personal con id: {}", objetivoPersonalUpdateDTO.getId());

        ObjetivoPersonal objetivoPersonal = objetivoPersonalRepository.findById(objetivoPersonalUpdateDTO.getId())
                .orElseThrow(() -> new NotFoundEntityException("El objetivo personal con id " + objetivoPersonalUpdateDTO.getId() + " no existe"));

        try {
            objetivoPersonal.setValorObjetivo(objetivoPersonalUpdateDTO.getValorObjetivo());
            objetivoPersonal.setCompletado(objetivoPersonalUpdateDTO.isCompletado());

            ObjetivoPersonal objetivoActualizado = objetivoPersonalRepository.save(objetivoPersonal);

            return objetivoPersonalMapper.toDTO(objetivoActualizado);
        } catch (Exception e) {
            throw new UpdateEntityException(ObjetivoPersonal.class.getSimpleName(), objetivoPersonalUpdateDTO, e);
        }
    }

    @Transactional
    @Override
    public void deleteById(Integer id) {
        logger.info("Eliminando objetivo personal con id: {}", id);

        ObjetivoPersonal objetivoPersonal = objetivoPersonalRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("El objetivo personal con id " + id + " no existe"));

        try {
            objetivoPersonalRepository.delete(objetivoPersonal);

            logger.info("Objetivo personal con id {} eliminado correctamente", id);
        } catch (Exception e) {
            throw new DeleteEntityException(ObjetivoPersonal.class.getSimpleName(), id, e);
        }
    }

    @Override
    public List<ObjetivoPersonalDTO> findByUsuarioId(Integer usuarioId) {
        logger.info("Buscando objetivos personales del usuario id: {}", usuarioId);

        List<ObjetivoPersonal> objetivosPersonales = objetivoPersonalRepository.findByUsuarioId(usuarioId);

        return objetivoPersonalMapper.toDTOList(objetivosPersonales);
    }

    @Override
    public List<ObjetivoPersonalDTO> findByUsuarioIdOrdenados(Integer usuarioId) {
        logger.info("Buscando objetivos personales del usuario id: {} ordenados por fecha", usuarioId);

        List<ObjetivoPersonal> objetivosPersonales = objetivoPersonalRepository.findByUsuarioIdOrderByFechaInicioDesc(usuarioId);

        return objetivoPersonalMapper.toDTOList(objetivosPersonales);
    }

    @Override
    public List<ObjetivoPersonalDTO> findPendientesByUsuarioId(Integer usuarioId) {
        logger.info("Buscando objetivos personales pendientes del usuario id: {}", usuarioId);

        List<ObjetivoPersonal> objetivosPersonales = objetivoPersonalRepository.findByUsuarioIdAndCompletadoFalse(usuarioId);

        return objetivoPersonalMapper.toDTOList(objetivosPersonales);
    }

    @Override
    public List<ObjetivoPersonalDTO> findCompletadosByUsuarioId(Integer usuarioId) {
        logger.info("Buscando objetivos personales completados del usuario id: {}", usuarioId);

        List<ObjetivoPersonal> objetivosPersonales = objetivoPersonalRepository.findByUsuarioIdAndCompletadoTrue(usuarioId);

        return objetivoPersonalMapper.toDTOList(objetivosPersonales);
    }

    @Override
    public List<ObjetivoPersonalDTO> findByTipoObjetivo(String tipoObjetivo) {
        logger.info("Buscando objetivos por tipo: {}", tipoObjetivo);

        TipoObjetivo tipo = TipoObjetivo.valueOf(tipoObjetivo.toUpperCase());

        List<ObjetivoPersonal> objetivosPersonales = objetivoPersonalRepository.findByTipoObjetivo(tipo);

        return objetivoPersonalMapper.toDTOList(objetivosPersonales);
    }

    @Override
    public ObjetivoPersonalDTO completar(Integer id) {
        logger.info("Completando objetivo personal con id: {}", id);

        ObjetivoPersonal objetivoPersonal = objetivoPersonalRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("El objetivo personal con id " + id + " no existe"));

        if (objetivoPersonal.getCompletado()) {
            throw new ObjetivoAlreadyCompletedException("El objetivo personal con id " + id + " ya está completado");
        }

        objetivoPersonal.setCompletado(true);
        objetivoPersonal.setFechaCompletado(LocalDateTime.now());

        ObjetivoPersonal objetivoActualizado = objetivoPersonalRepository.save(objetivoPersonal);

        logroService.evaluarLogros(objetivoActualizado.getUsuario().getId());

        return objetivoPersonalMapper.toDTO(objetivoActualizado);
    }

    @Override
    public Long countByUsuarioId(Integer usuarioId) {
        logger.info("Contando objetivos del usuario id: {}", usuarioId);

        return objetivoPersonalRepository.countByUsuarioId(usuarioId);
    }

    @Override
    public Long countCompletadosByUsuarioId(Integer usuarioId) {
        logger.info("Contando objetivos completados del usuario id: {}", usuarioId);

        return objetivoPersonalRepository.countByUsuarioIdAndCompletadoTrue(usuarioId);
    }

    @Override
    public Long countPendientesByUsuarioId(Integer usuarioId) {
        logger.info("Contando objetivos pendientes del usuario id: {}", usuarioId);

        return objetivoPersonalRepository.countByUsuarioIdAndCompletadoFalse(usuarioId);
    }

    @Transactional
    @Override
    public ObjetivoPersonalDTO patch(Integer id, ObjetivoPersonalPatchDTO patchDTO) {
        logger.info("Aplicando patch a objetivo personal con id: {}", id);

        ObjetivoPersonal objetivo = objetivoPersonalRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("El objetivo personal con id " + id + " no existe"));

        try {
            if (patchDTO.getDescripcion() != null) objetivo.setDescripcion(patchDTO.getDescripcion());
            if (patchDTO.getValorActual() != null) objetivo.setValorActual(patchDTO.getValorActual());
            if (patchDTO.getValorObjetivo() != null) objetivo.setValorObjetivo(patchDTO.getValorObjetivo());
            if (patchDTO.getUnidad() != null) objetivo.setUnidad(patchDTO.getUnidad());
            if (patchDTO.getFechaLimite() != null) objetivo.setFechaLimite(patchDTO.getFechaLimite());

            return objetivoPersonalMapper.toDTO(objetivoPersonalRepository.save(objetivo));
        } catch (Exception e) {
            throw new UpdateEntityException(ObjetivoPersonal.class.getSimpleName(), id, e);
        }
    }
}