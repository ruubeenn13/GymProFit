package com.gymprofit.api.service.comida;

import com.gymprofit.api.dto.entity.comida.ComidaCreateDTO;
import com.gymprofit.api.dto.entity.comida.ComidaDTO;
import com.gymprofit.api.entity.Comida;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.enums.TipoComida;
import com.gymprofit.api.exceptions.CreateEntityException;
import com.gymprofit.api.exceptions.DeleteEntityException;
import com.gymprofit.api.exceptions.NotFoundEntityException;
import com.gymprofit.api.exceptions.UpdateEntityException;
import com.gymprofit.api.mappers.ComidaMapper;
import com.gymprofit.api.repository.jpa.IComidaRepository;
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
public class ComidaService implements IComidaService {

    private final IComidaRepository comidaRepository;
    private final IUsuarioRepository usuarioRepository;
    private final ComidaMapper comidaMapper;
    private final Logger logger = LoggerFactory.getLogger(ComidaService.class);

    @Override
    public List<ComidaDTO> findAll() {
        logger.info("Buscando todas las comidas");

        List<Comida> comidas = (List<Comida>) comidaRepository.findAll();

        return comidaMapper.toDTOList(comidas);
    }

    @Override
    public ComidaDTO findById(Integer id) {
        logger.info("Buscando comida por id: {}", id);

        Comida comida = comidaRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("La comida con id " + id + " no existe"));

        return comidaMapper.toDTO(comida);
    }

    @Override
    public ComidaDTO save(ComidaCreateDTO comidaCreateDTO) {
        logger.info("Creando nueva comida de tipo: {}", comidaCreateDTO.getTipoComida());

        try {
            Usuario usuario = usuarioRepository.findById(comidaCreateDTO.getUsuarioId())
                    .orElseThrow(() -> new NotFoundEntityException("El usuario con id " + comidaCreateDTO.getUsuarioId() + " no existe"));

            Comida comida = comidaMapper.toEntity(comidaCreateDTO);
            comida.setUsuario(usuario);

            comida.setTipoComida(TipoComida.valueOf(comidaCreateDTO.getTipoComida().toUpperCase()));

            if (comida.getFecha() == null) {
                comida.setFecha(LocalDateTime.now());
            }

            Comida comidaGuardada = comidaRepository.save(comida);

            return comidaMapper.toDTO(comidaGuardada);
        } catch (NotFoundEntityException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new CreateEntityException(Comida.class.getSimpleName(), comidaCreateDTO, ex);
        }
    }

    @Override
    public ComidaDTO modify(ComidaDTO comidaDTO) {
        logger.info("Modificando comida con id: {}", comidaDTO.getId());

        Comida comida = comidaRepository.findById(comidaDTO.getId())
                .orElseThrow(() -> new NotFoundEntityException("La comida con id " + comidaDTO.getId() + " no existe"));

        try {
            Usuario usuario = usuarioRepository.findById(comidaDTO.getUsuarioId())
                    .orElseThrow(() -> new NotFoundEntityException("El usuario con id " + comidaDTO.getUsuarioId() + " no existe"));

            comida.setUsuario(usuario);
            comida.setFecha(comidaDTO.getFecha());
            comida.setTipoComida(TipoComida.valueOf(comidaDTO.getTipoComida().toUpperCase()));
            comida.setTotalCalorias(comidaDTO.getTotalCalorias());
            comida.setTotalProteinas(comidaDTO.getTotalProteinas());
            comida.setTotalCarbohidratos(comidaDTO.getTotalCarbohidratos());
            comida.setTotalGrasas(comidaDTO.getTotalGrasas());
            comida.setNotas(comidaDTO.getNotas());

            Comida comidaActualizada = comidaRepository.save(comida);

            return comidaMapper.toDTO(comidaActualizada);
        } catch (NotFoundEntityException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new UpdateEntityException(Comida.class.getSimpleName(), comidaDTO, ex);
        }
    }

    @Transactional
    @Override
    public void deleteById(Integer id) {
        logger.info("Eliminando comida con id: {}", id);

        Comida comida = comidaRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("La comida con id " + id + " no existe"));

        try {
            comidaRepository.delete(comida);

            logger.info("Comida con id {} eliminada correctamente", id);
        } catch (Exception ex) {
            throw new DeleteEntityException(Comida.class.getSimpleName(), id, ex);
        }
    }

    @Override
    public List<ComidaDTO> findByUsuarioId(Integer usuarioId) {
        logger.info("Buscando comidas por usuario id: {}", usuarioId);

        List<Comida> comidas = comidaRepository.findByUsuarioId(usuarioId);

        return comidaMapper.toDTOList(comidas);
    }

    @Override
    public List<ComidaDTO> findByTipoComida(String tipoComida) {
        logger.info("Buscando comidas por tipo: {}", tipoComida);

        TipoComida tipo = TipoComida.valueOf(tipoComida.toUpperCase());
        List<Comida> comidas = comidaRepository.findByTipoComida(tipo);

        return comidaMapper.toDTOList(comidas);
    }

    @Override
    public List<ComidaDTO> findByFecha(LocalDate fecha) {
        logger.info("Buscando comidas por fecha: {}", fecha);

        LocalDateTime inicio = fecha.atStartOfDay();
        LocalDateTime fin = fecha.atTime(23, 59, 59);

        List<Comida> comidas = comidaRepository.findByFechaBetween(inicio, fin);
        return comidaMapper.toDTOList(comidas);
    }

    @Override
    public List<ComidaDTO> findByUsuarioIdAndFecha(Integer usuarioId, LocalDate fecha) {
        logger.info("Buscando comidas por usuario {} y fecha {}", usuarioId, fecha);

        LocalDateTime inicio = fecha.atStartOfDay();
        LocalDateTime fin = fecha.atTime(23, 59, 59);

        List<Comida> comidas = comidaRepository.findByUsuarioIdAndFechaBetween(usuarioId, inicio, fin);
        return comidaMapper.toDTOList(comidas);
    }


    @Override
    public List<ComidaDTO> findByUsuarioIdAndTipoComida(Integer usuarioId, String tipoComida) {
        logger.info("Buscando comidas por usuario {} y tipo {}", usuarioId, tipoComida);

        TipoComida tipo = TipoComida.valueOf(tipoComida.toUpperCase());

        List<Comida> comidas = comidaRepository.findByUsuarioIdAndTipoComida(usuarioId, tipo);

        return comidaMapper.toDTOList(comidas);
    }

    @Override
    public Long countByUsuarioId(Integer usuarioId) {
        logger.info("Contando comidas del usuario: {}", usuarioId);

        return comidaRepository.countByUsuarioId(usuarioId);
    }

    @Override
    public Long countByTipoComida(String tipoComida) {
        logger.info("Contando comidas por tipo: {}", tipoComida);

        TipoComida tipo = TipoComida.valueOf(tipoComida.toUpperCase());

        return comidaRepository.countByTipoComida(tipo);
    }

    @Override
    public Long countByUsuarioIdAndTipoComida(Integer usuarioId, String tipoComida) {
        logger.info("Contando commidas del usuario {} por tipo {}", usuarioId, tipoComida);

        TipoComida tipo = TipoComida.valueOf(tipoComida.toUpperCase());

        return comidaRepository.countByUsuarioIdAndTipoComida(usuarioId, tipo);
    }
}