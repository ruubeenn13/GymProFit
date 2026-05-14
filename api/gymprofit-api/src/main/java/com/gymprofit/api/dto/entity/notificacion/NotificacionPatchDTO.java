package com.gymprofit.api.dto.entity.notificacion;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificacionPatchDTO implements Serializable {
    private String titulo;
    private String mensaje;
    private String tipo;
    private LocalDateTime fechaProgramada;
    private Boolean leida;
}
