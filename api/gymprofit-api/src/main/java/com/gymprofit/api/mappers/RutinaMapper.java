package com.gymprofit.api.mappers;

import com.gymprofit.api.dto.entity.rutina.RutinaCreateDTO;
import com.gymprofit.api.dto.entity.rutina.RutinaDTO;
import com.gymprofit.api.entity.Rutina;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.context.i18n.LocaleContextHolder;

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

    // Tras el mapeo base, localiza los textos del DTO: si el idioma del request
    // (Accept-Language → LocaleContextHolder) es inglés y la rutina tiene
    // traducción EN (solo las predefinidas del catálogo la tienen), la
    // sobreescribe; si no, se mantiene el ES (fallback).
    // MapStruct invoca este método automáticamente al final de toDTO/toDTOList.
    @AfterMapping
    default void localizarTextos(Rutina rutina, @MappingTarget RutinaDTO dto) {
        // Solo se traduce si el request llegó en inglés.
        if (!"en".equals(LocaleContextHolder.getLocale().getLanguage())) return;

        if (rutina.getNombreEn() != null && !rutina.getNombreEn().isBlank())
            dto.setNombre(rutina.getNombreEn());
        if (rutina.getDescripcionEn() != null && !rutina.getDescripcionEn().isBlank())
            dto.setDescripcion(rutina.getDescripcionEn());
        if (rutina.getCategoriaEn() != null && !rutina.getCategoriaEn().isBlank())
            dto.setCategoria(rutina.getCategoriaEn());
    }
}
