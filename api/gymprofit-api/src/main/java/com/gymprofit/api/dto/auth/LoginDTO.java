package com.gymprofit.api.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

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
    // El nombre de usuario o el correo (GP-103). El campo conserva el nombre para que la
    // 1.4.0, que manda «username», siga entrando igual.
    @NotBlank
    private String username;

    // Contraseña en texto plano (se valida contra el hash almacenado)
    @NotBlank
    @ToString.Exclude
    private String password;
}
