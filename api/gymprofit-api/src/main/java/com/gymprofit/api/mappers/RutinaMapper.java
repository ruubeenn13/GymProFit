package com.gymprofit.api.mappers;

import com.gymprofit.api.dto.entity.programa.RutinaConEjerciciosDTO;
import com.gymprofit.api.dto.entity.rutina.RutinaCreateDTO;
import com.gymprofit.api.dto.entity.rutina.RutinaDTO;
import com.gymprofit.api.entity.Rutina;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import com.gymprofit.api.service.rutina.TextosRutina;

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
    @Mapping(target = "plantillaId", source = "plantilla.id")
    @Mapping(target = "plantillaCodigo", source = "plantilla.codigo")
    @Mapping(target = "programaUsuarioId", source = "programaUsuario.id")
    @Mapping(target = "programaCodigo", source = "programaUsuario.programa.codigo")
    @Mapping(target = "programaNombre", source = "programaUsuario.programa.nombre")
    @Mapping(target = "musculos", ignore = true)   // solo en /activas, los pone el servicio (GP-232)
    @Mapping(target = "ultimas", ignore = true)
    RutinaDTO toDTO(Rutina rutina);

    // La misma conversión, en el DTO que además lleva los ejercicios (GP-074). Los
    // ejercicios los pone el servicio, que los localiza y los ordena.
    @Mapping(target = "usuarioId", source = "usuario.id")
    @Mapping(target = "plantillaId", source = "plantilla.id")
    @Mapping(target = "plantillaCodigo", source = "plantilla.codigo")
    @Mapping(target = "programaUsuarioId", source = "programaUsuario.id")
    @Mapping(target = "programaCodigo", source = "programaUsuario.programa.codigo")
    @Mapping(target = "programaNombre", source = "programaUsuario.programa.nombre")
    @Mapping(target = "ejercicios", ignore = true)
    @Mapping(target = "musculos", ignore = true)
    @Mapping(target = "ultimas", ignore = true)
    @Named("conEjercicios") // fuera de la selección automática: si no, toDTOList no sabría cuál usar
    RutinaConEjerciciosDTO toConEjercicios(Rutina rutina);

    // Convierte una lista de entidades Rutina en su lista de DTOs.
    List<RutinaDTO> toDTOList(List<Rutina> rutinas);

    // Crea una entidad Rutina a partir del DTO de creación; los campos
    // calculados o de estado se ignoran porque los asigna el servicio.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "usuario", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "activa", ignore = true)
    @Mapping(target = "numEjercicios", ignore = true)
    @Mapping(target = "codigo", ignore = true)
    @Mapping(target = "esPlantilla", ignore = true)
    @Mapping(target = "plantilla", ignore = true)
    @Mapping(target = "programaUsuario", ignore = true)
    Rutina toEntity(RutinaCreateDTO rutinaCreateDTO);

    // Tras el mapeo base, localiza los textos del DTO en el idioma del request
    // (Accept-Language → LocaleContextHolder): los del catálogo eligen columna, una copia de
    // plantilla sin tocar da el de su plantilla y lo escrito por el usuario se queda (GP-245,
    // TextosRutina). MapStruct invoca este método al final de toDTO/toDTOList/toConEjercicios.
    @AfterMapping
    default void localizarTextos(Rutina rutina, @MappingTarget RutinaDTO dto) {
        boolean en = TextosRutina.enIngles();
        dto.setNombre(TextosRutina.nombre(rutina, en));
        dto.setDescripcion(TextosRutina.descripcion(rutina, en));
        dto.setCategoria(TextosRutina.categoria(rutina, en));
        if (!en) return;
        // El programa del que sale una copia es del catálogo: ese sí tiene traducción.
        if (rutina.getProgramaUsuario() != null) {
            String programaEn = rutina.getProgramaUsuario().getPrograma().getNombreEn();
            if (programaEn != null && !programaEn.isBlank()) dto.setProgramaNombre(programaEn);
        }
    }
}
