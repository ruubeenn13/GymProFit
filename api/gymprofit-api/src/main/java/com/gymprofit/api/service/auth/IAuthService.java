package com.gymprofit.api.service.auth;

import com.gymprofit.api.dto.auth.LoginDTO;
import com.gymprofit.api.dto.auth.RegisterDTO;
import com.gymprofit.api.dto.auth.TokenDTO;

// ============================================================
// IAuthService — contrato del servicio de autenticación
// Define las operaciones de login, registro público y acceso como invitado.
// Implementada por AuthService, responsable de emitir tokens JWT.
// ============================================================
public interface IAuthService {

    // Autentica al usuario y devuelve un token JWT junto con sus roles.
    TokenDTO login(LoginDTO loginDTO);

    // Registra un nuevo usuario público (siempre con rol USER).
    void register(RegisterDTO registerDTO);

    // Genera un token JWT para el usuario invitado predefinido "guest".
    TokenDTO loginAsGuest();

    // Renueva el access token a partir de un refresh token válido (con rotación).
    TokenDTO refresh(String refreshTokenValue);

    // Cierra sesión revocando el refresh token indicado.
    void logout(String refreshTokenValue);
}
