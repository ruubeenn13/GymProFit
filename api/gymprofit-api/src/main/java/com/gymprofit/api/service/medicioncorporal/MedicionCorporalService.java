package com.gymprofit.api.service.medicioncorporal;

import com.gymprofit.api.dto.entity.medicioncorporal.MedicionCorporalCreateDTO;
import com.gymprofit.api.dto.entity.medicioncorporal.MedicionCorporalDTO;
import com.gymprofit.api.dto.entity.medicioncorporal.MedicionCorporalPatchDTO;
import com.gymprofit.api.entity.MedicionCorporal;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.exceptions.CreateEntityException;
import com.gymprofit.api.exceptions.DeleteEntityException;
import com.gymprofit.api.exceptions.NotFoundEntityException;
import com.gymprofit.api.exceptions.UpdateEntityException;
import com.gymprofit.api.mappers.MedicionCorporalMapper;
import com.gymprofit.api.repository.jpa.IMedicionCorporalRepository;
import com.gymprofit.api.repository.jpa.IUsuarioRepository;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
public class MedicionCorporalService implements IMedicionCorporalService {

    private final IMedicionCorporalRepository medicionCorporalRepository;
    private final IUsuarioRepository usuarioRepository;
    private final MedicionCorporalMapper medicionCorporalMapper;
    private final Logger logger = LoggerFactory.getLogger(MedicionCorporalService.class);

    @Override
    public List<MedicionCorporalDTO> findAll() {
        logger.info("Buscando todas las mediciones corporales");

        List<MedicionCorporal> lista = (List<MedicionCorporal>) medicionCorporalRepository.findAll();

        return medicionCorporalMapper.toDTOList(lista);
    }

    @Override
    public MedicionCorporalDTO findById(Integer id) {
        logger.info("Buscando medicion corporal por id: {}", id);

        MedicionCorporal medicion = medicionCorporalRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("La medición corporal con id " + id + " no existe"));

        return medicionCorporalMapper.toDTO(medicion);
    }

    @Transactional
    @Override
    public MedicionCorporalDTO save(MedicionCorporalCreateDTO createDTO) {
        logger.info("Creando nueva medición corporal para usuario id: {}", createDTO.getUsuarioId());

        Usuario usuario = usuarioRepository.findById(createDTO.getUsuarioId())
                .orElseThrow(() -> new NotFoundEntityException("El usuario con id " + createDTO.getUsuarioId() + " no existe"));

        try {
            MedicionCorporal medicion = medicionCorporalMapper.toEntity(createDTO);
            medicion.setUsuario(usuario);
            medicion.setFecha(LocalDateTime.now());

            // Calcular IMC automáticamente si tiene peso y altura
            if (createDTO.getPeso() != null && createDTO.getAltura() != null
                    && createDTO.getAltura().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal imc = createDTO.getPeso()
                        .divide(createDTO.getAltura().multiply(createDTO.getAltura()), 2, RoundingMode.HALF_UP);
                medicion.setImc(imc);
            }

            medicionCorporalRepository.save(medicion);

            MedicionCorporal recargada = medicionCorporalRepository.findByUsuarioIdOrderByFechaDesc(usuario.getId())
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new NotFoundEntityException("Error al recuperar la medición corporal guardada"));

            return medicionCorporalMapper.toDTO(recargada);
        } catch (NotFoundEntityException e) {
            throw e;
        } catch (Exception e) {
            throw new CreateEntityException(MedicionCorporal.class.getSimpleName(), createDTO, e);
        }
    }

    @Override
    public MedicionCorporalDTO modify(MedicionCorporalDTO dto) {
        logger.info("Modificando medición corporal con id: {}", dto.getId());

        MedicionCorporal medicion = medicionCorporalRepository.findById(dto.getId())
                .orElseThrow(() -> new NotFoundEntityException("La medición corporal con id " + dto.getId() + " no existe"));

        try {
            medicion.setPeso(dto.getPeso());
            medicion.setAltura(dto.getAltura());
            medicion.setGrasaCorporal(dto.getGrasaCorporal());
            medicion.setMasaMuscular(dto.getMasaMuscular());
            medicion.setCintura(dto.getCintura());
            medicion.setPecho(dto.getPecho());
            medicion.setBrazos(dto.getBrazos());
            medicion.setPiernas(dto.getPiernas());
            medicion.setNotas(dto.getNotas());

            // Recalcular IMC si tiene peso y altura
            if (dto.getPeso() != null && dto.getAltura() != null && dto.getAltura().compareTo(BigDecimal.ZERO) > 0) {

                BigDecimal imc = dto.getPeso()
                        .divide(dto.getAltura()
                                .multiply(dto.getAltura()), 2, RoundingMode.HALF_UP);

                medicion.setImc(imc);
            }

            MedicionCorporal actualizada = medicionCorporalRepository.save(medicion);

            return medicionCorporalMapper.toDTO(actualizada);
        } catch (Exception e) {
            throw new UpdateEntityException(MedicionCorporal.class.getSimpleName(), dto, e);
        }
    }

    @Transactional
    @Override
    public void deleteById(Integer id) {
        logger.info("Eliminando medición corporal con id: {}", id);

        MedicionCorporal medicion = medicionCorporalRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("La medición corporal con id " + id + " no existe"));

        try {
            medicionCorporalRepository.delete(medicion);

            logger.info("Medición corporal con id {} eliminada correctamente", id);

        } catch (Exception e) {
            throw new DeleteEntityException(MedicionCorporal.class.getSimpleName(), id, e);
        }
    }

    @Override
    public List<MedicionCorporalDTO> findByUsuarioId(Integer usuarioId) {
        logger.info("Buscando mediciones corporales del usuario id: {}", usuarioId);

        List<MedicionCorporal> lista = medicionCorporalRepository.findByUsuarioId(usuarioId);

        return medicionCorporalMapper.toDTOList(lista);
    }

    @Override
    public List<MedicionCorporalDTO> findByUsuarioIdOrdenadas(Integer usuarioId) {
        logger.info("Buscando mediciones corporales del usuario id: {} ordenadas por fecha", usuarioId);

        List<MedicionCorporal> lista = medicionCorporalRepository.findByUsuarioIdOrderByFechaDesc(usuarioId);

        return medicionCorporalMapper.toDTOList(lista);
    }

    @Override
    public List<MedicionCorporalDTO> findByUsuarioIdAndFechaBetween(Integer usuarioId, LocalDateTime inicio, LocalDateTime fin) {
        logger.info("Buscando mediciones corporales del usuario id: {} entre {} y {}", usuarioId, inicio, fin);

        List<MedicionCorporal> lista = medicionCorporalRepository.findByUsuarioIdAndFechaBetween(usuarioId, inicio, fin);

        return medicionCorporalMapper.toDTOList(lista);
    }

    @Override
    public List<MedicionCorporalDTO> getUltimasMediciones(Integer usuarioId) {
        logger.info("Obteniendo últimas mediciones del usuario id: {}", usuarioId);

        List<MedicionCorporal> lista = medicionCorporalRepository.getUltimasMediciones(usuarioId);

        return medicionCorporalMapper.toDTOList(lista);
    }

    @Transactional
    @Override
    public MedicionCorporalDTO patch(Integer id, MedicionCorporalPatchDTO patchDTO) {
        logger.info("Aplicando patch a medición corporal con id: {}", id);

        MedicionCorporal medicion = medicionCorporalRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("La medición corporal con id " + id + " no existe"));

        try {
            if (patchDTO.getFecha() != null) medicion.setFecha(patchDTO.getFecha());
            if (patchDTO.getPeso() != null) medicion.setPeso(patchDTO.getPeso());
            if (patchDTO.getAltura() != null) medicion.setAltura(patchDTO.getAltura());
            if (patchDTO.getGrasaCorporal() != null) medicion.setGrasaCorporal(patchDTO.getGrasaCorporal());
            if (patchDTO.getMasaMuscular() != null) medicion.setMasaMuscular(patchDTO.getMasaMuscular());
            if (patchDTO.getCintura() != null) medicion.setCintura(patchDTO.getCintura());
            if (patchDTO.getPecho() != null) medicion.setPecho(patchDTO.getPecho());
            if (patchDTO.getBrazos() != null) medicion.setBrazos(patchDTO.getBrazos());
            if (patchDTO.getPiernas() != null) medicion.setPiernas(patchDTO.getPiernas());
            if (patchDTO.getNotas() != null) medicion.setNotas(patchDTO.getNotas());

            // Recalcular IMC si se actualizó peso o altura
            if (patchDTO.getPeso() != null || patchDTO.getAltura() != null) {
                BigDecimal p = medicion.getPeso();
                BigDecimal a = medicion.getAltura();
                if (p != null && a != null && a.compareTo(BigDecimal.ZERO) > 0) {
                    medicion.setImc(p.divide(a.multiply(a), 2, RoundingMode.HALF_UP));
                }
            } else if (patchDTO.getImc() != null) {
                medicion.setImc(patchDTO.getImc());
            }

            return medicionCorporalMapper.toDTO(medicionCorporalRepository.save(medicion));
        } catch (Exception e) {
            throw new UpdateEntityException(MedicionCorporal.class.getSimpleName(), id, e);
        }
    }
}