package com.gymprofit.api.mappers;

import com.gymprofit.api.dto.entity.usuario.UsuarioCreateDTO;
import com.gymprofit.api.dto.entity.usuario.UsuarioDTO;
import com.gymprofit.api.dto.entity.usuario.UsuarioUpdateDTO;
import com.gymprofit.api.entity.Usuario;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

// ============================================================
// UsuarioMapper — mapeo MapStruct entre Usuario y sus DTOs.
// Convierte la entidad Usuario a UsuarioDTO, crea nuevas entidades a partir
// de UsuarioCreateDTO y aplica actualizaciones parciales (PATCH) desde
// UsuarioUpdateDTO sobre una entidad existente sin tocar credenciales ni estado.
// ============================================================
@Mapper(componentModel = "spring")
public interface UsuarioMapper {

    // Convierte una entidad Usuario en su DTO de salida.
    UsuarioDTO toDTO(Usuario usuario);

    // Convierte una lista de entidades Usuario en su lista de DTOs.
    List<UsuarioDTO> toDTOList(List<Usuario> usuarios);

    // Crea una entidad Usuario a partir del DTO de creación; id, fecha de
    // registro y estado activo se ignoran porque los asigna el servicio.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fechaRegistro", ignore = true)
    @Mapping(target = "activo", ignore = true)
    // fotoPerfil y roles los asigna el servicio; authorities es derivado de roles (UserDetails).
    @Mapping(target = "fotoPerfil", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "authorities", ignore = true)
    Usuario toEntity(UsuarioCreateDTO usuarioCreateDTO);

    // Actualiza en el sitio (@MappingTarget) los campos editables de una
    // entidad Usuario existente a partir del DTO de actualización; username,
    // password, fechaRegistro y activo quedan fuera de este mapeo por seguridad.
    @Mapping(target = "username", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "fechaRegistro", ignore = true)
    @Mapping(target = "activo", ignore = true)
    // fotoPerfil y roles no se editan en un update de perfil; authorities es derivado (UserDetails).
    @Mapping(target = "fotoPerfil", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "authorities", ignore = true)
    void updateEntityFromDTO(UsuarioUpdateDTO usuarioUpdateDTO, @MappingTarget Usuario usuario);
}