package com.gymprofit.api.mappers;

import com.gymprofit.api.dto.entity.rutina.RutinaCreateDTO;
import com.gymprofit.api.dto.entity.rutina.RutinaDTO;
import com.gymprofit.api.entity.Rutina;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RutinaMapper {

    @Mapping(target = "usuarioId", source = "usuario.id")
    @Mapping(target = "nivel", source = "nivel")
    RutinaDTO toDTO(Rutina rutina);

    List<RutinaDTO> toDTOList(List<Rutina> rutinas);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "usuario", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "activa", ignore = true)
    Rutina toEntity(RutinaCreateDTO rutinaCreateDTO);
}
