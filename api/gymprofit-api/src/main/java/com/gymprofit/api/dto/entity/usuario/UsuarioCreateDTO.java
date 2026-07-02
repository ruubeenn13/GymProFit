package com.gymprofit.api.dto.entity.usuario;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.gymprofit.api.enums.TipoObjetivo;

import java.io.Serializable;

// ============================================================
// UsuarioCreateDTO — DTO de entrada para la creación de un usuario
// Recoge los datos necesarios para dar de alta un usuario (registro),
// con las validaciones de formato/rango aplicadas vía Bean Validation
// antes de persistir la entidad en la base de datos.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UsuarioCreateDTO implements Serializable {
    // Nombre de usuario único, entre 3 y 50 caracteres.
    @NotBlank
    @Size(min = 3, max = 50)
    private String username;

    // Contraseña en claro recibida del cliente (se cifra en el servicio antes de guardar).
    @NotBlank
    @Size(min = 6, max = 100)
    private String password;

    // Correo electrónico válido y único.
    @NotBlank
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
}
