package com.gymprofit.api.mappers;

import com.gymprofit.api.dto.entity.notificacion.NotificacionCreateDTO;
import com.gymprofit.api.dto.entity.notificacion.NotificacionDTO;
import com.gymprofit.api.entity.Notificacion;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

// ============================================================
// NotificacionMapper — mapeo MapStruct entre Notificacion y sus DTOs.
// Convierte la entidad Notificacion a NotificacionDTO (aplanando el id del
// usuario) y crea nuevas entidades a partir de NotificacionCreateDTO, dejando
// los campos gestionados por el servidor (fecha, leída, etc.) fuera del mapeo.
// ============================================================
@Mapper(componentModel = "spring")
public interface NotificacionMapper {

    // Convierte una entidad Notificacion en su DTO de salida.
    @Mapping(target = "usuarioId", source = "usuario.id")
    @Mapping(target = "tipo", source = "tipo")
    NotificacionDTO toDTO(Notificacion notificacion);

    // Convierte una lista de entidades Notificacion en su lista de DTOs.
    List<NotificacionDTO> toDTOList(List<Notificacion> notificaciones);

    // Crea una entidad Notificacion a partir del DTO de creación; los campos
    // generados por el sistema (id, usuario, fecha, leida, tipo) se ignoran aquí.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "usuario", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "leida", ignore = true)
    @Mapping(target = "tipo", ignore = true)
    Notificacion toEntity(NotificacionCreateDTO notificacionCreateDTO);
}
