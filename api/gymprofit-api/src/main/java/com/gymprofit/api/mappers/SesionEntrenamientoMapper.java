package com.gymprofit.api.mappers;

import com.gymprofit.api.dto.entity.sesionentrenamiento.SesionEntrenamientoCreateDTO;
import com.gymprofit.api.dto.entity.sesionentrenamiento.SesionEntrenamientoDTO;
import com.gymprofit.api.entity.SesionEntrenamiento;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.List;

// ============================================================
// SesionEntrenamientoMapper — mapeo MapStruct entre SesionEntrenamiento y sus DTOs.
// Convierte la entidad a SesionEntrenamientoDTO (aplanando ids de usuario y
// rutina) dejando "nuevosLogros" fuera del mapeo automático porque lo rellena
// el servicio tras evaluar logros desbloqueados; crea entidades desde el DTO de creación.
// ============================================================
@Mapper(componentModel = "spring")
public interface SesionEntrenamientoMapper {

    // Convierte una entidad SesionEntrenamiento en su DTO de salida.
    // nuevosLogros se ignora aquí y lo completa el servicio tras el mapeo.
    @Mapping(target = "usuarioId", source = "usuario.id")
    @Mapping(target = "rutinaId", source = "rutina.id")
    @Mapping(target = "rutinaNombre", source = "rutina.nombre")
    @Mapping(target = "nuevosLogros", ignore = true)
    SesionEntrenamientoDTO toDTO(SesionEntrenamiento sesionEntrenamiento);

    // rutinaNombre sale de la rutina aunque esté desactivada (lote 1.2.1). Con la petición en
    // inglés y traducción en la rutina (solo las del catálogo la tienen), la traducción.
    @AfterMapping
    default void localizarRutina(SesionEntrenamiento sesion, @MappingTarget SesionEntrenamientoDTO dto) {
        if (sesion.getRutina() == null) return;
        String en = sesion.getRutina().getNombreEn();
        if ("en".equals(LocaleContextHolder.getLocale().getLanguage()) && en != null && !en.isBlank()) {
            dto.setRutinaNombre(en);
        }
    }

    // Convierte una lista de entidades SesionEntrenamiento en su lista de DTOs.
    List<SesionEntrenamientoDTO> toDTOList(List<SesionEntrenamiento> sesionesEntrenamiento);

    // Crea una entidad SesionEntrenamiento a partir del DTO de creación; usuario
    // y rutina se ignoran porque los resuelve el servicio.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "usuario", ignore = true)
    @Mapping(target = "rutina", ignore = true)
    SesionEntrenamiento toEntity(SesionEntrenamientoCreateDTO sesionEntrenamientoCreateDTO);
}