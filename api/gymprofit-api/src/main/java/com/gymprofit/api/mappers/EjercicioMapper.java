package com.gymprofit.api.mappers;

import com.gymprofit.api.dto.entity.ejercicio.EjercicioCreateDTO;
import com.gymprofit.api.dto.entity.ejercicio.EjercicioDTO;
import com.gymprofit.api.entity.Ejercicio;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.context.i18n.LocaleContextHolder;

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

    // Tras el mapeo base, localiza los textos del DTO: si el idioma del request
    // (Accept-Language → LocaleContextHolder) es inglés y la entidad tiene
    // traducción EN, la sobreescribe en el DTO; si no, se mantiene el ES (fallback).
    // MapStruct invoca este método automáticamente al final de toDTO/toDTOList.
    @AfterMapping
    default void localizarTextos(Ejercicio ejercicio, @MappingTarget EjercicioDTO dto) {
        // Solo se traduce si el request llegó en inglés.
        if (!"en".equals(LocaleContextHolder.getLocale().getLanguage())) return;

        if (ejercicio.getNombreEn() != null && !ejercicio.getNombreEn().isBlank())
            dto.setNombre(ejercicio.getNombreEn());
        if (ejercicio.getDescripcionEn() != null && !ejercicio.getDescripcionEn().isBlank())
            dto.setDescripcion(ejercicio.getDescripcionEn());
        if (ejercicio.getInstruccionesEn() != null && !ejercicio.getInstruccionesEn().isBlank())
            dto.setInstrucciones(ejercicio.getInstruccionesEn());
        if (ejercicio.getEquipoNecesarioEn() != null && !ejercicio.getEquipoNecesarioEn().isBlank())
            dto.setEquipoNecesario(ejercicio.getEquipoNecesarioEn());
        if (ejercicio.getMusculoPrimarioEn() != null && !ejercicio.getMusculoPrimarioEn().isBlank())
            dto.setMusculoPrimario(ejercicio.getMusculoPrimarioEn());
    }
}
