package com.gymprofit.api.mappers;

import com.gymprofit.api.dto.entity.objetivopersonal.ObjetivoPersonalCreateDTO;
import com.gymprofit.api.dto.entity.objetivopersonal.ObjetivoPersonalDTO;
import com.gymprofit.api.entity.ObjetivoPersonal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

// ============================================================
// ObjetivoPersonalMapper — mapeo MapStruct entre ObjetivoPersonal y sus DTOs.
// Convierte la entidad ObjetivoPersonal a ObjetivoPersonalDTO (aplanando el id
// del usuario) y crea nuevas entidades a partir de ObjetivoPersonalCreateDTO,
// dejando fuera los campos de progreso gestionados por el servicio.
// ============================================================
@Mapper(componentModel = "spring")
public interface ObjetivoPersonalMapper {

    // Convierte una entidad ObjetivoPersonal en su DTO de salida.
    @Mapping(target = "usuarioId", source = "usuario.id")
    ObjetivoPersonalDTO toDTO(ObjetivoPersonal objetivoPersonal);

    // Convierte una lista de entidades ObjetivoPersonal en su lista de DTOs.
    List<ObjetivoPersonalDTO> toDTOList(List<ObjetivoPersonal> objetivosPersonales);

    // Crea una entidad ObjetivoPersonal a partir del DTO de creación; los campos
    // de estado (fechaInicio, completado, fechaCompletado) se ignoran aquí.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "usuario", ignore = true)
    @Mapping(target = "fechaInicio", ignore = true)
    @Mapping(target = "completado", ignore = true)
    @Mapping(target = "fechaCompletado", ignore = true)
    ObjetivoPersonal toEntity(ObjetivoPersonalCreateDTO objetivoPersonalCreateDTO);
}