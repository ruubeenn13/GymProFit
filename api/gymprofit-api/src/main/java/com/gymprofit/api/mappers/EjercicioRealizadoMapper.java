package com.gymprofit.api.mappers;

import com.gymprofit.api.dto.entity.ejerciciorealizado.EjercicioRealizadoCreateDTO;
import com.gymprofit.api.dto.entity.ejerciciorealizado.EjercicioRealizadoDTO;
import com.gymprofit.api.entity.EjercicioRealizado;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

// ============================================================
// EjercicioRealizadoMapper — conversión entre EjercicioRealizado y sus DTOs
// Mapper MapStruct que traduce el registro de un ejercicio realizado
// dentro de una sesión de entrenamiento a DTOs de lectura y creación.
// ============================================================
@Mapper(componentModel = "spring")
public interface EjercicioRealizadoMapper {

    // Convierte la entidad a DTO, exponiendo los ids de sesión y ejercicio.
    @Mapping(target = "sesionId", source = "sesion.id")
    @Mapping(target = "ejercicioId", source = "ejercicio.id")
    EjercicioRealizadoDTO toDTO(EjercicioRealizado ejercicioRealizado);

    // Convierte una lista de entidades a su correspondiente lista de DTOs.
    List<EjercicioRealizadoDTO> toDTOList(List<EjercicioRealizado> ejerciciosRealizados);

    // Crea la entidad a partir del DTO de creación, ignorando campos
    // gestionados por el service (id, sesión y ejercicio).
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sesion", ignore = true)
    @Mapping(target = "ejercicio", ignore = true)
    EjercicioRealizado toEntity(EjercicioRealizadoCreateDTO ejercicioRealizadoCreateDTO);
}
