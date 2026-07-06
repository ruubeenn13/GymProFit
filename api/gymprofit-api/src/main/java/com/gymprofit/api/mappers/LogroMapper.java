package com.gymprofit.api.mappers;

import com.gymprofit.api.dto.entity.logro.LogroDTO;
import com.gymprofit.api.dto.entity.logro.UsuarioLogroDTO;
import com.gymprofit.api.entity.Logro;
import com.gymprofit.api.entity.UsuarioLogro;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.List;

// ============================================================
// LogroMapper — conversión entre Logro/UsuarioLogro y sus DTOs
// Mapper MapStruct para el sistema de logros/gamificación: traduce
// tanto el catálogo de logros como la relación usuario-logro
// (logro obtenido por un usuario concreto) a DTOs de lectura.
// ============================================================
@Mapper(componentModel = "spring")
public interface LogroMapper {

    // Convierte la entidad Logro (catálogo) a DTO.
    LogroDTO toDTO(Logro logro);

    // Convierte una lista de logros a su correspondiente lista de DTOs.
    List<LogroDTO> toDTOList(List<Logro> logros);

    // Convierte la relación usuario-logro a DTO, aplanando los datos
    // del logro asociado (id, nombre, descripción y tipo).
    @Mapping(source = "logro.id",          target = "logroId")
    @Mapping(source = "logro.nombre",      target = "logroNombre")
    @Mapping(source = "logro.descripcion", target = "logroDescripcion")
    @Mapping(source = "logro.tipo",        target = "logroTipo")
    UsuarioLogroDTO toUsuarioLogroDTO(UsuarioLogro usuarioLogro);

    // Convierte una lista de relaciones usuario-logro a su lista de DTOs.
    List<UsuarioLogroDTO> toUsuarioLogroDTOList(List<UsuarioLogro> usuarioLogros);

    // Tras el mapeo base, localiza los textos del DTO de catálogo: si el idioma
    // del request (Accept-Language → LocaleContextHolder) es inglés y el logro
    // tiene traducción EN, la sobreescribe; si no, se mantiene el ES (fallback).
    // MapStruct invoca este método automáticamente al final de toDTO/toDTOList.
    @AfterMapping
    default void localizarTextos(Logro logro, @MappingTarget LogroDTO dto) {
        // Solo se traduce si el request llegó en inglés.
        if (!"en".equals(LocaleContextHolder.getLocale().getLanguage())) return;

        if (logro.getNombreEn() != null && !logro.getNombreEn().isBlank())
            dto.setNombre(logro.getNombreEn());
        if (logro.getDescripcionEn() != null && !logro.getDescripcionEn().isBlank())
            dto.setDescripcion(logro.getDescripcionEn());
    }

    // Misma localización para el DTO aplanado usuario-logro (logros obtenidos):
    // sobreescribe logroNombre/logroDescripcion con la versión EN si existe.
    @AfterMapping
    default void localizarTextos(UsuarioLogro usuarioLogro, @MappingTarget UsuarioLogroDTO dto) {
        // Solo se traduce si el request llegó en inglés y hay logro asociado.
        if (!"en".equals(LocaleContextHolder.getLocale().getLanguage())) return;
        Logro logro = usuarioLogro.getLogro();
        if (logro == null) return;

        if (logro.getNombreEn() != null && !logro.getNombreEn().isBlank())
            dto.setLogroNombre(logro.getNombreEn());
        if (logro.getDescripcionEn() != null && !logro.getDescripcionEn().isBlank())
            dto.setLogroDescripcion(logro.getDescripcionEn());
    }
}
