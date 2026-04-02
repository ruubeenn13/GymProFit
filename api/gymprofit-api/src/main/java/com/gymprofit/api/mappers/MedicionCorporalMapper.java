package com.gymprofit.api.mappers;

import com.gymprofit.api.dto.entity.medicioncorporal.MedicionCorporalCreateDTO;
import com.gymprofit.api.dto.entity.medicioncorporal.MedicionCorporalDTO;
import com.gymprofit.api.entity.MedicionCorporal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MedicionCorporalMapper {

    @Mapping(target = "usuarioId", source = "usuario.id")
    MedicionCorporalDTO toDTO(MedicionCorporal medicionCorporal);

    List<MedicionCorporalDTO> toDTOList(List<MedicionCorporal> medicionesCorporales);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "usuario", ignore = true)
    @Mapping(target = "fecha", ignore = true)
    @Mapping(target = "imc", ignore = true)
    MedicionCorporal toEntity(MedicionCorporalCreateDTO medicionCorporalCreateDTO);
}
