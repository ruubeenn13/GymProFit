package com.gymprofit.api.mappers;

import com.gymprofit.api.dto.entity.progresoejercicio.ProgresoEjercicioCreateDTO;
import com.gymprofit.api.dto.entity.progresoejercicio.ProgresoEjercicioDTO;
import com.gymprofit.api.entity.ProgresoEjercicio;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

// ============================================================
// ProgresoEjercicioMapper — mapeo MapStruct entre ProgresoEjercicio y sus DTOs.
// Convierte la entidad ProgresoEjercicio a ProgresoEjercicioDTO (aplanando los
// ids de usuario y ejercicio) y crea nuevas entidades a partir del DTO de
// creación, dejando la fecha de registro para que la asigne el servicio.
// ============================================================
@Mapper(componentModel = "spring")
public interface ProgresoEjercicioMapper {

    // Convierte una entidad ProgresoEjercicio en su DTO de salida.
    @Mapping(target = "usuarioId", source = "usuario.id")
    @Mapping(target = "ejercicioId", source = "ejercicio.id")
    ProgresoEjercicioDTO toDTO(ProgresoEjercicio progresoEjercicio);

    // Convierte una lista de entidades ProgresoEjercicio en su lista de DTOs.
    List<ProgresoEjercicioDTO> toDTOList(List<ProgresoEjercicio> progresoEjercicios);

    // Crea una entidad ProgresoEjercicio a partir del DTO de creación; usuario,
    // ejercicio y fecha se ignoran porque los asigna el servicio.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "usuario", ignore = true)
    @Mapping(target = "ejercicio", ignore = true)
    @Mapping(target = "fecha", ignore = true)
    ProgresoEjercicio toEntity(ProgresoEjercicioCreateDTO progresoEjercicioCreateDTO);
}
