package com.gymprofit.api.mappers;

import com.gymprofit.api.dto.entity.alimentocomida.AlimentoComidaCreateDTO;
import com.gymprofit.api.dto.entity.alimentocomida.AlimentoComidaDTO;
import com.gymprofit.api.entity.AlimentoComida;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AlimentoComidaMapper {

    @Mapping(target = "comidaId", source = "comida.id")
    @Mapping(target = "alimentoId", source = "alimento.id")
    AlimentoComidaDTO toDTO(AlimentoComida alimentoComida);

    List<AlimentoComidaDTO> toDTOList(List<AlimentoComida> alimentosComidas);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "comida", ignore = true)
    @Mapping(target = "alimento", ignore = true)
    @Mapping(target = "caloriasTotales", ignore = true)
    AlimentoComida toEntity(AlimentoComidaCreateDTO alimentoComidaCreateDTO);
}
