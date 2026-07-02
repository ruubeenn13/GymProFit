package com.gymprofit.api.mappers;

import com.gymprofit.api.dto.entity.ejercicio.EjercicioCreateDTO;
import com.gymprofit.api.dto.entity.ejercicio.EjercicioDTO;
import com.gymprofit.api.entity.Ejercicio;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

// ============================================================
// EjercicioMapper — conversión entre Ejercicio y sus DTOs
// Mapper MapStruct que traduce el catálogo de ejercicios del
// gimnasio (grupo muscular, dificultad) a DTOs de lectura y creación.
// ============================================================
@Mapper(componentModel = "spring")
public interface EjercicioMapper {

    // Convierte la entidad a DTO.
    @Mapping(target = "grupoMuscular", source = "grupoMuscular")
    @Mapping(target = "dificultad", source = "dificultad")
    EjercicioDTO toDTO(Ejercicio ejercicio);

    // Convierte una lista de entidades a su correspondiente lista de DTOs.
    List<EjercicioDTO> toDTOList(List<Ejercicio> ejercicios);

    // Crea la entidad a partir del DTO de creación, ignorando campos
    // gestionados por el service (id y estado activo).
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "activo", ignore = true)
    Ejercicio toEntity(EjercicioCreateDTO ejercicioCreateDTO);
}
