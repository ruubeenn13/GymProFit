package com.gymprofit.api.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

// ============================================================
// RefreshRequestDTO — cuerpo de las peticiones de refresh/logout
// Transporta el refresh token opaco que el cliente ha guardado, tanto para
// renovar el access token (POST /auth/refresh) como para cerrar sesión
// revocándolo (POST /auth/logout).
// ============================================================
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RefreshRequestDTO implements Serializable {

    // Refresh token opaco emitido previamente en el login.
    @NotBlank(message = "El refresh token es obligatorio")
    private String refreshToken;
}
