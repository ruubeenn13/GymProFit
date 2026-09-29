package com.gymprofit.api.mappers;

import com.gymprofit.api.dto.entity.rutinaejercicio.RutinaEjercicioCreateDTO;
import com.gymprofit.api.dto.entity.rutinaejercicio.RutinaEjercicioDTO;
import com.gymprofit.api.entity.RutinaEjercicio;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.context.i18n.LocaleContextHolder;

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
    @Mapping(target = "nombreEjercicio", source = "ejercicio.nombre")
    RutinaEjercicioDTO toDTO(RutinaEjercicio rutinaEjercicio);

    // Con la petición en inglés (Accept-Language → LocaleContextHolder) y traducción en el
    // ejercicio, el nombre en inglés; si no, el español (GP-132). MapStruct lo llama al
    // final de toDTO y de toDTOList.
    @AfterMapping
    default void localizarNombre(RutinaEjercicio rutinaEjercicio, @MappingTarget RutinaEjercicioDTO dto) {
        if (!"en".equals(LocaleContextHolder.getLocale().getLanguage())) return;
        if (rutinaEjercicio.getEjercicio() == null) return;
        String en = rutinaEjercicio.getEjercicio().getNombreEn();
        if (en != null && !en.isBlank()) dto.setNombreEjercicio(en);
    }

    // Convierte una lista de entidades RutinaEjercicio en su lista de DTOs.
    List<RutinaEjercicioDTO> toDTOList(List<RutinaEjercicio> rutinaEjercicios);

    // Crea una entidad RutinaEjercicio a partir del DTO de creación; rutina y
    // ejercicio se ignoran porque los resuelve y asigna el servicio.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "rutina", ignore = true)
    @Mapping(target = "ejercicio", ignore = true)
    @Mapping(target = "repeticionesMin", ignore = true)
    @Mapping(target = "repeticionesMax", ignore = true)
    @Mapping(target = "medida", ignore = true)
    @Mapping(target = "tipo", ignore = true)
    @Mapping(target = "porLado", ignore = true)
    @Mapping(target = "notasEn", ignore = true)
    RutinaEjercicio toEntity(RutinaEjercicioCreateDTO rutinaEjercicioCreateDTO);
}
