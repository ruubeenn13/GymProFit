package com.gymprofit.api.service.rutina;

import com.gymprofit.api.dto.entity.rutina.RutinaCreateDTO;
import com.gymprofit.api.dto.entity.rutina.RutinaDTO;
import com.gymprofit.api.entity.Rutina;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.enums.Nivel;
import com.gymprofit.api.exceptions.CreateEntityException;
import com.gymprofit.api.exceptions.DeleteEntityException;
import com.gymprofit.api.exceptions.NotFoundEntityException;
import com.gymprofit.api.exceptions.UpdateEntityException;
import com.gymprofit.api.mappers.RutinaMapper;
import com.gymprofit.api.repository.jpa.IRutinaRepository;
import com.gymprofit.api.repository.jpa.IUsuarioRepository;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
public class RutinaService implements IRutinaService{

    private final IRutinaRepository rutinaRepository;
    private final IUsuarioRepository usuarioRepository;
    private final RutinaMapper rutinaMapper;
    private final Logger logger = LoggerFactory.getLogger(RutinaService.class);

    @Override
    public List<RutinaDTO> findAll() {
        logger.info("Buscando todas las rutinas");

        List<Rutina> rutinas = (List<Rutina>) rutinaRepository.findAll();

        return rutinaMapper.toDTOList(rutinas);
    }

    @Override
    public RutinaDTO findById(Integer id) {
        logger.info("Buscando rutina por id: {}", id);

        Rutina rutina = rutinaRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("La rutina con id " + id + " no existe"));

        return rutinaMapper.toDTO(rutina);
    }

    @Override
    public RutinaDTO save(RutinaCreateDTO rutinaCreateDTO) {
        logger.info("Creando nueva rutina: {}", rutinaCreateDTO.getNombre());

        try {
            Usuario usuario = usuarioRepository.findById(rutinaCreateDTO.getUsuarioId())
                    .orElseThrow(() -> new NotFoundEntityException("El usuario con id " + rutinaCreateDTO.getUsuarioId() + " no existe"));

            Rutina rutina = rutinaMapper.toEntity(rutinaCreateDTO);
            rutina.setUsuario(usuario);
            rutina.setFechaCreacion(LocalDateTime.now());
            rutina.setActiva(true);

            Rutina rutinaGuardada = rutinaRepository.save(rutina);

            return rutinaMapper.toDTO(rutinaGuardada);
        } catch (NotFoundEntityException e) {
            throw e;
        } catch (Exception e) {
            throw new CreateEntityException(Rutina.class.getSimpleName(), rutinaCreateDTO, e);
        }
    }

    @Transactional
    @Override
    public void deleteById(Integer id) {
        logger.info("Desactivando rutina con id: {}", id);

        Rutina rutina = rutinaRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("La rutina con id " + id + " no existe"));

        try {
            rutina.setActiva(false);
            rutinaRepository.save(rutina);

            logger.info("Rutina con id {} desactivada correctamente", id);
        } catch (Exception e) {
            throw new DeleteEntityException(Rutina.class.getSimpleName(), id, e);
        }
    }

    @Transactional
    @Override
    public void activateById(Integer id) {
        logger.info("Activando rutina con id: {}", id);

        Rutina rutina = rutinaRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("La rutina con id " + id + " no existe"));

        try {
            rutina.setActiva(true);
            rutinaRepository.save(rutina);

            logger.info("Rutina con id {} activada correctamente", id);
        } catch (Exception e) {
            throw new UpdateEntityException(Rutina.class.getSimpleName());
        }
    }

    @Transactional
    @Override
    public void permanentDeleteById(Integer id) {
        logger.info("Eliminando permanentemente rutina con id: {}", id);

        Rutina rutina = rutinaRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("La rutina con id " + id + " no existe"));

        try {
            rutinaRepository.delete(rutina);

            logger.info("Rutina con id {} eliminada permanentemente", id);
        } catch (Exception e) {
            throw new DeleteEntityException(Rutina.class.getSimpleName(), id, e);
        }
    }

    @Override
    public RutinaDTO modify(RutinaDTO rutinaDTO) {
        logger.info("Modificando rutina con id: {}", rutinaDTO.getId());

        Rutina rutina = rutinaRepository.findById(rutinaDTO.getId())
                .orElseThrow(() -> new NotFoundEntityException("La rutina con id " + rutinaDTO.getId() + " no existe"));

        try {
            rutina.setNombre(rutinaDTO.getNombre());
            rutina.setDescripcion(rutinaDTO.getDescripcion());
            rutina.setNivel(Nivel.valueOf(rutinaDTO.getNivel()));
            rutina.setDuracionMinutos(rutinaDTO.getDuracionMinutos());
            rutina.setDiasSemana(rutinaDTO.getDiasSemana());
            rutina.setActiva(rutinaDTO.getActiva());

            Rutina rutinaActualizada = rutinaRepository.save(rutina);

            return rutinaMapper.toDTO(rutinaActualizada);
        } catch (Exception e) {
            throw new UpdateEntityException(Rutina.class.getSimpleName(), rutinaDTO, e);
        }
    }

    @Override
    public List<RutinaDTO> findByUsuarioId(Integer usuarioId) {
        logger.info("Buscando rutinas por el usuario con id: {}", usuarioId);

        List<Rutina> rutinas = rutinaRepository.findByUsuarioId(usuarioId);

        return rutinaMapper.toDTOList(rutinas);
    }

    @Override
    public List<RutinaDTO> findByNivel(String nivel) {
        logger.info("Buscando rutinas por nivel: {}", nivel);

        Nivel nivelEnum = Nivel.valueOf(nivel.toUpperCase());

        List<Rutina> rutinas = rutinaRepository.findByNivel(nivelEnum);

        return rutinaMapper.toDTOList(rutinas);
    }

    @Override
    public List<RutinaDTO> findByNombre(String nombre) {
        logger.info("Buscando rutinas por nombre: {}", nombre);

        List<Rutina> rutinas = rutinaRepository.findByNombreContainingIgnoreCase(nombre);

        return rutinaMapper.toDTOList(rutinas);
    }

    @Override
    public List<RutinaDTO> findActivas() {
        logger.info("Buscando rutinas activas");

        List<Rutina> rutinas = rutinaRepository.findByActivaTrue();

        return rutinaMapper.toDTOList(rutinas);
    }

    @Override
    public List<RutinaDTO> findPredefinidas() {
        logger.info("Buscando rutinas predefinidas");

        List<Rutina> rutinas = rutinaRepository.findByEsPredefinidaTrue();

        return rutinaMapper.toDTOList(rutinas);
    }

    @Override
    public List<RutinaDTO> findByUsuarioIdAndActivas(Integer usuarioId) {
        logger.info("Buscando rutinas activas del usuario id: {}", usuarioId);

        List<Rutina> rutinas = rutinaRepository.findByUsuarioIdAndActivaTrue(usuarioId);

        return rutinaMapper.toDTOList(rutinas);
    }

    @Override
    public List<RutinaDTO> findPredefinidasByNivel(String nivel) {
        logger.info("Buscando rutinas predefinidas por nivel: {}", nivel);

        Nivel nivelEnum = Nivel.valueOf(nivel.toUpperCase());

        List<Rutina> rutinas = rutinaRepository.getRutinasPredefinidas(nivelEnum);

        return rutinaMapper.toDTOList(rutinas);
    }


}
