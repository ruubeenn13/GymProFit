package com.gymprofit.api.mappers;

import com.gymprofit.api.dto.entity.comida.ComidaCreateDTO;
import com.gymprofit.api.dto.entity.comida.ComidaDTO;
import com.gymprofit.api.entity.Comida;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

// ============================================================
// ComidaMapper — conversión entre Comida y sus DTOs
// Mapper MapStruct que traduce las comidas registradas por el
// usuario (desayuno, almuerzo, etc.) a DTOs de lectura y creación.
// ============================================================
@Mapper(componentModel = "spring")
public interface ComidaMapper {

    // Convierte la entidad a DTO, exponiendo solo el id del usuario propietario.
    @Mapping(target = "usuarioId", source = "usuario.id")
    ComidaDTO toDTO(Comida comida);

    // Convierte una lista de entidades a su correspondiente lista de DTOs.
    List<ComidaDTO> toDTOList(List<Comida> comidas);

    // Crea la entidad a partir del DTO de creación, ignorando campos
    // gestionados por el service (id y usuario).
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "usuario", ignore = true)
    // Los totales de macros son campos calculados de la entidad, no vienen del DTO de creación.
    @Mapping(target = "totalCalorias", ignore = true)
    @Mapping(target = "totalProteinas", ignore = true)
    @Mapping(target = "totalCarbohidratos", ignore = true)
    @Mapping(target = "totalGrasas", ignore = true)
    Comida toEntity(ComidaCreateDTO comidaCreateDTO);
}
