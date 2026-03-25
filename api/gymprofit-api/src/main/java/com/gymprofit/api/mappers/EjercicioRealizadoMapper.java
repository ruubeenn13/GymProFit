package com.gymprofit.api.mappers;

import com.gymprofit.api.dto.entity.ejerciciorealizado.EjercicioRealizadoCreateDTO;
import com.gymprofit.api.dto.entity.ejerciciorealizado.EjercicioRealizadoDTO;
import com.gymprofit.api.entity.EjercicioRealizado;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface EjercicioRealizadoMapper {

    @Mapping(target = "sesionId", source = "sesion.id")
    @Mapping(target = "ejercicioId", source = "ejercicio.id")
    EjercicioRealizadoDTO toDTO(EjercicioRealizado ejercicioRealizado);

    List<EjercicioRealizadoDTO> toDTOList(List<EjercicioRealizado> ejerciciosRealizados);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sesion", ignore = true)
    @Mapping(target = "ejercicio", ignore = true)
    EjercicioRealizado toEntity(EjercicioRealizadoCreateDTO ejercicioRealizadoCreateDTO);
}
