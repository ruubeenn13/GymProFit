package com.gymprofit.api.mappers;

import com.gymprofit.api.dto.entity.alimentocomida.AlimentoComidaCreateDTO;
import com.gymprofit.api.dto.entity.alimentocomida.AlimentoComidaDTO;
import com.gymprofit.api.entity.AlimentoComida;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

// ============================================================
// AlimentoComidaMapper — conversión entre AlimentoComida y sus DTOs
// Mapper MapStruct que traduce la relación alimento-comida a DTO,
// calculando los macros totales (proteínas/carbohidratos/grasas)
// proporcionalmente a la cantidad de gramos consumidos.
// ============================================================
@Mapper(componentModel = "spring", imports = {BigDecimal.class, RoundingMode.class})
public interface AlimentoComidaMapper {

    // Convierte la entidad a DTO, calculando los totales de macros
    // en función de los gramos consumidos (regla de tres sobre 100g).
    @Mapping(target = "comidaId", source = "comida.id")
    @Mapping(target = "alimentoId", source = "alimento.id")
    @Mapping(target = "nombreAlimento", source = "alimento.nombre")
    @Mapping(target = "categoriaAlimento", source = "alimento.categoria")
    @Mapping(target = "usuarioIdAlimento", source = "alimento.usuario.id")
    @Mapping(target = "proteinasTotales", expression = "java(alimentoComida.getAlimento().getProteinas() != null ? alimentoComida.getAlimento().getProteinas().multiply(alimentoComida.getCantidadGramos()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO)")
    @Mapping(target = "carbohidratosTotales", expression = "java(alimentoComida.getAlimento().getCarbohidratos() != null ? alimentoComida.getAlimento().getCarbohidratos().multiply(alimentoComida.getCantidadGramos()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO)")
    @Mapping(target = "grasasTotales", expression = "java(alimentoComida.getAlimento().getGrasas() != null ? alimentoComida.getAlimento().getGrasas().multiply(alimentoComida.getCantidadGramos()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO)")
    AlimentoComidaDTO toDTO(AlimentoComida alimentoComida);

    // Convierte una lista de entidades a su correspondiente lista de DTOs.
    List<AlimentoComidaDTO> toDTOList(List<AlimentoComida> alimentosComidas);

    // Crea la entidad a partir del DTO de creación, ignorando campos
    // que se resuelven en el service (id, relaciones y calorías totales).
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "comida", ignore = true)
    @Mapping(target = "alimento", ignore = true)
    @Mapping(target = "caloriasTotales", ignore = true)
    AlimentoComida toEntity(AlimentoComidaCreateDTO alimentoComidaCreateDTO);
}
