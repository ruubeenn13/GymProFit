package com.gymprofit.api.mappers;

import com.gymprofit.api.dto.entity.rutinaejercicio.RutinaEjercicioCreateDTO;
import com.gymprofit.api.dto.entity.rutinaejercicio.RutinaEjercicioDTO;
import com.gymprofit.api.entity.RutinaEjercicio;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

// ============================================================
// RutinaEjercicioMapper — mapeo MapStruct entre RutinaEjercicio y sus DTOs.
// Convierte la entidad de unión RutinaEjercicio a su DTO enriqueciéndolo con
// datos derivados del ejercicio (nombre, calorías) y crea nuevas entidades a
// partir del DTO de creación, delegando la resolución de rutina/ejercicio al servicio.
// ============================================================
@Mapper(componentModel = "spring")
public interface RutinaEjercicioMapper {

    // Convierte una entidad RutinaEjercicio en su DTO, aplanando ids y
    // copiando nombre/calorías del ejercicio asociado para evitar otra consulta.
    @Mapping(target = "rutinaId", source = "rutina.id")
    @Mapping(target = "ejercicioId", source = "ejercicio.id")
    @Mapping(target = "caloriasEjercicio", source = "ejercicio.caloriasQuemadas")
    @Mapping(target = "nombreEjercicio", source = "ejercicio.nombre")
    RutinaEjercicioDTO toDTO(RutinaEjercicio rutinaEjercicio);

    // Convierte una lista de entidades RutinaEjercicio en su lista de DTOs.
    List<RutinaEjercicioDTO> toDTOList(List<RutinaEjercicio> rutinaEjercicios);

    // Crea una entidad RutinaEjercicio a partir del DTO de creación; rutina y
    // ejercicio se ignoran porque los resuelve y asigna el servicio.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "rutina", ignore = true)
    @Mapping(target = "ejercicio", ignore = true)
    RutinaEjercicio toEntity(RutinaEjercicioCreateDTO rutinaEjercicioCreateDTO);
}
