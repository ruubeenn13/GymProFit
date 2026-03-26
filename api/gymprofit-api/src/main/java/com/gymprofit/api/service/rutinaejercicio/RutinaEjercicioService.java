package com.gymprofit.api.service.rutinaejercicio;

import com.gymprofit.api.dto.entity.rutinaejercicio.RutinaEjercicioCreateDTO;
import com.gymprofit.api.dto.entity.rutinaejercicio.RutinaEjercicioDTO;
import com.gymprofit.api.entity.Ejercicio;
import com.gymprofit.api.entity.Rutina;
import com.gymprofit.api.entity.RutinaEjercicio;
import com.gymprofit.api.exceptions.CreateEntityException;
import com.gymprofit.api.exceptions.DeleteEntityException;
import com.gymprofit.api.exceptions.NotFoundEntityException;
import com.gymprofit.api.exceptions.UpdateEntityException;
import com.gymprofit.api.mappers.RutinaEjercicioMapper;
import com.gymprofit.api.repository.jpa.IEjercicioRepository;
import com.gymprofit.api.repository.jpa.IRutinaEjercicioRepository;
import com.gymprofit.api.repository.jpa.IRutinaRepository;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@AllArgsConstructor
public class RutinaEjercicioService implements IRutinaEjercicioService {

    private final IRutinaEjercicioRepository rutinaEjercicioRepository;
    private final IRutinaRepository rutinaRepository;
    private final IEjercicioRepository ejercicioRepository;
    private final RutinaEjercicioMapper rutinaEjercicioMapper;
    private final Logger logger = LoggerFactory.getLogger(RutinaEjercicioService.class);

    @Override
    public List<RutinaEjercicioDTO> findAll() {
        logger.info("Buscando todos los ejercicios de rutinas");

        List<RutinaEjercicio> lista = (List<RutinaEjercicio>) rutinaEjercicioRepository.findAll();

        return rutinaEjercicioMapper.toDTOList(lista);
    }

    @Override
    public RutinaEjercicioDTO findById(Integer id) {
        logger.info("Buscando ejercicio de rutina por id: {}", id);

        RutinaEjercicio rutinaEjercicio = rutinaEjercicioRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("El ejercicio de rutina con id " + id + " no existe"));

        return rutinaEjercicioMapper.toDTO(rutinaEjercicio);
    }

    @Override
    public RutinaEjercicioDTO save(RutinaEjercicioCreateDTO createDTO) {
        logger.info("Añadiendo ejercicio id: {} a rutina id: {}", createDTO.getEjercicioId(), createDTO.getRutinaId());

        Rutina rutina = rutinaRepository.findById(createDTO.getRutinaId())
                .orElseThrow(() -> new NotFoundEntityException("La rutina con id " + createDTO.getRutinaId() + " no existe"));

        Ejercicio ejercicio = ejercicioRepository.findById(createDTO.getEjercicioId())
                .orElseThrow(() -> new NotFoundEntityException("El ejercicio con id " + createDTO.getEjercicioId() + " no existe"));

        try {
            RutinaEjercicio rutinaEjercicio = rutinaEjercicioMapper.toEntity(createDTO);
            rutinaEjercicio.setRutina(rutina);
            rutinaEjercicio.setEjercicio(ejercicio);

            RutinaEjercicio guardado = rutinaEjercicioRepository.save(rutinaEjercicio);

            RutinaEjercicio recargado = rutinaEjercicioRepository.findById(guardado.getId())
                    .orElseThrow(() -> new NotFoundEntityException("Error al recuperar el ejercicio de rutina guardado"));

            return rutinaEjercicioMapper.toDTO(recargado);
        } catch (Exception e) {
            throw new CreateEntityException(RutinaEjercicio.class.getSimpleName(), createDTO, e);
        }
    }

    @Override
    public RutinaEjercicioDTO modify(RutinaEjercicioDTO dto) {
        logger.info("Modificando ejercicio de rutina con id: {}", dto.getId());

        RutinaEjercicio rutinaEjercicio = rutinaEjercicioRepository.findById(dto.getId())
                .orElseThrow(() -> new NotFoundEntityException("El ejercicio de rutina con id " + dto.getId() + " no existe"));

        try {
            rutinaEjercicio.setSeries(dto.getSeries());
            rutinaEjercicio.setRepeticiones(dto.getRepeticiones());
            rutinaEjercicio.setPesoRecomendado(dto.getPesoRecomendado());
            rutinaEjercicio.setTiempoDescanso(dto.getTiempoDescanso());
            rutinaEjercicio.setOrden(dto.getOrden());
            rutinaEjercicio.setNotas(dto.getNotas());

            RutinaEjercicio actualizado = rutinaEjercicioRepository.save(rutinaEjercicio);

            return rutinaEjercicioMapper.toDTO(actualizado);
        } catch (Exception e) {
            throw new UpdateEntityException(RutinaEjercicio.class.getSimpleName(), dto, e);
        }
    }

    @Transactional
    @Override
    public void deleteById(Integer id) {
        logger.info("Eliminando ejercicio de rutina con id: {}", id);

        RutinaEjercicio rutinaEjercicio = rutinaEjercicioRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("El ejercicio de rutina con id " + id + " no existe"));

        try {
            rutinaEjercicioRepository.delete(rutinaEjercicio);

            logger.info("Ejercicio de rutina con id {} eliminado correctamente", id);
        } catch (Exception e) {
            throw new DeleteEntityException(RutinaEjercicio.class.getSimpleName(), id, e);
        }
    }

    @Override
    public List<RutinaEjercicioDTO> findByRutinaId(Integer rutinaId) {
        logger.info("Buscando ejercicios de rutina id: {}", rutinaId);

        List<RutinaEjercicio> lista = rutinaEjercicioRepository.findByRutinaId(rutinaId);

        return rutinaEjercicioMapper.toDTOList(lista);
    }

    @Override
    public List<RutinaEjercicioDTO> findByEjercicioId(Integer ejercicioId) {
        logger.info("Buscando rutinas que contienen el ejercicio id: {}", ejercicioId);

        List<RutinaEjercicio> lista = rutinaEjercicioRepository.findByEjercicioId(ejercicioId);

        return rutinaEjercicioMapper.toDTOList(lista);
    }

    @Override
    public List<RutinaEjercicioDTO> findByRutinaIdOrdenado(Integer rutinaId) {
        logger.info("Buscando ejercicios de rutina id: {} ordenados por posición", rutinaId);

        List<RutinaEjercicio> lista = rutinaEjercicioRepository.findByRutinaIdOrderByOrdenAsc(rutinaId);

        return rutinaEjercicioMapper.toDTOList(lista);
    }

    @Override
    public RutinaEjercicioDTO findByRutinaIdAndEjercicioId(Integer rutinaId, Integer ejercicioId) {
        logger.info("Buscando ejercicio id: {} en rutina id: {}", ejercicioId, rutinaId);

        RutinaEjercicio rutinaEjercicio = rutinaEjercicioRepository.findByRutinaIdAndEjercicioId(rutinaId, ejercicioId)
                .orElseThrow(() -> new NotFoundEntityException("No existe el ejercicio " + ejercicioId + " en la rutina " + rutinaId));

        return rutinaEjercicioMapper.toDTO(rutinaEjercicio);
    }

    @Override
    public Long countByRutinaId(Integer rutinaId) {
        logger.info("Contando ejercicios de rutina id: {}", rutinaId);

        return rutinaEjercicioRepository.countByRutinaId(rutinaId);
    }

    @Override
    public Long countByEjercicioId(Integer ejercicioId) {
        logger.info("Contando rutinas que contienen el ejercicio id: {}", ejercicioId);

        return rutinaEjercicioRepository.countByEjercicioId(ejercicioId);
    }

    @Transactional
    @Override
    public void deleteByRutinaId(Integer rutinaId) {
        logger.info("Eliminando todos los ejercicios de rutina id: {}", rutinaId);

        try {
            rutinaEjercicioRepository.deleteByRutinaId(rutinaId);

            logger.info("Ejercicios de rutina id {} eliminados correctamente", rutinaId);
        } catch (Exception e) {
            throw new DeleteEntityException(RutinaEjercicio.class.getSimpleName(), rutinaId, e);
        }
    }

    @Transactional
    @Override
    public void deleteByRutinaIdAndEjercicioId(Integer rutinaId, Integer ejercicioId) {
        logger.info("Eliminando ejercicio id: {} de rutina id: {}", ejercicioId, rutinaId);

        try {
            rutinaEjercicioRepository.deleteByRutinaIdAndEjercicioId(rutinaId, ejercicioId);

            logger.info("Ejercicio {} eliminado de rutina {} correctamente", ejercicioId, rutinaId);
        } catch (Exception e) {
            throw new DeleteEntityException(RutinaEjercicio.class.getSimpleName(), rutinaId, e);
        }
    }

    @Override
    public boolean existsByRutinaIdAndEjercicioId(Integer rutinaId, Integer ejercicioId) {
        logger.info("Verificando si existe ejercicio id: {} en rutina id: {}", ejercicioId, rutinaId);

        return rutinaEjercicioRepository.existsByRutinaIdAndEjercicioId(rutinaId, ejercicioId);
    }
}