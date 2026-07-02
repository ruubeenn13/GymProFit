package com.gymprofit.api.service.rutina;

import com.gymprofit.api.dto.admin.AdminRutinaDTO;
import com.gymprofit.api.dto.entity.rutina.RutinaCreateDTO;
import com.gymprofit.api.dto.entity.rutina.RutinaDTO;
import com.gymprofit.api.entity.Rutina;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.enums.Nivel;
import com.gymprofit.api.enums.RoleType;
import com.gymprofit.api.exceptions.*;
import com.gymprofit.api.mappers.RutinaMapper;
import com.gymprofit.api.repository.jooq.rutina.IAdminRutinaJooqRepository;
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

// ============================================================
// RutinaService — implementación del servicio de rutinas de entrenamiento
// Gestiona el CRUD de rutinas, distinguiendo entre predefinidas (visibles
// para todos, solo editables por ADMIN) y propias de cada usuario, con
// comprobaciones de visibilidad y propiedad basadas en el usuario autenticado.
// ============================================================
@Service
@AllArgsConstructor
@Transactional(readOnly = true)
public class RutinaService implements IRutinaService {

    private final IRutinaRepository rutinaRepository;
    private final IUsuarioRepository usuarioRepository;
    private final IAdminRutinaJooqRepository adminRutinaJooqRepository;
    private final RutinaMapper rutinaMapper;
    private final Logger logger = LoggerFactory.getLogger(RutinaService.class);

    // Obtiene el usuario autenticado desde el contexto de seguridad de Spring.
    private Usuario getCurrentUser() {
        return (Usuario) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    // Comprueba si el usuario dado tiene el rol ADMIN.
    private boolean isAdmin(Usuario usuario) {
        return usuario.getRoles().stream()
                .anyMatch(role -> role.getNombre() == RoleType.ADMIN);
    }

    // Devuelve todas las rutinas del sistema. Solo ADMIN.
    @Override
    public List<RutinaDTO> findAll() {
        logger.info("Buscando todas las rutinas");

        if (!isAdmin(getCurrentUser())) {
            throw new UnauthorizedException("Solo ADMIN puede listar todas las rutinas");
        }

        return rutinaMapper.toDTOList(rutinaRepository.findAll());
    }

    // Busca una rutina por id, verificando que el usuario puede visualizarla.
    @Override
    public RutinaDTO findById(Integer id) {
        logger.info("Buscando rutina por id: {}", id);

        Rutina rutina = rutinaRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("La rutina con id " + id + " no existe"));

        if (!canView(rutina)) {
            throw new UnauthorizedException("No tienes acceso a esta rutina");
        }

        return rutinaMapper.toDTO(rutina);
    }

    // Crea una rutina nueva; valida permisos según sea predefinida o propia del usuario.
    @Override
    @Transactional
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

    // Baja lógica de la rutina (activa=false) tras comprobar la propiedad.
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

    // Reactiva una rutina previamente desactivada, comprobando la propiedad.
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

    // Elimina definitivamente la rutina de la base de datos, comprobando la propiedad.
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

    // Actualiza los datos completos de una rutina existente, comprobando la propiedad.
    @Override
    @Transactional
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

    // Lista las rutinas propias de un usuario. Solo el propio usuario o ADMIN pueden consultarlas.
    @Override
    public List<RutinaDTO> findByUsuarioId(Integer usuarioId) {
        logger.info("Buscando rutinas por el usuario con id: {}", usuarioId);

        Usuario currentUser = getCurrentUser();
        if (!isAdmin(currentUser) && !currentUser.getId().equals(usuarioId)) {
            throw new UnauthorizedException("No tienes acceso a las rutinas de otro usuario");
        }

        return rutinaMapper.toDTOList(rutinaRepository.findByUsuarioId(usuarioId));
    }

    // Lista rutinas por nivel, filtrando solo las visibles para el usuario autenticado.
    @Override
    public List<RutinaDTO> findByNivel(String nivel) {
        logger.info("Buscando rutinas por nivel: {}", nivel);
        return rutinaMapper.toDTOList(filterViewable(rutinaRepository.findByNivel(Nivel.valueOf(nivel.toUpperCase()))));
    }

    // Busca rutinas por nombre (contiene, sin distinguir mayúsculas), filtrando las visibles.
    @Override
    public List<RutinaDTO> findByNombre(String nombre) {
        logger.info("Buscando rutinas por nombre: {}", nombre);
        return rutinaMapper.toDTOList(filterViewable(rutinaRepository.findByNombreContainingIgnoreCase(nombre)));
    }

    // Lista las rutinas activas visibles para el usuario autenticado.
    @Override
    public List<RutinaDTO> findActivas() {
        logger.info("Buscando rutinas activas");
        return rutinaMapper.toDTOList(filterViewable(rutinaRepository.findByActivaTrue()));
    }

    // Lista todas las rutinas predefinidas (compartidas por todos los usuarios).
    @Override
    public List<RutinaDTO> findPredefinidas() {
        logger.info("Buscando rutinas predefinidas");
        return rutinaMapper.toDTOList(rutinaRepository.findByEsPredefinidaTrue());
    }

    // Lista las rutinas activas propias de un usuario. Solo el propio usuario o ADMIN.
    @Override
    public List<RutinaDTO> findByUsuarioIdAndActivas(Integer usuarioId) {
        logger.info("Buscando rutinas activas del usuario id: {}", usuarioId);

        Usuario currentUser = getCurrentUser();
        if (!isAdmin(currentUser) && !currentUser.getId().equals(usuarioId)) {
            throw new UnauthorizedException("No tienes acceso a las rutinas de otro usuario");
        }

        return rutinaMapper.toDTOList(rutinaRepository.findByUsuarioIdAndActivaTrue(usuarioId));
    }

    // Lista las rutinas predefinidas filtradas por nivel de dificultad.
    @Override
    public List<RutinaDTO> findPredefinidasByNivel(String nivel) {
        logger.info("Buscando rutinas predefinidas por nivel: {}", nivel);
        return rutinaMapper.toDTOList(rutinaRepository.getRutinasPredefinidas(Nivel.valueOf(nivel.toUpperCase())));
    }

    // Actualiza parcialmente una rutina (solo los campos no nulos del patchDTO), comprobando la propiedad.
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

    // Búsqueda de rutinas predefinidas para el panel admin (incluye inactivas) mediante jOOQ.
    @Override
    @Transactional(readOnly = true)
    public List<AdminRutinaDTO> busquedaRutinasPredefinidas(String nombre, String nivel, String categoria, Boolean activa) {
        logger.info("Búsqueda admin de rutinas predefinidas");

        return adminRutinaJooqRepository.busquedaRutinasPredefinidas(nombre, nivel, categoria, activa);
    }

    /**
     * Verifica que el usuario autenticado tenga permisos sobre la rutina.
     * ADMIN puede operar sobre cualquier rutina.
     * USER solo puede operar sobre sus propias rutinas (esPredefinida=false, usuario=suyo).
     */
    /**
     * Indica si el usuario autenticado puede visualizar la rutina.
     * Son visibles las rutinas predefinidas (compartidas), las propias y todas para ADMIN.
     */
    private boolean canView(Rutina rutina) {
        Usuario currentUser = getCurrentUser();
        if (isAdmin(currentUser)) return true;
        if (Boolean.TRUE.equals(rutina.getEsPredefinida())) return true;
        return rutina.getUsuario() != null && currentUser.getId().equals(rutina.getUsuario().getId());
    }

    /**
     * Filtra una lista de rutinas dejando solo las visibles para el usuario autenticado.
     */
    private List<Rutina> filterViewable(List<Rutina> rutinas) {
        return rutinas.stream().filter(this::canView).toList();
    }

    // Verifica que el usuario autenticado pueda modificar/eliminar la rutina: ADMIN siempre,
    // o el propio dueño si la rutina no es predefinida; lanza UnauthorizedException si no.
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
