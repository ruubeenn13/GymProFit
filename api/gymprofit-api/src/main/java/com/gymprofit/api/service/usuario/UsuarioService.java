package com.gymprofit.api.service.usuario;

import com.gymprofit.api.dto.entity.usuario.UsuarioCreateDTO;
import com.gymprofit.api.dto.entity.usuario.UsuarioDTO;
import com.gymprofit.api.dto.entity.usuario.UsuarioUpdateDTO;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.exceptions.*;
import com.gymprofit.api.mappers.UsuarioMapper;
import com.gymprofit.api.repository.jpa.IUsuarioRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
public class UsuarioService implements IUsuarioService {

    private final IUsuarioRepository usuarioRepository;
    private final UsuarioMapper usuarioMapper;
    private final Logger logger = LoggerFactory.getLogger(UsuarioService.class);


    @Override
    public List<UsuarioDTO> findAll() {
        logger.info("Buscando todos los usuarios");
        List<Usuario> usuarios = (List<Usuario>) usuarioRepository.findAll();
        return usuarioMapper.toDTOList(usuarios);
    }

    @Override
    public UsuarioDTO findById(Integer id) {
        logger.info("Buscando usuario por id: {}", id);
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException("El usuario con id " + id + " no existe"));
        return usuarioMapper.toDTO(usuario);
    }

    @Override
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

    @Transactional
    @Override
    public void deleteById(Integer id) {
        logger.info("Intento de eliminr un usuario con id: {}", id);

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

    @Override
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

    @Override
    public UsuarioDTO findByUsername(String username) {
        logger.info("Buscando usuario por username: {}", username);
        Usuario usuario = usuarioRepository.findByUsername(username);

        if (usuario == null) {
            throw new NotFoundEntityException("El usuario con username '" + username + "' no existe");
        }

        return usuarioMapper.toDTO(usuario);

    }

    @Override
    public UsuarioDTO findByEmail(String email) {
        logger.info("Buscando un usuario por email: {}", email);
        Usuario usuario = usuarioRepository.findByEmail(email);

        if (usuario == null) {
            throw new NotFoundEntityException("El usuario con email '" + email + "' no existe");
        }

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

    @Override
    public List<UsuarioDTO> findActivos() {
        logger.info("Buscando usuarios activos");
        List<Usuario> usuarios = usuarioRepository.findByActivoTrue();
        return usuarioMapper.toDTOList(usuarios);
    }
}