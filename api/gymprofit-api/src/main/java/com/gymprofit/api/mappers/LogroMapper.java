package com.gymprofit.api.mappers;

import com.gymprofit.api.dto.entity.logro.LogroDTO;
import com.gymprofit.api.dto.entity.logro.UsuarioLogroDTO;
import com.gymprofit.api.entity.Logro;
import com.gymprofit.api.entity.UsuarioLogro;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface LogroMapper {

    LogroDTO toDTO(Logro logro);

    List<LogroDTO> toDTOList(List<Logro> logros);

    @Mapping(source = "logro.id",          target = "logroId")
    @Mapping(source = "logro.nombre",      target = "logroNombre")
    @Mapping(source = "logro.descripcion", target = "logroDescripcion")
    @Mapping(source = "logro.tipo",        target = "logroTipo")
    UsuarioLogroDTO toUsuarioLogroDTO(UsuarioLogro usuarioLogro);

    List<UsuarioLogroDTO> toUsuarioLogroDTOList(List<UsuarioLogro> usuarioLogros);
}
