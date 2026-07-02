package com.gymprofit.api.dto.entity.notificacion;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

// ============================================================
// NotificacionDTO — representación completa de una notificación de usuario.
// Se usa para exponer al cliente los datos de una notificación ya persistida,
// incluyendo su estado de lectura y fechas de creación/programación.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificacionDTO implements Serializable {
    private Integer id;
    private Integer usuarioId;
    private String titulo;
    private String mensaje;
    private String tipo;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaProgramada;
    private Boolean leida;
}
