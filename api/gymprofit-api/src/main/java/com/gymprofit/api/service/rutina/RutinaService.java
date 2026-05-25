package com.gymprofit.api.service.rutina;

import com.gymprofit.api.dto.entity.rutina.RutinaCreateDTO;
import com.gymprofit.api.dto.entity.rutina.RutinaDTO;
import com.gymprofit.api.entity.Rutina;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.enums.Nivel;
import com.gymprofit.api.enums.RoleType;
import com.gymprofit.api.exceptions.*;
import com.gymprofit.api.mappers.RutinaMapper;
import com.gymprofit.api.repository.jpa.IRutinaRepository;
import com.gymprofit.api.repository.jpa.IUsuarioRepository;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
public class RutinaService implements IRutinaService {

    private final IRutinaRepository rutinaRepository;
    private final IUsuarioRepository usuarioRepository;
    private final RutinaMapper rutinaMapper;
    private final Logger logger = LoggerFactory.getLogger(RutinaService.class);

    private Usuario getCurrentUser() {
        return (Usuario) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private boolean isAdmin(Usuario usuario) {
        return usuario.getRoles().stream()
                .anyMatch(role -> role.getNombre() == RoleType.ADMIN);
    }

    @Override
    public List<RutinaDTO> findAll() {
        logger.info("Buscando todas las rutinas");
        return rutinaMapper.toDTOList((List<Rutina>) rutinaRepository.findAll());
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

        Usuario currentUser = getCurrentUser();
        boolean admin = isAdmin(currentUser);
        boolean predefinida = Boolean.TRUE.equals(rutinaCreateDTO.getEsPredefinida());

        if (predefinida && !admin) {
            throw new UnauthorizedException("Solo ADMIN puede crear rutinas predefinidas");
        }
        if (!predefinida && !admin && !currentUser.getId().equals(rutinaCreateDTO.getUsuarioId())) {
            throw new UnauthorizedException("No puedes crear rutinas para otro usuario");
        }

        try {
            Rutina rutina = rutinaMapper.toEntity(rutinaCreateDTO);
            rutina.setFechaCreacion(LocalDateTime.now());
            rutina.setActiva(true);

            if (!predefinida) {
                Integer targetId = admin && rutinaCreateDTO.getUsuarioId() != null
                        ? rutinaCreateDTO.getUsuarioId()
                        : currentUser.getId();
                Usuario propietario = usuarioRepository.findById(targetId)
                        .orElseThrow(() -> new NotFoundEntityException("El usuario con id " + targetId + " no existe"));
                rutina.setUsuario(propietario);
            }

            return rutinaMapper.toDTO(rutinaRepository.save(rutina));
        } catch (NotFoundEntityException | UnauthorizedException e) {
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

        checkOwnership(rutina);

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

        checkOwnership(rutina);

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

        checkOwnership(rutina);

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

        checkOwnership(rutina);

        try {
            rutina.setNombre(rutinaDTO.getNombre());
            rutina.setDescripcion(rutinaDTO.getDescripcion());
            rutina.setNivel(Nivel.valueOf(rutinaDTO.getNivel()));
            rutina.setDuracionMinutos(rutinaDTO.getDuracionMinutos());
            rutina.setDiasSemana(rutinaDTO.getDiasSemana());
            rutina.setActiva(rutinaDTO.getActiva());

            return rutinaMapper.toDTO(rutinaRepository.save(rutina));
        } catch (Exception e) {
            throw new UpdateEntityException(Rutina.class.getSimpleName(), rutinaDTO, e);
        }
    }

    @Override
    public List<RutinaDTO> findByUsuarioId(Integer usuarioId) {
        logger.info("Buscando rutinas por el usuario con id: {}", usuarioId);
        return rutinaMapper.toDTOList(rutinaRepository.findByUsuarioId(usuarioId));
    }

    @Override
    public List<RutinaDTO> findByNivel(String nivel) {
        logger.info("Buscando rutinas por nivel: {}", nivel);
        return rutinaMapper.toDTOList(rutinaRepository.findByNivel(Nivel.valueOf(nivel.toUpperCase())));
    }

    @Override
    public List<RutinaDTO> findByNombre(String nombre) {
        logger.info("Buscando rutinas por nombre: {}", nombre);
        return rutinaMapper.toDTOList(rutinaRepository.findByNombreContainingIgnoreCase(nombre));
    }

    @Override
    public List<RutinaDTO> findActivas() {
        logger.info("Buscando rutinas activas");
        return rutinaMapper.toDTOList(rutinaRepository.findByActivaTrue());
    }

    @Override
    public List<RutinaDTO> findPredefinidas() {
        logger.info("Buscando rutinas predefinidas");
        return rutinaMapper.toDTOList(rutinaRepository.findByEsPredefinidaTrue());
    }

    @Override
    public List<RutinaDTO> findByUsuarioIdAndActivas(Integer usuarioId) {
        logger.info("Buscando rutinas activas del usuario id: {}", usuarioId);
        return rutinaMapper.toDTOList(rutinaRepository.findByUsuarioIdAndActivaTrue(usuarioId));
    }

    @Override
    public List<RutinaDTO> findPredefinidasByNivel(String nivel) {
        logger.info("Buscando rutinas predefinidas por nivel: {}", nivel);
        return rutinaMapper.toDTOList(rutinaRepository.getRutinasPredefinidas(Nivel.valueOf(nivel.toUpperCase())));
    }

    @Transactional
    @Override
    public RutinaDTO patch(Integer id, com.gymprofit.api.dto.entity.rutina.RutinaPatchDTO patchDTO) {
        logger.info("Aplicando patch a rutina con id: {}", id);

        Rutina rutina = rutinaRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("La rutina con id " + id + " no existe"));

        checkOwnership(rutina);

        try {
            if (patchDTO.getNombre() != null) rutina.setNombre(patchDTO.getNombre());
            if (patchDTO.getDescripcion() != null) rutina.setDescripcion(patchDTO.getDescripcion());
            if (patchDTO.getDuracionMinutos() != null) rutina.setDuracionMinutos(patchDTO.getDuracionMinutos());
            if (patchDTO.getCaloriasAproximadas() != null) rutina.setCaloriasAproximadas(patchDTO.getCaloriasAproximadas());
            if (patchDTO.getNivel() != null) rutina.setNivel(Nivel.valueOf(patchDTO.getNivel().toUpperCase()));
            if (patchDTO.getCategoria() != null) rutina.setCategoria(patchDTO.getCategoria());
            if (patchDTO.getDiasSemana() != null) rutina.setDiasSemana(patchDTO.getDiasSemana());
            if (patchDTO.getActiva() != null) rutina.setActiva(patchDTO.getActiva());

            return rutinaMapper.toDTO(rutinaRepository.save(rutina));
        } catch (UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            throw new UpdateEntityException(Rutina.class.getSimpleName(), id, e);
        }
    }

    /**
     * Verifica que el usuario autenticado tenga permisos sobre la rutina.
     * ADMIN puede operar sobre cualquier rutina.
     * USER solo puede operar sobre sus propias rutinas (esPredefinida=false, usuario=suyo).
     */
    private void checkOwnership(Rutina rutina) {
        Usuario currentUser = getCurrentUser();
        if (isAdmin(currentUser)) return;

        if (Boolean.TRUE.equals(rutina.getEsPredefinida())) {
            throw new UnauthorizedException("Solo ADMIN puede modificar rutinas predefinidas");
        }
        if (rutina.getUsuario() == null || !currentUser.getId().equals(rutina.getUsuario().getId())) {
            throw new UnauthorizedException();
        }
    }
}
