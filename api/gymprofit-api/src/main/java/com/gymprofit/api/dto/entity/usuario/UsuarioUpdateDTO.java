package com.gymprofit.api.dto.entity.usuario;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.gymprofit.api.enums.TipoObjetivo;

import java.io.Serializable;

// ============================================================
// UsuarioUpdateDTO — DTO de entrada para la actualización completa de un usuario
// Requiere el id del usuario a actualizar y permite modificar sus datos
// de perfil (email, peso, altura, edad, nivel, objetivo y estado activo).
// Pensado para el flujo de actualización completa (rol ADMIN).
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UsuarioUpdateDTO implements Serializable {
    // Identificador del usuario a actualizar; obligatorio.
    @NotNull
    private Integer id;

    @Email
    @Size(max = 100)
    private String email;

    @Positive
    private Double peso;

    @Positive
    private Double altura;

    @Min(0) @Max(120)
    private Integer edad;

    private String nivelExperiencia;
    private TipoObjetivo objetivo;
    private Boolean activo;
}
