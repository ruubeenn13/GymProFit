package com.gymprofit.api.service.comida;

import com.gymprofit.api.dto.entity.comida.ComidaCreateDTO;
import com.gymprofit.api.dto.entity.comida.ComidaDTO;
import com.gymprofit.api.dto.entity.comida.ComidaPatchDTO;
import com.gymprofit.api.config.security.SecurityUtils;
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

// ============================================================
// ComidaService — servicio de comidas del diario nutricional
// Gestiona el CRUD de comidas (desayuno, comida, cena...) registradas por
// cada usuario, aplicando control de propiedad (ownership) y agregando los
// totales nutricionales calculados a partir de sus alimentos asociados.
// ============================================================
@Service
@AllArgsConstructor
public class ComidaService implements IComidaService {

    private final IComidaRepository comidaRepository;
    private final IUsuarioRepository usuarioRepository;
    private final ComidaMapper comidaMapper;
    private final SecurityUtils securityUtils;
    private final Logger logger = LoggerFactory.getLogger(ComidaService.class);

    // Devuelve todas las comidas (requiere rol ADMIN).
    @Override
    public List<ComidaDTO> findAll() {
        logger.info("Buscando todas las comidas");

        securityUtils.requireAdmin();

        List<Comida> comidas = (List<Comida>) comidaRepository.findAll();

        return comidaMapper.toDTOList(comidas);
    }

    // Busca una comida por id y verifica que el usuario autenticado es su
    // propietario (o ADMIN).
    @Override
    public ComidaDTO findById(Integer id) {
        logger.info("Buscando comida por id: {}", id);

        Comida comida = comidaRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("La comida con id " + id + " no existe"));

        securityUtils.checkOwnership(comida.getUsuario().getId());

        return comidaMapper.toDTO(comida);
    }

    // Crea una nueva comida. Si el solicitante no es ADMIN, fuerza el
    // usuarioId al usuario autenticado (evita crear comidas para terceros).
    @Override
    public ComidaDTO save(ComidaCreateDTO comidaCreateDTO) {
        logger.info("Creando nueva comida de tipo: {}", comidaCreateDTO.getTipoComida());

        if (!securityUtils.isAdmin()) {
            comidaCreateDTO.setUsuarioId(securityUtils.getCurrentUserId());
        }

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

    // Modifica una comida existente (verifica propiedad antes de actualizar).
    @Override
    public ComidaDTO modify(ComidaDTO comidaDTO) {
        logger.info("Modificando comida con id: {}", comidaDTO.getId());

        Comida comida = comidaRepository.findById(comidaDTO.getId())
                .orElseThrow(() -> new NotFoundEntityException("La comida con id " + comidaDTO.getId() + " no existe"));

        securityUtils.checkOwnership(comida.getUsuario().getId());

        try {
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

    // Elimina una comida (verifica propiedad antes de borrar).
    @Transactional
    @Override
    public void deleteById(Integer id) {
        logger.info("Eliminando comida con id: {}", id);

        Comida comida = comidaRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("La comida con id " + id + " no existe"));

        securityUtils.checkOwnership(comida.getUsuario().getId());

        try {
            comidaRepository.delete(comida);

            logger.info("Comida con id {} eliminada correctamente", id);
        } catch (Exception ex) {
            throw new DeleteEntityException(Comida.class.getSimpleName(), id, ex);
        }
    }

    // Busca todas las comidas de un usuario (verifica propiedad).
    @Override
    public List<ComidaDTO> findByUsuarioId(Integer usuarioId) {
        logger.info("Buscando comidas por usuario id: {}", usuarioId);

        securityUtils.checkOwnership(usuarioId);

        List<Comida> comidas = comidaRepository.findByUsuarioId(usuarioId);

        return comidaMapper.toDTOList(comidas);
    }

    // Busca comidas por tipo entre todos los usuarios (requiere ADMIN).
    @Override
    public List<ComidaDTO> findByTipoComida(String tipoComida) {
        logger.info("Buscando comidas por tipo: {}", tipoComida);

        securityUtils.requireAdmin();

        TipoComida tipo = TipoComida.valueOf(tipoComida.toUpperCase());
        List<Comida> comidas = comidaRepository.findByTipoComida(tipo);

        return comidaMapper.toDTOList(comidas);
    }

    // Busca comidas registradas en una fecha concreta entre todos los
    // usuarios (requiere ADMIN); construye el rango [00:00, 23:59:59].
    @Override
    public List<ComidaDTO> findByFecha(LocalDate fecha) {
        logger.info("Buscando comidas por fecha: {}", fecha);

        securityUtils.requireAdmin();

        LocalDateTime inicio = fecha.atStartOfDay();
        LocalDateTime fin = fecha.atTime(23, 59, 59);

        List<Comida> comidas = comidaRepository.findByFechaBetween(inicio, fin);
        return comidaMapper.toDTOList(comidas);
    }

    // Busca las comidas de un usuario en una fecha concreta (verifica propiedad).
    @Override
    public List<ComidaDTO> findByUsuarioIdAndFecha(Integer usuarioId, LocalDate fecha) {
        logger.info("Buscando comidas por usuario {} y fecha {}", usuarioId, fecha);

        securityUtils.checkOwnership(usuarioId);

        LocalDateTime inicio = fecha.atStartOfDay();
        LocalDateTime fin = fecha.atTime(23, 59, 59);

        List<Comida> comidas = comidaRepository.findByUsuarioIdAndFechaBetween(usuarioId, inicio, fin);
        return comidaMapper.toDTOList(comidas);
    }


    // Busca las comidas de un usuario filtradas por tipo (verifica propiedad).
    @Override
    public List<ComidaDTO> findByUsuarioIdAndTipoComida(Integer usuarioId, String tipoComida) {
        logger.info("Buscando comidas por usuario {} y tipo {}", usuarioId, tipoComida);

        securityUtils.checkOwnership(usuarioId);

        TipoComida tipo = TipoComida.valueOf(tipoComida.toUpperCase());

        List<Comida> comidas = comidaRepository.findByUsuarioIdAndTipoComida(usuarioId, tipo);

        return comidaMapper.toDTOList(comidas);
    }

    // Cuenta las comidas de un usuario (verifica propiedad).
    @Override
    public Long countByUsuarioId(Integer usuarioId) {
        logger.info("Contando comidas del usuario: {}", usuarioId);

        securityUtils.checkOwnership(usuarioId);

        return comidaRepository.countByUsuarioId(usuarioId);
    }

    // Cuenta las comidas por tipo entre todos los usuarios (requiere ADMIN).
    @Override
    public Long countByTipoComida(String tipoComida) {
        logger.info("Contando comidas por tipo: {}", tipoComida);

        securityUtils.requireAdmin();

        TipoComida tipo = TipoComida.valueOf(tipoComida.toUpperCase());

        return comidaRepository.countByTipoComida(tipo);
    }

    // Cuenta las comidas de un usuario filtradas por tipo (verifica propiedad).
    @Override
    public Long countByUsuarioIdAndTipoComida(Integer usuarioId, String tipoComida) {
        logger.info("Contando commidas del usuario {} por tipo {}", usuarioId, tipoComida);

        securityUtils.checkOwnership(usuarioId);

        TipoComida tipo = TipoComida.valueOf(tipoComida.toUpperCase());

        return comidaRepository.countByUsuarioIdAndTipoComida(usuarioId, tipo);
    }

    // Aplica una actualización parcial sobre una comida (solo los campos no nulos).
    @Transactional
    @Override
    public ComidaDTO patch(Integer id, ComidaPatchDTO patchDTO) {
        logger.info("Aplicando patch a comida con id: {}", id);

        Comida comida = comidaRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("La comida con id " + id + " no existe"));

        securityUtils.checkOwnership(comida.getUsuario().getId());

        try {
            if (patchDTO.getFecha() != null) comida.setFecha(patchDTO.getFecha());
            if (patchDTO.getTipoComida() != null)
                comida.setTipoComida(TipoComida.valueOf(patchDTO.getTipoComida().toUpperCase()));
            if (patchDTO.getTotalCalorias() != null) comida.setTotalCalorias(patchDTO.getTotalCalorias());
            if (patchDTO.getTotalProteinas() != null) comida.setTotalProteinas(patchDTO.getTotalProteinas());
            if (patchDTO.getTotalCarbohidratos() != null) comida.setTotalCarbohidratos(patchDTO.getTotalCarbohidratos());
            if (patchDTO.getTotalGrasas() != null) comida.setTotalGrasas(patchDTO.getTotalGrasas());
            if (patchDTO.getNotas() != null) comida.setNotas(patchDTO.getNotas());

            return comidaMapper.toDTO(comidaRepository.save(comida));
        } catch (Exception e) {
            throw new UpdateEntityException(Comida.class.getSimpleName(), id, e);
        }
    }
}