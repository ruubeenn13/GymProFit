package com.gymprofit.api.dto.entity.notificacion;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

// ============================================================
// NotificacionPatchDTO — actualización parcial (PATCH) de una notificación.
// Todos los campos son opcionales: solo se modifican los que llegan no nulos,
// permitiendo por ejemplo marcar como leída sin tocar el resto de datos.
// ============================================================
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
