package com.gymprofit.api.mappers;

import com.gymprofit.api.dto.entity.notificacion.NotificacionCreateDTO;
import com.gymprofit.api.dto.entity.notificacion.NotificacionDTO;
import com.gymprofit.api.entity.Notificacion;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface NotificacionMapper {

    @Mapping(target = "usuarioId", source = "usuario.id")
    @Mapping(target = "tipo", source = "tipo")
    NotificacionDTO toDTO(Notificacion notificacion);

    List<NotificacionDTO> toDTOList(List<Notificacion> notificaciones);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "usuario", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "leida", ignore = true)
    @Mapping(target = "tipo", ignore = true)
    Notificacion toEntity(NotificacionCreateDTO notificacionCreateDTO);
}
