package com.gymprofit.api.mappers;

import com.gymprofit.api.dto.entity.objetivopersonal.ObjetivoPersonalCreateDTO;
import com.gymprofit.api.dto.entity.objetivopersonal.ObjetivoPersonalDTO;
import com.gymprofit.api.entity.ObjetivoPersonal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ObjetivoPersonalMapper {

    @Mapping(target = "usuarioId", source = "usuario.id")
    ObjetivoPersonalDTO toDTO(ObjetivoPersonal objetivoPersonal);

    List<ObjetivoPersonalDTO> toDTOList(List<ObjetivoPersonal> objetivosPersonales);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "usuario", ignore = true)
    @Mapping(target = "fechaInicio", ignore = true)
    @Mapping(target = "completado", ignore = true)
    @Mapping(target = "fechaCompletado", ignore = true)
    ObjetivoPersonal toEntity(ObjetivoPersonalCreateDTO objetivoPersonalCreateDTO);
}