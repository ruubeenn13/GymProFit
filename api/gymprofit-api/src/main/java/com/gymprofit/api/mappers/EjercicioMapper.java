package com.gymprofit.api.mappers;

import com.gymprofit.api.dto.entity.ejercicio.EjercicioCreateDTO;
import com.gymprofit.api.dto.entity.ejercicio.EjercicioDTO;
import com.gymprofit.api.entity.Ejercicio;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface EjercicioMapper {

    @Mapping(target = "grupoMuscular", source = "grupoMuscular")
    @Mapping(target = "dificultad", source = "dificultad")
    EjercicioDTO toDTO(Ejercicio ejercicio);

    List<EjercicioDTO> toDTOList(List<Ejercicio> ejercicios);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "activo", ignore = true)
    Ejercicio toEntity(EjercicioCreateDTO ejercicioCreateDTO);
}
