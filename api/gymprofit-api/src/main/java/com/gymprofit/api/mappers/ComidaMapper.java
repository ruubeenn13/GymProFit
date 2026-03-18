package com.gymprofit.api.mappers;

import com.gymprofit.api.dto.entity.comida.ComidaCreateDTO;
import com.gymprofit.api.dto.entity.comida.ComidaDTO;
import com.gymprofit.api.entity.Comida;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ComidaMapper {

    @Mapping(target = "usuarioId", source = "usuario.id")
    ComidaDTO toDTO(Comida comida);

    List<ComidaDTO> toDTOList(List<Comida> comidas);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "usuario", ignore = true)
    Comida toEntity(ComidaCreateDTO comidaCreateDTO);
}
