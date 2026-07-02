package com.gymprofit.api.mappers;

import com.gymprofit.api.dto.entity.rutina.RutinaCreateDTO;
import com.gymprofit.api.dto.entity.rutina.RutinaDTO;
import com.gymprofit.api.entity.Rutina;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

// ============================================================
// RutinaMapper — mapeo MapStruct entre Rutina y sus DTOs.
// Convierte la entidad Rutina a RutinaDTO (aplanando el id del usuario) y
// crea nuevas entidades a partir de RutinaCreateDTO, dejando fuera los campos
// calculados/gestionados por el servicio (fecha, estado activa, contadores).
// ============================================================
@Mapper(componentModel = "spring")
public interface RutinaMapper {

    // Convierte una entidad Rutina en su DTO de salida.
    @Mapping(target = "usuarioId", source = "usuario.id")
    @Mapping(target = "nivel", source = "nivel")
    RutinaDTO toDTO(Rutina rutina);

    // Convierte una lista de entidades Rutina en su lista de DTOs.
    List<RutinaDTO> toDTOList(List<Rutina> rutinas);

    // Crea una entidad Rutina a partir del DTO de creación; los campos
    // calculados o de estado se ignoran porque los asigna el servicio.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "usuario", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "activa", ignore = true)
    @Mapping(target = "numEjercicios", ignore = true)
    @Mapping(target = "caloriasAproximadas", ignore = true)
    Rutina toEntity(RutinaCreateDTO rutinaCreateDTO);
}
