package com.gymprofit.api.mappers;

import com.gymprofit.api.dto.entity.sesionentrenamiento.SesionEntrenamientoCreateDTO;
import com.gymprofit.api.dto.entity.sesionentrenamiento.SesionEntrenamientoDTO;
import com.gymprofit.api.entity.SesionEntrenamiento;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SesionEntrenamientoMapper {

    @Mapping(target = "usuarioId", source = "usuario.id")
    @Mapping(target = "rutinaId", source = "rutina.id")
    @Mapping(target = "nuevosLogros", ignore = true)
    SesionEntrenamientoDTO toDTO(SesionEntrenamiento sesionEntrenamiento);

    List<SesionEntrenamientoDTO> toDTOList(List<SesionEntrenamiento> sesionesEntrenamiento);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "usuario", ignore = true)
    @Mapping(target = "rutina", ignore = true)
    SesionEntrenamiento toEntity(SesionEntrenamientoCreateDTO sesionEntrenamientoCreateDTO);
}