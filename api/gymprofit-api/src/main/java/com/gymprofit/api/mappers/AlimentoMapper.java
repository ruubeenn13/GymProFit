package com.gymprofit.api.mappers;

import com.gymprofit.api.dto.entity.alimento.AlimentoCreateDTO;
import com.gymprofit.api.dto.entity.alimento.AlimentoDTO;
import com.gymprofit.api.dto.entity.alimento.RacionDTO;
import com.gymprofit.api.entity.Alimento;
import com.gymprofit.api.entity.AlimentoRacion;
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
    @Mapping(target = "raciones", ignore = true)
    @Mapping(target = "grupo", ignore = true)
    AlimentoDTO toDTO(Alimento alimento);

    // Convierte una lista de entidades a su correspondiente lista de DTOs.
    List<AlimentoDTO> toDTOList(List<Alimento> alimentos);

    // Crea la entidad a partir del DTO de creación, ignorando campos
    // gestionados por el service (id, estado activo y usuario).
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "usuario", ignore = true)
    @Mapping(target = "fuente", ignore = true)
    @Mapping(target = "codigoOrigen", ignore = true)
    @Mapping(target = "revisado", ignore = true)
    @Mapping(target = "raciones", ignore = true)
    Alimento toEntity(AlimentoCreateDTO alimentoCreateDTO);

    // Tras el mapeo base, localiza los textos del DTO: si el idioma del request
    // (Accept-Language → LocaleContextHolder) es inglés y la entidad tiene
    // traducción EN, la sobreescribe en el DTO; si no, se mantiene el ES (fallback).
    // MapStruct invoca este método automáticamente al final de toDTO/toDTOList.
    @AfterMapping
    default void localizarTextos(Alimento alimento, @MappingTarget AlimentoDTO dto) {
        boolean ingles = "en".equals(LocaleContextHolder.getLocale().getLanguage());
        dto.setRaciones(raciones(alimento.getRaciones(), ingles));

        // Solo se traduce si el request llegó en inglés.
        if (!ingles) return;

        if (alimento.getNombreEn() != null && !alimento.getNombreEn().isBlank())
            dto.setNombre(alimento.getNombreEn());
        if (alimento.getCategoriaEn() != null && !alimento.getCategoriaEn().isBlank())
            dto.setCategoria(alimento.getCategoriaEn());
        if (alimento.getDescripcionEn() != null && !alimento.getDescripcionEn().isBlank())
            dto.setDescripcion(alimento.getDescripcionEn());
    }

    // Raciones en el idioma de la petición (GP-127). Sin raciones, lista vacía.
    static List<RacionDTO> raciones(List<AlimentoRacion> raciones, boolean ingles) {
        if (raciones == null) return List.of();
        return raciones.stream()
                .map(r -> new RacionDTO(r.getId(), ingles ? r.getNombreEn() : r.getNombre(), r.getGramos()))
                .toList();
    }
}
