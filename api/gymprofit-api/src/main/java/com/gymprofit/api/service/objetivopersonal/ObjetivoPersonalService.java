package com.gymprofit.api.service.objetivopersonal;

import com.gymprofit.api.dto.entity.objetivopersonal.ObjetivoPersonalCreateDTO;
import com.gymprofit.api.dto.entity.objetivopersonal.ObjetivoPersonalDTO;
import com.gymprofit.api.dto.entity.objetivopersonal.ObjetivoPersonalPatchDTO;
import com.gymprofit.api.dto.entity.objetivopersonal.ObjetivoPersonalUpdateDTO;
import com.gymprofit.api.config.security.SecurityUtils;
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

// ============================================================
// ObjetivoPersonalService — implementación del servicio de objetivos personales
// Gestiona la creación, actualización, completado y consulta de objetivos
// (peso, repeticiones, etc.) de cada usuario, comprobando propiedad y
// disparando la evaluación de logros al completarse un objetivo.
// ============================================================
@Service
@AllArgsConstructor
public class ObjetivoPersonalService implements IObjetivoPersonalService{

    private final IObjetivoPersonalRepository objetivoPersonalRepository;
    private final IUsuarioRepository usuarioRepository;
    private final ObjetivoPersonalMapper objetivoPersonalMapper;
    private final ILogroService logroService;
    private final SecurityUtils securityUtils;
    private final Logger logger = LoggerFactory.getLogger(ObjetivoPersonalService.class);


    // Devuelve todos los objetivos personales del sistema. Solo ADMIN.
    @Override
    public List<ObjetivoPersonalDTO> findAll() {
        securityUtils.requireAdmin();

        logger.info("Buscando todos los objetivos personales");

        List<ObjetivoPersonal> objetivosPersonales = (List<ObjetivoPersonal>) objetivoPersonalRepository.findAll();

        return objetivoPersonalMapper.toDTOList(objetivosPersonales);
    }

    // Busca un objetivo personal por id, verificando la propiedad del usuario.
    @Override
    public ObjetivoPersonalDTO findById(Integer id) {
        logger.info("Buscando objetivo personal por id: {}", id);

        ObjetivoPersonal objetivoPersonal = objetivoPersonalRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("El objetivo personal con id " + id + " no existe"));

        securityUtils.checkOwnership(objetivoPersonal.getUsuario().getId());

        return objetivoPersonalMapper.toDTO(objetivoPersonal);
    }

    // Crea un objetivo personal nuevo, con fecha de inicio actual y sin completar.
    @Transactional
    @Override
    public ObjetivoPersonalDTO save(ObjetivoPersonalCreateDTO objetivoPersonalCreateDTO) {
        if (!securityUtils.isAdmin()) objetivoPersonalCreateDTO.setUsuarioId(securityUtils.getCurrentUserId());

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

    // Actualiza el valor objetivo y el estado de completado de un objetivo existente.
    @Override
    public ObjetivoPersonalDTO update(ObjetivoPersonalUpdateDTO objetivoPersonalUpdateDTO) {
        logger.info("Actualizando el objetivo personal con id: {}", objetivoPersonalUpdateDTO.getId());

        ObjetivoPersonal objetivoPersonal = objetivoPersonalRepository.findById(objetivoPersonalUpdateDTO.getId())
                .orElseThrow(() -> new NotFoundEntityException("El objetivo personal con id " + objetivoPersonalUpdateDTO.getId() + " no existe"));

        securityUtils.checkOwnership(objetivoPersonal.getUsuario().getId());

        try {
            objetivoPersonal.setValorObjetivo(objetivoPersonalUpdateDTO.getValorObjetivo());
            objetivoPersonal.setCompletado(objetivoPersonalUpdateDTO.isCompletado());

            ObjetivoPersonal objetivoActualizado = objetivoPersonalRepository.save(objetivoPersonal);

            return objetivoPersonalMapper.toDTO(objetivoActualizado);
        } catch (Exception e) {
            throw new UpdateEntityException(ObjetivoPersonal.class.getSimpleName(), objetivoPersonalUpdateDTO, e);
        }
    }

    // Elimina definitivamente un objetivo personal tras comprobar la propiedad.
    @Transactional
    @Override
    public void deleteById(Integer id) {
        logger.info("Eliminando objetivo personal con id: {}", id);

        ObjetivoPersonal objetivoPersonal = objetivoPersonalRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("El objetivo personal con id " + id + " no existe"));

        securityUtils.checkOwnership(objetivoPersonal.getUsuario().getId());

        try {
            objetivoPersonalRepository.delete(objetivoPersonal);

            logger.info("Objetivo personal con id {} eliminado correctamente", id);
        } catch (Exception e) {
            throw new DeleteEntityException(ObjetivoPersonal.class.getSimpleName(), id, e);
        }
    }

    // Lista todos los objetivos personales de un usuario.
    @Override
    public List<ObjetivoPersonalDTO> findByUsuarioId(Integer usuarioId) {
        securityUtils.checkOwnership(usuarioId);

        logger.info("Buscando objetivos personales del usuario id: {}", usuarioId);

        List<ObjetivoPersonal> objetivosPersonales = objetivoPersonalRepository.findByUsuarioId(usuarioId);

        return objetivoPersonalMapper.toDTOList(objetivosPersonales);
    }

    // Lista los objetivos de un usuario ordenados por fecha de inicio descendente.
    @Override
    public List<ObjetivoPersonalDTO> findByUsuarioIdOrdenados(Integer usuarioId) {
        securityUtils.checkOwnership(usuarioId);

        logger.info("Buscando objetivos personales del usuario id: {} ordenados por fecha", usuarioId);

        List<ObjetivoPersonal> objetivosPersonales = objetivoPersonalRepository.findByUsuarioIdOrderByFechaInicioDesc(usuarioId);

        return objetivoPersonalMapper.toDTOList(objetivosPersonales);
    }

    // Lista solo los objetivos pendientes (no completados) de un usuario.
    @Override
    public List<ObjetivoPersonalDTO> findPendientesByUsuarioId(Integer usuarioId) {
        securityUtils.checkOwnership(usuarioId);

        logger.info("Buscando objetivos personales pendientes del usuario id: {}", usuarioId);

        List<ObjetivoPersonal> objetivosPersonales = objetivoPersonalRepository.findByUsuarioIdAndCompletadoFalse(usuarioId);

        return objetivoPersonalMapper.toDTOList(objetivosPersonales);
    }

    // Lista solo los objetivos ya completados de un usuario.
    @Override
    public List<ObjetivoPersonalDTO> findCompletadosByUsuarioId(Integer usuarioId) {
        securityUtils.checkOwnership(usuarioId);

        logger.info("Buscando objetivos personales completados del usuario id: {}", usuarioId);

        List<ObjetivoPersonal> objetivosPersonales = objetivoPersonalRepository.findByUsuarioIdAndCompletadoTrue(usuarioId);

        return objetivoPersonalMapper.toDTOList(objetivosPersonales);
    }

    // Lista objetivos por tipo (p.ej. PESO, REPETICIONES) en todo el sistema. Solo ADMIN.
    @Override
    public List<ObjetivoPersonalDTO> findByTipoObjetivo(String tipoObjetivo) {
        securityUtils.requireAdmin();

        logger.info("Buscando objetivos por tipo: {}", tipoObjetivo);

        TipoObjetivo tipo = TipoObjetivo.valueOf(tipoObjetivo.toUpperCase());

        List<ObjetivoPersonal> objetivosPersonales = objetivoPersonalRepository.findByTipoObjetivo(tipo);

        return objetivoPersonalMapper.toDTOList(objetivosPersonales);
    }

    // Marca el objetivo como completado, registra la fecha y evalúa logros del usuario.
    @Override
    public ObjetivoPersonalDTO completar(Integer id) {
        logger.info("Completando objetivo personal con id: {}", id);

        ObjetivoPersonal objetivoPersonal = objetivoPersonalRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("El objetivo personal con id " + id + " no existe"));

        securityUtils.checkOwnership(objetivoPersonal.getUsuario().getId());

        if (objetivoPersonal.getCompletado()) {
            throw new ObjetivoAlreadyCompletedException("El objetivo personal con id " + id + " ya está completado");
        }

        objetivoPersonal.setCompletado(true);
        objetivoPersonal.setFechaCompletado(LocalDateTime.now());

        ObjetivoPersonal objetivoActualizado = objetivoPersonalRepository.save(objetivoPersonal);

        logroService.evaluarLogros(objetivoActualizado.getUsuario().getId());

        return objetivoPersonalMapper.toDTO(objetivoActualizado);
    }

    // Cuenta el total de objetivos personales de un usuario.
    @Override
    public Long countByUsuarioId(Integer usuarioId) {
        securityUtils.checkOwnership(usuarioId);

        logger.info("Contando objetivos del usuario id: {}", usuarioId);

        return objetivoPersonalRepository.countByUsuarioId(usuarioId);
    }

    // Cuenta los objetivos completados de un usuario.
    @Override
    public Long countCompletadosByUsuarioId(Integer usuarioId) {
        securityUtils.checkOwnership(usuarioId);

        logger.info("Contando objetivos completados del usuario id: {}", usuarioId);

        return objetivoPersonalRepository.countByUsuarioIdAndCompletadoTrue(usuarioId);
    }

    // Cuenta los objetivos pendientes (no completados) de un usuario.
    @Override
    public Long countPendientesByUsuarioId(Integer usuarioId) {
        securityUtils.checkOwnership(usuarioId);

        logger.info("Contando objetivos pendientes del usuario id: {}", usuarioId);

        return objetivoPersonalRepository.countByUsuarioIdAndCompletadoFalse(usuarioId);
    }

    // Actualiza parcialmente un objetivo personal (solo los campos no nulos del patchDTO).
    @Transactional
    @Override
    public ObjetivoPersonalDTO patch(Integer id, ObjetivoPersonalPatchDTO patchDTO) {
        logger.info("Aplicando patch a objetivo personal con id: {}", id);

        ObjetivoPersonal objetivo = objetivoPersonalRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("El objetivo personal con id " + id + " no existe"));

        securityUtils.checkOwnership(objetivo.getUsuario().getId());

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