package com.gymprofit.api.mappers;

import com.gymprofit.api.dto.entity.alimentocomida.AlimentoComidaCreateDTO;
import com.gymprofit.api.dto.entity.alimentocomida.AlimentoComidaDTO;
import com.gymprofit.api.entity.AlimentoComida;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.context.i18n.LocaleContextHolder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

// ============================================================
// AlimentoComidaMapper — conversión entre AlimentoComida y sus DTOs
// Mapper MapStruct que traduce la relación alimento-comida a DTO. Los macros
// (proteínas/carbohidratos/grasas) son los guardados en la línea al escribirla
// (lote 1.6.6, A1), no los de hoy del alimento.
// ============================================================
@Mapper(componentModel = "spring", imports = {BigDecimal.class, RoundingMode.class})
public interface AlimentoComidaMapper {

    // Convierte la entidad a DTO; kcal y macros, tal como se guardaron.
    @Mapping(target = "comidaId", source = "comida.id")
    @Mapping(target = "alimentoId", source = "alimento.id")
    @Mapping(target = "nombreAlimento", source = "alimento.nombre")
    @Mapping(target = "categoriaAlimento", source = "alimento.categoria")
    @Mapping(target = "usuarioIdAlimento", source = "alimento.usuario.id")
    @Mapping(target = "racionId", source = "racion.id")
    @Mapping(target = "racionGramos", source = "racion.gramos")
    @Mapping(target = "racionNombre", ignore = true)
    @Mapping(target = "racionUnidad", ignore = true)
    @Mapping(target = "racionUnidadPlural", ignore = true)
    @Mapping(target = "racionSinPeso", ignore = true)
    @Mapping(target = "alimentoEsPlato", ignore = true)
    AlimentoComidaDTO toDTO(AlimentoComida alimentoComida);

    // Convierte una lista de entidades a su correspondiente lista de DTOs.
    List<AlimentoComidaDTO> toDTOList(List<AlimentoComida> alimentosComidas);

    // Crea la entidad a partir del DTO de creación, ignorando campos
    // que se resuelven en el service (id, relaciones y calorías totales).
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "comida", ignore = true)
    @Mapping(target = "alimento", ignore = true)
    @Mapping(target = "caloriasTotales", ignore = true)
    @Mapping(target = "racion", ignore = true)
    @Mapping(target = "raciones", ignore = true)
    @Mapping(target = "proteinasTotales", ignore = true)
    @Mapping(target = "carbohidratosTotales", ignore = true)
    @Mapping(target = "grasasTotales", ignore = true)
    AlimentoComida toEntity(AlimentoComidaCreateDTO alimentoComidaCreateDTO);

    // El nombre del alimento y el de la ración en el idioma de la petición, con el español
    // de respaldo, como en AlimentoMapper (GP-176). La categoría NO se traduce: sale en su
    // forma canónica (AlimentoController.CATEGORIAS) porque la app la usa como clave para
    // el icono y el nombre (NombreLineaIdiomaTest lo fija).
    @AfterMapping
    default void nombreRacion(AlimentoComida alimentoComida, @MappingTarget AlimentoComidaDTO dto) {
        boolean ingles = "en".equals(LocaleContextHolder.getLocale().getLanguage());
        if (ingles && alimentoComida.getAlimento() != null) {
            String nombreEn = alimentoComida.getAlimento().getNombreEn();
            if (nombreEn != null && !nombreEn.isBlank()) dto.setNombreAlimento(nombreEn);
        }
        // Un plato (lote 1.6.6): la app le pone su marca, y una línea con la ración de un plato
        // sin peso hecho no enseña gramos, porque no son de verdad.
        dto.setAlimentoEsPlato(com.gymprofit.api.service.plato.Platos.esPlato(alimentoComida.getAlimento()));
        if (alimentoComida.getRacion() == null) return;
        dto.setRacionSinPeso(com.gymprofit.api.service.plato.Platos.esRacionSinPeso(alimentoComida.getRacion()));
        // GP-177: si la ración ya no pesa lo mismo, la línea sale en gramos, sin nada de
        // la ración, y todas las builds la enseñan y la editan así.
        if (com.gymprofit.api.service.alimentocomida.RacionVigente.de(alimentoComida) == null) {
            dto.setRacionId(null);
            dto.setRacionGramos(null);
            dto.setRaciones(null);
            return;
        }
        String nombre = ingles ? alimentoComida.getRacion().getNombreEn() : alimentoComida.getRacion().getNombre();
        dto.setRacionNombre(nombre);
        com.gymprofit.api.service.busqueda.UnidadesRacion.de(nombre, ingles).ifPresent(u -> {
            dto.setRacionUnidad(u.singular());
            dto.setRacionUnidadPlural(u.plural());
        });
    }
}
