package com.gymprofit.api.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

// ============================================================
// LoginDTO — credenciales de acceso para el endpoint de login
// DTO de entrada usado por AuthService para validar usuario y contraseña
// al iniciar sesión en GymProFit.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginDTO implements Serializable {
    // Nombre de usuario para autenticación
    @NotBlank
    private String username;

    // Contraseña en texto plano (se valida contra el hash almacenado)
    @NotBlank
    private String password;
}
