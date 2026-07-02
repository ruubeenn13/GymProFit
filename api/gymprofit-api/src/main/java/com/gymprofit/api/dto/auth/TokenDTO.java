package com.gymprofit.api.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

// ============================================================
// TokenDTO — respuesta de autenticación con el token JWT emitido
// Se devuelve tras un login/registro exitoso e incluye el token,
// el nombre de usuario y los roles asignados para uso en el cliente.
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TokenDTO implements Serializable {
    private String token;
    private String username;
    private List<String> roles;
}
