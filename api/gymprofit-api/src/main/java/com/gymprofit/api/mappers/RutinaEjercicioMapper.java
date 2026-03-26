package com.gymprofit.api.mappers;

import com.gymprofit.api.dto.entity.rutinaejercicio.RutinaEjercicioCreateDTO;
import com.gymprofit.api.dto.entity.rutinaejercicio.RutinaEjercicioDTO;
import com.gymprofit.api.entity.RutinaEjercicio;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RutinaEjercicioMapper {

    @Mapping(target = "rutinaId", source = "rutina.id")
    @Mapping(target = "ejercicioId", source = "ejercicio.id")
    RutinaEjercicioDTO toDTO(RutinaEjercicio rutinaEjercicio);

    List<RutinaEjercicioDTO> toDTOList(List<RutinaEjercicio> rutinaEjercicios);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "rutina", ignore = true)
    @Mapping(target = "ejercicio", ignore = true)
    RutinaEjercicio toEntity(RutinaEjercicioCreateDTO rutinaEjercicioCreateDTO);
}
