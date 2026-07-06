package com.gymprofit.api.mappers;

import com.gymprofit.api.dto.entity.alimento.AlimentoCreateDTO;
import com.gymprofit.api.dto.entity.alimento.AlimentoDTO;
import com.gymprofit.api.entity.Alimento;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.List;

// ============================================================
// AlimentoMapper — conversión entre Alimento y sus DTOs
// Mapper MapStruct que traduce la entidad Alimento (catálogo de
// alimentos, base para el sistema de nutrición) a DTOs de lectura
// y creación.
// ============================================================
@Mapper(componentModel = "spring")
public interface AlimentoMapper {

    // Convierte la entidad a DTO, exponiendo solo el id del usuario propietario.
    @Mapping(source = "usuario.id", target = "usuarioId")
    AlimentoDTO toDTO(Alimento alimento);

    // Convierte una lista de entidades a su correspondiente lista de DTOs.
    List<AlimentoDTO> toDTOList(List<Alimento> alimentos);

    // Crea la entidad a partir del DTO de creación, ignorando campos
    // gestionados por el service (id, estado activo y usuario).
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "usuario", ignore = true)
    Alimento toEntity(AlimentoCreateDTO alimentoCreateDTO);

    // Tras el mapeo base, localiza los textos del DTO: si el idioma del request
    // (Accept-Language → LocaleContextHolder) es inglés y la entidad tiene
    // traducción EN, la sobreescribe en el DTO; si no, se mantiene el ES (fallback).
    // MapStruct invoca este método automáticamente al final de toDTO/toDTOList.
    @AfterMapping
    default void localizarTextos(Alimento alimento, @MappingTarget AlimentoDTO dto) {
        // Solo se traduce si el request llegó en inglés.
        if (!"en".equals(LocaleContextHolder.getLocale().getLanguage())) return;

        if (alimento.getNombreEn() != null && !alimento.getNombreEn().isBlank())
            dto.setNombre(alimento.getNombreEn());
        if (alimento.getCategoriaEn() != null && !alimento.getCategoriaEn().isBlank())
            dto.setCategoria(alimento.getCategoriaEn());
        if (alimento.getDescripcionEn() != null && !alimento.getDescripcionEn().isBlank())
            dto.setDescripcion(alimento.getDescripcionEn());
    }
}
