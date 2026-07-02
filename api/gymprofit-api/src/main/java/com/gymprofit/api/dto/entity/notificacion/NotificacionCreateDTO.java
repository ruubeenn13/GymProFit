package com.gymprofit.api.dto.entity.notificacion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

// ============================================================
// NotificacionCreateDTO — DTO para la creación de una notificación
// Recoge los datos necesarios para generar una notificación dirigida
// a un usuario, con soporte para envío programado.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificacionCreateDTO implements Serializable {
    // Usuario destinatario de la notificación, obligatorio
    @NotNull
    private Integer usuarioId;

    // Título de la notificación, obligatorio y máximo 200 caracteres
    @NotBlank
    @Size(max = 200)
    private String titulo;

    // Cuerpo/mensaje de la notificación, obligatorio
    @NotBlank
    private String mensaje;

    // Tipo de notificación (categoría), obligatorio
    @NotBlank
    private String tipo;

    // Fecha/hora en la que se debe enviar la notificación (opcional, envío programado)
    private LocalDateTime fechaProgramada;
}
