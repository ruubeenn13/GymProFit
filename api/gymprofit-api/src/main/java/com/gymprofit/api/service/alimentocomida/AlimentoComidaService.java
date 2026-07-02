package com.gymprofit.api.service.alimentocomida;

import com.gymprofit.api.dto.entity.alimentocomida.AlimentoComidaCreateDTO;
import com.gymprofit.api.dto.entity.alimentocomida.AlimentoComidaDTO;
import com.gymprofit.api.dto.entity.alimentocomida.AlimentoComidaPatchDTO;
import com.gymprofit.api.entity.Alimento;
import com.gymprofit.api.entity.AlimentoComida;
import com.gymprofit.api.entity.Comida;
import com.gymprofit.api.config.security.SecurityUtils;
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
    private final SecurityUtils securityUtils;
    private final Logger logger = LoggerFactory.getLogger(AlimentoComidaService.class);


    @Override
    public List<AlimentoComidaDTO> findAll() {
        logger.info("Buscando todos los alimentos-comidas");
        securityUtils.requireAdmin();

        List<AlimentoComida> alimentosComida = (List<AlimentoComida>) alimentoComidaRepository.findAll();

        return alimentoComidaMapper.toDTOList(alimentosComida);
    }

    @Override
    public AlimentoComidaDTO findById(Integer id) {
        logger.info("Buscando alimento-comida por id: {}", id);

        AlimentoComida alimentoComida = alimentoComidaRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("El alimento-comida con id " + id + " no existe"));

        securityUtils.checkOwnership(alimentoComida.getComida().getUsuario().getId());

        return alimentoComidaMapper.toDTO(alimentoComida);
    }

    @Override
    public AlimentoComidaDTO save(AlimentoComidaCreateDTO alimentoComidaCreateDTO) {
        logger.info("Creando nuevo alimento-comida para comida id: {} y alimento id: {}", alimentoComidaCreateDTO.getAlimentoId(), alimentoComidaCreateDTO.getComidaId());

        try {
            Comida comida = comidaRepository.findById(alimentoComidaCreateDTO.getComidaId())
                    .orElseThrow(() -> new NotFoundEntityException("La comida con id " + alimentoComidaCreateDTO.getComidaId() + " no existe"));

            securityUtils.checkOwnership(comida.getUsuario().getId());

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
            recalcularTotalesComida(comida);

            return alimentoComidaMapper.toDTO(alimentoComidaGuardado);
        } catch (NotFoundEntityException | DuplicateEntityException | UnauthorizedException e) {
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

        securityUtils.checkOwnership(alimentoComida.getComida().getUsuario().getId());

        try {
            Comida comida = comidaRepository.findById(alimentoComidaDTO.getComidaId())
                    .orElseThrow(() -> new NotFoundEntityException("La comida con id " + alimentoComidaDTO.getComidaId() + " no existe"));

            securityUtils.checkOwnership(comida.getUsuario().getId());

            Alimento alimento = alimentoRepository.findById(alimentoComidaDTO.getAlimentoId())
                    .orElseThrow(() -> new NotFoundEntityException("El alimento con id " + alimentoComidaDTO.getAlimentoId() + " no existe"));

            alimentoComida.setComida(comida);
            alimentoComida.setAlimento(alimento);
            alimentoComida.setCantidadGramos(alimentoComidaDTO.getCantidadGramos());

            Integer caloriasTotales = calcularCalorias(alimento, alimentoComidaDTO.getCantidadGramos());
            alimentoComida.setCaloriasTotales(caloriasTotales);

            AlimentoComida alimentoComidaActualizado = alimentoComidaRepository.save(alimentoComida);
            recalcularTotalesComida(comida);

            return alimentoComidaMapper.toDTO(alimentoComidaActualizado);
        } catch (NotFoundEntityException | UnauthorizedException e) {
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

        Comida comida = alimentoComida.getComida();
        securityUtils.checkOwnership(comida.getUsuario().getId());
        try {
            alimentoComidaRepository.delete(alimentoComida);
            recalcularTotalesComida(comida);

            logger.info("Alimento-comida con id {} eliminado correctamente", id);
        } catch (Exception e) {
            throw new DeleteEntityException(AlimentoComida.class.getSimpleName(), id, e);
        }
    }

    @Override
    public List<AlimentoComidaDTO> findByComidaId(Integer comidaId) {
        logger.info("Buscando alimentos de la comida id: {}", comidaId);
        checkComidaOwnership(comidaId);

        List<AlimentoComida> alimentosComida = alimentoComidaRepository.findByComidaId(comidaId);

        return alimentoComidaMapper.toDTOList(alimentosComida);
    }

    @Override
    public List<AlimentoComidaDTO> findByAlimentoId(Integer alimentoId) {
        logger.info("Buscando comidas que contienen el alimento id: {}", alimentoId);
        securityUtils.requireAdmin();

        List<AlimentoComida> alimentosComida = alimentoComidaRepository.findByAlimentoId(alimentoId);

        return alimentoComidaMapper.toDTOList(alimentosComida);
    }

    @Override
    public AlimentoComidaDTO findByComidaIdAndAlimentoId(Integer comidaId, Integer alimentoId) {
        logger.info("Buscando relación entre comida {} y alimento {}", comidaId, alimentoId);
        checkComidaOwnership(comidaId);

        AlimentoComida alimentoComida = alimentoComidaRepository.findByComidaIdAndAlimentoId(comidaId, alimentoId)
                .orElseThrow(() -> new NotFoundEntityException("No existe relación entre la comida " + comidaId + " y el alimento " + alimentoId));

        return alimentoComidaMapper.toDTO(alimentoComida);
    }

    @Transactional
    @Override
    public void deleteByComidaId(Integer comidaId) {
        logger.info("Eliminando todos los alimentos de la comida id: {}", comidaId);
        checkComidaOwnership(comidaId);

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
        checkComidaOwnership(comidaId);

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
        checkComidaOwnership(comidaId);

        return alimentoComidaRepository.existsByComidaIdAndAlimentoId(comidaId, alimentoId);
    }

    @Override
    public Long countByComidaId(Integer comidaId) {
        logger.info("Contando alimentos de la comida id: {}", comidaId);
        checkComidaOwnership(comidaId);

        return alimentoComidaRepository.countByComidaId(comidaId);
    }

    @Override
    public Long countByAlimentoId(Integer alimentoId) {
        logger.info("Contando comidas que contienen el alimento id: {}", alimentoId);
        securityUtils.requireAdmin();

        return alimentoComidaRepository.countByAlimentoId(alimentoId);
    }

    @Transactional
    @Override
    public AlimentoComidaDTO patch(Integer id, AlimentoComidaPatchDTO patchDTO) {
        logger.info("Aplicando patch a alimento-comida con id: {}", id);

        AlimentoComida alimentoComida = alimentoComidaRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("El alimento-comida con id " + id + " no existe"));

        securityUtils.checkOwnership(alimentoComida.getComida().getUsuario().getId());

        try {
            if (patchDTO.getCantidadGramos() != null) {
                alimentoComida.setCantidadGramos(patchDTO.getCantidadGramos());
                alimentoComida.setCaloriasTotales(calcularCalorias(alimentoComida.getAlimento(), patchDTO.getCantidadGramos()));
            } else if (patchDTO.getCaloriasTotales() != null) {
                alimentoComida.setCaloriasTotales(patchDTO.getCaloriasTotales());
            }

            AlimentoComida saved = alimentoComidaRepository.save(alimentoComida);
            recalcularTotalesComida(saved.getComida());
            return alimentoComidaMapper.toDTO(saved);
        } catch (Exception e) {
            throw new UpdateEntityException(AlimentoComida.class.getSimpleName(), id, e);
        }
    }

    /**
     * Carga la comida indicada y verifica que el usuario autenticado es su propietario (o ADMIN).
     * Propiedad indirecta: el recurso alimento-comida hereda el owner de su comida.
     *
     * @param comidaId id de la comida a la que pertenece(rá) la línea.
     * @throws NotFoundEntityException si la comida no existe.
     * @throws com.gymprofit.api.exceptions.UnauthorizedException (→ 403) si no es propietario ni ADMIN.
     */
    private void checkComidaOwnership(Integer comidaId) {
        Comida comida = comidaRepository.findById(comidaId)
                .orElseThrow(() -> new NotFoundEntityException("La comida con id " + comidaId + " no existe"));
        securityUtils.checkOwnership(comida.getUsuario().getId());
    }

    private Integer calcularCalorias(Alimento alimento, BigDecimal cantidadGramos) {
        if (alimento.getCalorias() == null || cantidadGramos == null) {
            return 0;
        }
        return (int) Math.round((alimento.getCalorias() * cantidadGramos.doubleValue()) / 100.0);
    }

    private void recalcularTotalesComida(Comida comida) {
        List<AlimentoComida> items = alimentoComidaRepository.findByComidaId(comida.getId());
        int totalCal = 0;
        BigDecimal totalProt = BigDecimal.ZERO;
        BigDecimal totalCarb = BigDecimal.ZERO;
        BigDecimal totalGras = BigDecimal.ZERO;

        for (AlimentoComida item : items) {
            if (item.getCaloriasTotales() != null) totalCal += item.getCaloriasTotales();
            Alimento a = item.getAlimento();
            if (a != null && item.getCantidadGramos() != null) {
                BigDecimal cantidad = item.getCantidadGramos();
                BigDecimal cien = BigDecimal.valueOf(100);
                if (a.getProteinas() != null)
                    totalProt = totalProt.add(a.getProteinas().multiply(cantidad).divide(cien, 2, java.math.RoundingMode.HALF_UP));
                if (a.getCarbohidratos() != null)
                    totalCarb = totalCarb.add(a.getCarbohidratos().multiply(cantidad).divide(cien, 2, java.math.RoundingMode.HALF_UP));
                if (a.getGrasas() != null)
                    totalGras = totalGras.add(a.getGrasas().multiply(cantidad).divide(cien, 2, java.math.RoundingMode.HALF_UP));
            }
        }

        comida.setTotalCalorias(totalCal);
        comida.setTotalProteinas(totalProt);
        comida.setTotalCarbohidratos(totalCarb);
        comida.setTotalGrasas(totalGras);
        comidaRepository.save(comida);
    }
}
