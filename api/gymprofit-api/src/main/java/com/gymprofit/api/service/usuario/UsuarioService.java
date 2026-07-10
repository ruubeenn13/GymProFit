package com.gymprofit.api.service.usuario;

import com.gymprofit.api.config.security.SecurityUtils;
import com.gymprofit.api.dto.admin.AdminEstadisticasDTO;
import com.gymprofit.api.dto.admin.AdminUsuarioDTO;
import com.gymprofit.api.dto.entity.usuario.UsuarioCreateDTO;
import com.gymprofit.api.dto.entity.usuario.UsuarioDTO;
import com.gymprofit.api.dto.entity.usuario.UsuarioEstadisticasDTO;
import com.gymprofit.api.dto.entity.usuario.UsuarioPatchDTO;
import com.gymprofit.api.dto.entity.usuario.UsuarioUpdateDTO;
import com.gymprofit.api.entity.FotoPerfil;
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
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

// ============================================================
// UsuarioService — implementa la gestión de usuarios de GymProFit.
// Cubre el CRUD de usuarios, la carga de credenciales para Spring Security
// (loadUserByUsername), la subida/lectura de foto de perfil en BD (BLOB) y las
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
    // Fotos de perfil persistidas en BD (BLOB): el FS de Render es efímero.
    private final com.gymprofit.api.repository.jpa.IFotoPerfilRepository fotoPerfilRepository;
    // Logger para trazar las operaciones del servicio.
    private final Logger logger = LoggerFactory.getLogger(UsuarioService.class);

    // Tamaño máximo de la foto de perfil (5 MB): evita meter binarios enormes en la BD.
    private static final long MAX_FOTO_BYTES = 5 * 1024 * 1024;


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

    // Guarda la foto de perfil en BD (BLOB, tabla fotos_perfil) y marca la referencia en el
    // usuario. Antes se guardaba en disco, pero el filesystem de Render es EFÍMERO y las
    // fotos se perdían en cada redeploy.
    @Override
    @Transactional
    public UsuarioDTO uploadFotoPerfil(Integer id, MultipartFile file) {
        logger.info("Subiendo foto de perfil para usuario id={}", id);

        securityUtils.checkOwnership(id);

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("Usuario con id " + id + " no encontrado"));

        // Validaciones básicas del binario antes de persistirlo.
        if (file == null || file.isEmpty()) {
            throw new InvalidDataException("La foto de perfil está vacía");
        }
        if (file.getSize() > MAX_FOTO_BYTES) {
            throw new InvalidDataException("La foto de perfil supera el tamaño máximo de 5 MB");
        }

        // Se leen los bytes una sola vez para poder inspeccionar la cabecera y persistirla.
        byte[] datos;
        try {
            datos = file.getBytes();
        } catch (IOException e) {
            throw new RuntimeException("Error al leer la foto de perfil: " + e.getMessage());
        }

        // Validación del contenido REAL por magic bytes: no se confía en el content-type
        // que envía el cliente (se puede falsear para colar un binario que no es imagen).
        String tipoReal = detectarTipoImagen(datos);
        if (tipoReal == null) {
            throw new InvalidDataException("El archivo no es una imagen válida (se admite JPEG, PNG o WEBP)");
        }

        // Upsert por usuario (PK = usuario_id): reutiliza la fila si ya tenía foto.
        FotoPerfil foto = fotoPerfilRepository.findById(id).orElseGet(FotoPerfil::new);
        foto.setUsuarioId(id);
        foto.setDatos(datos);
        foto.setContentType(tipoReal);
        foto.setFechaActualizacion(LocalDateTime.now());
        fotoPerfilRepository.save(foto);

        // La columna legacy foto_perfil queda como marcador de "tiene foto".
        usuario.setFotoPerfil(id + ".jpg");
        usuarioRepository.save(usuario);

        return usuarioMapper.toDTO(usuario);
    }

    // Detecta el tipo real de una imagen por sus bytes de cabecera (magic numbers),
    // ignorando lo que declare el cliente. Devuelve el MIME si es una imagen soportada
    // (JPEG/PNG/WEBP) o null si el binario no es ninguna de ellas.
    private static String detectarTipoImagen(byte[] d) {
        if (d == null) {
            return null;
        }
        // JPEG: FF D8 FF
        if (d.length >= 3
                && (d[0] & 0xFF) == 0xFF && (d[1] & 0xFF) == 0xD8 && (d[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        // PNG: 89 50 4E 47 0D 0A 1A 0A
        if (d.length >= 8
                && (d[0] & 0xFF) == 0x89 && d[1] == 'P' && d[2] == 'N' && d[3] == 'G'
                && (d[4] & 0xFF) == 0x0D && (d[5] & 0xFF) == 0x0A
                && (d[6] & 0xFF) == 0x1A && (d[7] & 0xFF) == 0x0A) {
            return "image/png";
        }
        // WEBP: "RIFF" (0-3) .... "WEBP" (8-11)
        if (d.length >= 12
                && d[0] == 'R' && d[1] == 'I' && d[2] == 'F' && d[3] == 'F'
                && d[8] == 'W' && d[9] == 'E' && d[10] == 'B' && d[11] == 'P') {
            return "image/webp";
        }
        return null;
    }

    // Devuelve los bytes de la foto de perfil desde la BD.
    @Override
    public byte[] getFotoPerfil(Integer id) {
        return fotoPerfilRepository.findById(id)
                .map(FotoPerfil::getDatos)
                .orElseThrow(() -> new NotFoundEntityException("El usuario " + id + " no tiene foto de perfil"));
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