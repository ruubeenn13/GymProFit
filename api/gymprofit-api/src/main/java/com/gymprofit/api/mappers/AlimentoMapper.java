package com.gymprofit.api.mappers;

import com.gymprofit.api.dto.entity.alimento.AlimentoCreateDTO;
import com.gymprofit.api.dto.entity.alimento.AlimentoDTO;
import com.gymprofit.api.entity.Alimento;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AlimentoMapper {

    @Mapping(source = "usuario.id", target = "usuarioId")
    AlimentoDTO toDTO(Alimento alimento);

    List<AlimentoDTO> toDTOList(List<Alimento> alimentos);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "usuario", ignore = true)
    Alimento toEntity(AlimentoCreateDTO alimentoCreateDTO);
}
