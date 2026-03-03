package com.gymprofit.api.dto.entity.notificacion;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificacionCreateDTO implements Serializable {
    private Integer usuarioId;
    private String titulo;
    private String mensaje;
    private String tipo;
    private LocalDateTime fechaProgramada;
}
