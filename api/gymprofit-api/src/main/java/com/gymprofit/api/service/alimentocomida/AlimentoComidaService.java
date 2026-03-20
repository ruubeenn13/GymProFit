package com.gymprofit.api.service.alimentocomida;

import com.gymprofit.api.dto.entity.alimentocomida.AlimentoComidaCreateDTO;
import com.gymprofit.api.dto.entity.alimentocomida.AlimentoComidaDTO;
import com.gymprofit.api.entity.Alimento;
import com.gymprofit.api.entity.AlimentoComida;
import com.gymprofit.api.entity.Comida;
import com.gymprofit.api.exceptions.*;
import com.gymprofit.api.mappers.AlimentoComidaMapper;
import com.gymprofit.api.repository.jpa.IAlimentoComidaRepository;
import com.gymprofit.api.repository.jpa.IAlimentoRepository;
import com.gymprofit.api.repository.jpa.IComidaRepository;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@AllArgsConstructor
public class AlimentoComidaService implements IAlimentoComidaService {

    private final IAlimentoComidaRepository alimentoComidaRepository;
    private final IComidaRepository comidaRepository;
    private final IAlimentoRepository alimentoRepository;
    private final AlimentoComidaMapper alimentoComidaMapper;
    private final Logger logger = LoggerFactory.getLogger(AlimentoComidaService.class);


    @Override
    public List<AlimentoComidaDTO> findAll() {
        logger.info("Buscando todos los alimentos-comidas");

        List<AlimentoComida> alimentosComida = (List<AlimentoComida>) alimentoComidaRepository.findAll();

        return alimentoComidaMapper.toDTOList(alimentosComida);
    }

    @Override
    public AlimentoComidaDTO findById(Integer id) {
        logger.info("Buscando alimento-comida por id: {}", id);

        AlimentoComida alimentoComida = alimentoComidaRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("El alimento-comida con id " + id + " no existe"));

        return alimentoComidaMapper.toDTO(alimentoComida);
    }

    @Override
    public AlimentoComidaDTO save(AlimentoComidaCreateDTO alimentoComidaCreateDTO) {
        logger.info("Creando nuevo alimento-comida para comida id: {} y alimento id: {}", alimentoComidaCreateDTO.getAlimentoId(), alimentoComidaCreateDTO.getComidaId());

        try {
            Comida comida = comidaRepository.findById(alimentoComidaCreateDTO.getComidaId())
                    .orElseThrow(() -> new NotFoundEntityException("La comida con id " + alimentoComidaCreateDTO.getComidaId() + " no existe"));

            Alimento alimento = alimentoRepository.findById(alimentoComidaCreateDTO.getAlimentoId())
                    .orElseThrow(() -> new NotFoundEntityException("El alimento con id " + alimentoComidaCreateDTO.getAlimentoId() + " no existe"));

            if (alimentoComidaRepository.existsByComidaIdAndAlimentoId(
                    alimentoComidaCreateDTO.getComidaId(),
                    alimentoComidaCreateDTO.getAlimentoId())) {
                throw new DuplicateEntityException("El alimento ya está asociado a esta comida");
            }

            AlimentoComida alimentoComida = alimentoComidaMapper.toEntity(alimentoComidaCreateDTO);
            alimentoComida.setComida(comida);
            alimentoComida.setAlimento(alimento);

            Integer caloriasTotales = calcularCalorias(alimento, alimentoComidaCreateDTO.getCantidadGramos());
            alimentoComida.setCaloriasTotales(caloriasTotales);

            AlimentoComida alimentoComidaGuardado = alimentoComidaRepository.save(alimentoComida);

            return alimentoComidaMapper.toDTO(alimentoComidaGuardado);
        } catch (NotFoundEntityException | DuplicateEntityException e) {
            throw e;
        } catch (Exception e) {
            throw new CreateEntityException(AlimentoComida.class.getSimpleName(), alimentoComidaCreateDTO, e);
        }
    }

    @Override
    public AlimentoComidaDTO modify(AlimentoComidaDTO alimentoComidaDTO) {
        logger.info("Modificando alimento-comida con id: {}", alimentoComidaDTO.getId());

        AlimentoComida alimentoComida = alimentoComidaRepository.findById(alimentoComidaDTO.getId())
                .orElseThrow(() -> new NotFoundEntityException("El alimento-comida con id " + alimentoComidaDTO.getId() + " no existe"));

        try {
            Comida comida = comidaRepository.findById(alimentoComidaDTO.getComidaId())
                    .orElseThrow(() -> new NotFoundEntityException("La comida con id " + alimentoComidaDTO.getComidaId() + " no existe"));

            Alimento alimento = alimentoRepository.findById(alimentoComidaDTO.getAlimentoId())
                    .orElseThrow(() -> new NotFoundEntityException("El alimento con id " + alimentoComidaDTO.getAlimentoId() + " no existe"));

            alimentoComida.setComida(comida);
            alimentoComida.setAlimento(alimento);
            alimentoComida.setCantidadGramos(alimentoComidaDTO.getCantidadGramos());

            Integer caloriasTotales = calcularCalorias(alimento, alimentoComidaDTO.getCantidadGramos());
            alimentoComida.setCaloriasTotales(caloriasTotales);

            AlimentoComida alimentoComidaActualizado = alimentoComidaRepository.save(alimentoComida);

            return alimentoComidaMapper.toDTO(alimentoComidaActualizado);
        } catch (NotFoundEntityException e) {
            throw e;
        } catch (Exception e) {
            throw new UpdateEntityException(AlimentoComida.class.getSimpleName(), alimentoComidaDTO, e);
        }
    }

    @Transactional
    @Override
    public void deleteById(Integer id) {
        logger.info("Eliminando alimnto-comida con id: {}", id);

        AlimentoComida alimentoComida = alimentoComidaRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("El alimento-comida con id " + id + " no existe"));

        try {
            alimentoComidaRepository.delete(alimentoComida);

            logger.info("Alimento-comida con id {} eliminado correctamente", id);
        } catch (Exception e) {
            throw new DeleteEntityException(AlimentoComida.class.getSimpleName(), id, e);
        }
    }

    @Override
    public List<AlimentoComidaDTO> findByComidaId(Integer comidaId) {
        logger.info("Buscando alimentos de la comida id: {}", comidaId);

        List<AlimentoComida> alimentosComida = alimentoComidaRepository.findByComidaId(comidaId);

        return alimentoComidaMapper.toDTOList(alimentosComida);
    }

    @Override
    public List<AlimentoComidaDTO> findByAlimentoId(Integer alimentoId) {
        logger.info("Buscando comidas que contienen el alimento id: {}", alimentoId);

        List<AlimentoComida> alimentosComida = alimentoComidaRepository.findByAlimentoId(alimentoId);

        return alimentoComidaMapper.toDTOList(alimentosComida);
    }

    @Override
    public AlimentoComidaDTO findByComidaIdAndAlimentoId(Integer comidaId, Integer alimentoId) {
        logger.info("Buscando relación entre comida {} y alimento {}", comidaId, alimentoId);

        AlimentoComida alimentoComida = alimentoComidaRepository.findByComidaIdAndAlimentoId(comidaId, alimentoId)
                .orElseThrow(() -> new NotFoundEntityException("No existe relación entre la comida " + comidaId + " y el alimento " + alimentoId));

        return alimentoComidaMapper.toDTO(alimentoComida);
    }

    @Transactional
    @Override
    public void deleteByComidaId(Integer comidaId) {
        logger.info("Eliminando todos los alimentos de la comida id: {}", comidaId);

        try {
            alimentoComidaRepository.deleteByComidaId(comidaId);

            logger.info("Alimentos de la comida {} eliminados correctamente", comidaId);
        } catch (Exception e) {
            throw new DeleteEntityException("AlimentoComida", comidaId, e);
        }
    }

    @Transactional
    @Override
    public void deleteByComidaIdAndAlimentoId(Integer comidaId, Integer alimentoId) {
        logger.info("Eliminando alimento {} de la comida {}", alimentoId, comidaId);

        try {
            alimentoComidaRepository.deleteByComidaIdAndAlimentoId(comidaId, alimentoId);

            logger.info("Alimento {} eliminado de la comida {} correctamente", alimentoId, comidaId);
        } catch (Exception e) {
            throw new DeleteEntityException("AlimentoComida", comidaId, e);
        }
    }

    @Override
    public boolean existsByComidaIdAndAlimentoId(Integer comidaId, Integer alimentoId) {
        logger.info("Verificando si existe la relación entre la comida {} y el alimento {}", comidaId, alimentoId);

        return alimentoComidaRepository.existsByComidaIdAndAlimentoId(comidaId, alimentoId);
    }

    @Override
    public Long countByComidaId(Integer comidaId) {
        logger.info("Contando alimentos de la comida id: {}", comidaId);

        return alimentoComidaRepository.countByComidaId(comidaId);
    }

    @Override
    public Long countByAlimentoId(Integer alimentoId) {
        logger.info("Contando comidas que contienen el alimento id: {}", alimentoId);

        return alimentoComidaRepository.countByAlimentoId(alimentoId);
    }

    private Integer calcularCalorias(Alimento alimento, BigDecimal cantidadGramos) {
        if (alimento.getCalorias() == null || alimento.getPorcionGramos() == null || cantidadGramos == null) {
            return 0;
        }

        double calorias = alimento.getCalorias();
        double porcion = alimento.getPorcionGramos();
        double cantidad = cantidadGramos.doubleValue();

        double caloriasTotales = (calorias * cantidad) / porcion;

        return (int) Math.round(caloriasTotales);
    }
}
