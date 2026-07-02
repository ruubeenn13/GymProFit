package com.gymprofit.api.service.usuario;

import com.gymprofit.api.config.security.SecurityUtils;
import com.gymprofit.api.dto.admin.AdminEstadisticasDTO;
import com.gymprofit.api.dto.admin.AdminUsuarioDTO;
import com.gymprofit.api.dto.entity.usuario.UsuarioCreateDTO;
import com.gymprofit.api.dto.entity.usuario.UsuarioDTO;
import com.gymprofit.api.dto.entity.usuario.UsuarioEstadisticasDTO;
import com.gymprofit.api.dto.entity.usuario.UsuarioPatchDTO;
import com.gymprofit.api.dto.entity.usuario.UsuarioUpdateDTO;
import com.gymprofit.api.entity.Role;
import com.gymprofit.api.enums.NivelExperiencia;
import com.gymprofit.api.enums.RoleType;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.exceptions.*;
import com.gymprofit.api.mappers.UsuarioMapper;
import com.gymprofit.api.repository.jpa.IRoleRepository;
import com.gymprofit.api.repository.jpa.IUsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

// ============================================================
// UsuarioService — implementa la gestión de usuarios de GymProFit.
// Cubre el CRUD de usuarios, la carga de credenciales para Spring Security
// (loadUserByUsername), la subida/lectura de foto de perfil en disco y las
// operaciones administrativas (estadísticas globales, activar/desactivar, cambiar rol).
// ============================================================
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UsuarioService implements IUsuarioService {

    private final IUsuarioRepository usuarioRepository;
    private final IRoleRepository roleRepository;
    private final UsuarioMapper usuarioMapper;
    private final com.gymprofit.api.repository.jooq.usuario.IUsuarioJooqRepository usuarioJooqRepository;
    private final SecurityUtils securityUtils;
    // Logger para trazar las operaciones del servicio.
    private final Logger logger = LoggerFactory.getLogger(UsuarioService.class);

    // Directorio donde se guardan las fotos de perfil (configurable por properties).
    @Value("${app.upload.dir:./uploads/fotos-perfil}")
    private String uploadDir;


    // Carga el usuario por username para el proceso de autenticación de Spring Security.
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));
    }

    // Lista todos los usuarios del sistema.
    @Override
    public List<UsuarioDTO> findAll() {
        logger.info("Buscando todos los usuarios");

        List<Usuario> usuarios = usuarioRepository.findAll();

        return usuarioMapper.toDTOList(usuarios);
    }

    // Busca un usuario por id, comprobando que el solicitante sea el propio usuario o ADMIN.
    @Override
    public UsuarioDTO findById(Integer id) {
        logger.info("Buscando usuario por id: {}", id);

        securityUtils.checkOwnership(id);

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("El usuario con id " + id + " no existe"));

        return usuarioMapper.toDTO(usuario);
    }

    // Crea un nuevo usuario, validando que username y email no estén ya en uso.
    @Override
    @Transactional
    public UsuarioDTO save(UsuarioCreateDTO usuarioCreateDTO) {
        logger.info("Intento de crear un usuario");

        if (existsByUsername(usuarioCreateDTO.getUsername())) {
            throw new DuplicateEntityException("username", usuarioCreateDTO.getUsername());
        }

        if (existsByEmail(usuarioCreateDTO.getEmail())) {
            throw new DuplicateEntityException("email", usuarioCreateDTO.getEmail());
        }

        try {
            Usuario newUsuario = usuarioMapper.toEntity(usuarioCreateDTO);
            newUsuario.setFechaRegistro(LocalDateTime.now());
            newUsuario.setActivo(true);

            newUsuario = usuarioRepository.save(newUsuario);

            return usuarioMapper.toDTO(newUsuario);
        } catch (Exception e) {
            throw new CreateEntityException(Usuario.class.getSimpleName(), usuarioCreateDTO, e);
        }
    }

    // Baja lógica: desactiva el usuario en lugar de borrarlo físicamente.
    @Transactional
    @Override
    public void deleteById(Integer id) {
        logger.info("Intento de eliminar un usuario con id: {}", id);

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("El usuario con id " + id + " no existe"));

        try {
            usuario.setActivo(false);
            usuarioRepository.save(usuario);

            logger.info("Usuario con id {} desactivado correctamente", id);
        } catch (Exception e) {
            throw new DeleteEntityException(Usuario.class.getSimpleName(), id, e);
        }
    }

    // Reactiva un usuario previamente desactivado.
    @Transactional
    @Override
    public void activateById(Integer id) {
        logger.info("Activando usuario con id: {}", id);

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("El usuario con id " + id + " no existe"));

        try {
            usuario.setActivo(true);
            usuarioRepository.save(usuario);

            logger.info("Usuario con id {} activado correctamente", id);
        } catch (Exception ex) {
            throw new UpdateEntityException(Usuario.class.getSimpleName(), id, ex);
        }
    }

    // Elimina permanentemente el usuario y sus datos asociados de la base de datos.
    @Transactional
    @Override
    public void permanentDeleteById(Integer id) {
        logger.info("Eliminando permanentemente usuario con id: {}", id);

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("El usuario con id " + id + " no existe"));

        try {
            usuarioRepository.delete(usuario);

            logger.info("Usuario con id {} eliminado permanentemente", id);
        } catch (Exception ex) {
            throw new DeleteEntityException(Usuario.class.getSimpleName(), id, ex);
        }
    }

    // Sustituye los datos de un usuario existente usando el mapper (updateEntityFromDTO).
    @Override
    @Transactional
    public UsuarioDTO modify(UsuarioUpdateDTO usuarioUpdateDTO) {
        logger.info("Intento de modificar usuario con id: {}", usuarioUpdateDTO.getId());

        Usuario usuario = usuarioRepository.findById(usuarioUpdateDTO.getId())
                .orElseThrow(() -> new NotFoundEntityException("El usuario con id " + usuarioUpdateDTO.getId() + " no existe"));

        try {
            usuarioMapper.updateEntityFromDTO(usuarioUpdateDTO, usuario);

            usuarioRepository.save(usuario);

            return usuarioMapper.toDTO(usuario);
        } catch (Exception e) {
            throw new UpdateEntityException(Usuario.class.getSimpleName(), usuarioUpdateDTO, e);
        }
    }

    // Busca un usuario por username, comprobando propiedad sobre el resultado.
    @Override
    public UsuarioDTO findByUsername(String username) {
        logger.info("Buscando usuario por username: {}", username);

        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundEntityException("El usuario con username '" + username + "' no existe"));

        securityUtils.checkOwnership(usuario.getId());

        return usuarioMapper.toDTO(usuario);
    }

    // Busca un usuario por email (usado internamente, p.ej. en el registro/login).
    @Override
    public UsuarioDTO findByEmail(String email) {
        logger.info("Buscando un usuario por email: {}", email);

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundEntityException("El usuario con email '" + email + "' no existe"));

        return usuarioMapper.toDTO(usuario);
    }

    @Override
    public Boolean existsByUsername(String username) {
        return usuarioRepository.existsByUsername(username);
    }

    @Override
    public Boolean existsByEmail(String email) {
        return usuarioRepository.existsByEmail(email);
    }

    // Lista los usuarios con estado activo.
    @Override
    public List<UsuarioDTO> findActivos() {
        logger.info("Buscando usuarios activos");

        List<Usuario> usuarios = usuarioRepository.findByActivoTrue();

        return usuarioMapper.toDTOList(usuarios);
    }

    // Actualiza parcialmente un usuario (perfil, datos físicos, nivel de experiencia...)
    // con los campos no nulos del patch.
    @Transactional
    @Override
    public UsuarioDTO patch(Integer id, UsuarioPatchDTO patchDTO) {
        logger.info("Aplicando patch a usuario con id: {}", id);

        securityUtils.checkOwnership(id);

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("El usuario con id " + id + " no existe"));

        // Se valida y convierte el nivel de experiencia antes de aplicar cambios.
        NivelExperiencia nivel = null;
        if (patchDTO.getNivelExperiencia() != null && !patchDTO.getNivelExperiencia().isBlank()) {
            try {
                nivel = NivelExperiencia.valueOf(patchDTO.getNivelExperiencia().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new InvalidDataException("Nivel de experiencia inválido: " + patchDTO.getNivelExperiencia());
            }
        }

        try {
            if (patchDTO.getEmail() != null) usuario.setEmail(patchDTO.getEmail());
            if (patchDTO.getPeso() != null) usuario.setPeso(patchDTO.getPeso());
            if (patchDTO.getAltura() != null) usuario.setAltura(patchDTO.getAltura());
            if (patchDTO.getEdad() != null) usuario.setEdad(patchDTO.getEdad());
            if (nivel != null) usuario.setNivelExperiencia(nivel);
            if (patchDTO.getObjetivo() != null) usuario.setObjetivo(patchDTO.getObjetivo());
            if (patchDTO.getActivo() != null) usuario.setActivo(patchDTO.getActivo());

            return usuarioMapper.toDTO(usuarioRepository.save(usuario));
        } catch (Exception e) {
            throw new UpdateEntityException(Usuario.class.getSimpleName(), id, e);
        }
    }

    // Obtiene las estadísticas personales de un usuario mediante una consulta jOOQ optimizada.
    @Override
    public UsuarioEstadisticasDTO getEstadisticas(Integer usuarioId) {
        securityUtils.checkOwnership(usuarioId);

        if (!usuarioRepository.existsById(usuarioId)) {
            throw new NotFoundEntityException("Usuario con id " + usuarioId + " no encontrado");
        }
        return usuarioJooqRepository.getEstadisticas(usuarioId);
    }

    // Lista usuarios paginados con filtros (activo, rol, username) para el panel de administración.
    @Override
    public List<AdminUsuarioDTO> getUsuariosAdmin(Boolean activo, String rol, String username, int page, int size) {
        logger.info("Admin: listando usuarios con filtros activo={}, rol={}, username={}, page={}, size={}", activo, rol, username, page, size);
        return usuarioJooqRepository.getUsuariosAdmin(activo, rol, username, page, size);
    }

    // Obtiene estadísticas globales de la aplicación (uso administrativo).
    @Override
    public AdminEstadisticasDTO getEstadisticasGlobales() {
        logger.info("Admin: obteniendo estadísticas globales");
        return usuarioJooqRepository.getEstadisticasGlobales();
    }

    // Alterna el estado activo/inactivo de un usuario (uso administrativo).
    @Transactional
    @Override
    public void toggleActivo(Integer id) {
        logger.info("Admin: toggle activo usuario id={}", id);

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("El usuario con id " + id + " no existe"));

        try {
            usuario.setActivo(!Boolean.TRUE.equals(usuario.getActivo()));
            usuarioRepository.save(usuario);
            logger.info("Admin: usuario id={} activo={}", id, usuario.getActivo());
        } catch (Exception e) {
            throw new UpdateEntityException(Usuario.class.getSimpleName(), id, e);
        }
    }

    // Guarda la foto de perfil recibida en disco (nombre = id.jpg) y actualiza la referencia en BD.
    @Override
    @Transactional
    public UsuarioDTO uploadFotoPerfil(Integer id, MultipartFile file) {
        logger.info("Subiendo foto de perfil para usuario id={}", id);

        securityUtils.checkOwnership(id);

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("Usuario con id " + id + " no encontrado"));

        try {
            Path dir = Paths.get(uploadDir);
            // Se crea el directorio de subida si todavía no existe.
            if (!Files.exists(dir)) Files.createDirectories(dir);

            String filename = id + ".jpg";
            Files.write(dir.resolve(filename), file.getBytes());
            usuario.setFotoPerfil(filename);
            usuarioRepository.save(usuario);
        } catch (IOException e) {
            throw new RuntimeException("Error al guardar la foto de perfil: " + e.getMessage());
        }

        return usuarioMapper.toDTO(usuario);
    }

    // Lee del disco los bytes de la foto de perfil de un usuario.
    @Override
    public byte[] getFotoPerfil(Integer id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("Usuario con id " + id + " no encontrado"));

        if (usuario.getFotoPerfil() == null) {
            throw new NotFoundEntityException("El usuario " + id + " no tiene foto de perfil");
        }

        try {
            return Files.readAllBytes(Paths.get(uploadDir).resolve(usuario.getFotoPerfil()));
        } catch (IOException e) {
            throw new NotFoundEntityException("Foto de perfil no encontrada para usuario " + id);
        }
    }

    // Cambia el rol de un usuario (uso administrativo), validando que el rol exista.
    @Transactional
    @Override
    public void cambiarRol(Integer id, String nuevoRol) {
        logger.info("Admin: cambiando rol usuario id={} a {}", id, nuevoRol);

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("El usuario con id " + id + " no existe"));

        RoleType roleType;
        try {
            roleType = RoleType.valueOf(nuevoRol.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidDataException("Rol inválido: " + nuevoRol + ". Valores válidos: ADMIN, USER, GUEST");
        }

        Role role = roleRepository.findByNombre(roleType)
                .orElseThrow(() -> new NotFoundEntityException("Rol " + nuevoRol + " no encontrado en BD"));

        try {
            usuario.setRoles(List.of(role));
            usuarioRepository.save(usuario);
            logger.info("Admin: usuario id={} rol cambiado a {}", id, roleType);
        } catch (Exception e) {
            throw new UpdateEntityException(Usuario.class.getSimpleName(), id, e);
        }
    }
}