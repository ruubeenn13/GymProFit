package com.gymprofit.api.mappers;

import com.gymprofit.api.dto.entity.logro.LogroDTO;
import com.gymprofit.api.dto.entity.logro.UsuarioLogroDTO;
import com.gymprofit.api.entity.Logro;
import com.gymprofit.api.entity.UsuarioLogro;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

// ============================================================
// LogroMapper — conversión entre Logro/UsuarioLogro y sus DTOs
// Mapper MapStruct para el sistema de logros/gamificación: traduce
// tanto el catálogo de logros como la relación usuario-logro
// (logro obtenido por un usuario concreto) a DTOs de lectura.
// ============================================================
@Mapper(componentModel = "spring")
public interface LogroMapper {

    // Convierte la entidad Logro (catálogo) a DTO.
    LogroDTO toDTO(Logro logro);

    // Convierte una lista de logros a su correspondiente lista de DTOs.
    List<LogroDTO> toDTOList(List<Logro> logros);

    // Convierte la relación usuario-logro a DTO, aplanando los datos
    // del logro asociado (id, nombre, descripción y tipo).
    @Mapping(source = "logro.id",          target = "logroId")
    @Mapping(source = "logro.nombre",      target = "logroNombre")
    @Mapping(source = "logro.descripcion", target = "logroDescripcion")
    @Mapping(source = "logro.tipo",        target = "logroTipo")
    UsuarioLogroDTO toUsuarioLogroDTO(UsuarioLogro usuarioLogro);

    // Convierte una lista de relaciones usuario-logro a su lista de DTOs.
    List<UsuarioLogroDTO> toUsuarioLogroDTOList(List<UsuarioLogro> usuarioLogros);
}
