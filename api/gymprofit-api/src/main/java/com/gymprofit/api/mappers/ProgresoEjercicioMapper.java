package com.gymprofit.api.mappers;

import com.gymprofit.api.dto.entity.progresoejercicio.ProgresoEjercicioCreateDTO;
import com.gymprofit.api.dto.entity.progresoejercicio.ProgresoEjercicioDTO;
import com.gymprofit.api.entity.ProgresoEjercicio;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProgresoEjercicioMapper {

    @Mapping(target = "usuarioId", source = "usuario.id")
    @Mapping(target = "ejercicioId", source = "ejercicio.id")
    ProgresoEjercicioDTO toDTO(ProgresoEjercicio progresoEjercicio);

    List<ProgresoEjercicioDTO> toDTOList(List<ProgresoEjercicio> progresoEjercicios);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "usuario", ignore = true)
    @Mapping(target = "ejercicio", ignore = true)
    @Mapping(target = "fecha", ignore = true)
    ProgresoEjercicio toEntity(ProgresoEjercicioCreateDTO progresoEjercicioCreateDTO);
}
