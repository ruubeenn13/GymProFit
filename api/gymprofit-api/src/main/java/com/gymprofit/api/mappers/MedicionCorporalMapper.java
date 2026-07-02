package com.gymprofit.api.mappers;

import com.gymprofit.api.dto.entity.medicioncorporal.MedicionCorporalCreateDTO;
import com.gymprofit.api.dto.entity.medicioncorporal.MedicionCorporalDTO;
import com.gymprofit.api.entity.MedicionCorporal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

// ============================================================
// MedicionCorporalMapper — conversión entre MedicionCorporal y sus DTOs
// Mapper MapStruct que traduce las mediciones corporales (peso,
// medidas, IMC) registradas por el usuario a DTOs de lectura y creación.
// ============================================================
@Mapper(componentModel = "spring")
public interface MedicionCorporalMapper {

    // Convierte la entidad a DTO, exponiendo solo el id del usuario propietario.
    @Mapping(target = "usuarioId", source = "usuario.id")
    MedicionCorporalDTO toDTO(MedicionCorporal medicionCorporal);

    // Convierte una lista de entidades a su correspondiente lista de DTOs.
    List<MedicionCorporalDTO> toDTOList(List<MedicionCorporal> medicionesCorporales);

    // Crea la entidad a partir del DTO de creación, ignorando campos
    // calculados o gestionados por el service (id, usuario, fecha e IMC).
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "usuario", ignore = true)
    @Mapping(target = "fecha", ignore = true)
    @Mapping(target = "imc", ignore = true)
    MedicionCorporal toEntity(MedicionCorporalCreateDTO medicionCorporalCreateDTO);
}
