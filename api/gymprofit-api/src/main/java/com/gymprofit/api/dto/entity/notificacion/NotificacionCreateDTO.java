package com.gymprofit.api.dto.entity.notificacion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificacionCreateDTO implements Serializable {
    @NotNull
    private Integer usuarioId;

    @NotBlank
    @Size(max = 200)
    private String titulo;

    @NotBlank
    private String mensaje;

    @NotBlank
    private String tipo;

    private LocalDateTime fechaProgramada;
}
