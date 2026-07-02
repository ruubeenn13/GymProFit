package com.gymprofit.api.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

// ============================================================
// TokenDTO — respuesta de autenticación con los tokens emitidos
// Se devuelve tras un login/refresh exitoso e incluye el access token JWT
// (de vida corta), el refresh token opaco (para renovar sin re-login), el
// nombre de usuario y los roles asignados para uso en el cliente.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TokenDTO implements Serializable {
    // Access token JWT de vida corta que autoriza las peticiones.
    private String token;
    // Refresh token opaco de larga duración para renovar el access token.
    private String refreshToken;
    private String username;
    private List<String> roles;
}
