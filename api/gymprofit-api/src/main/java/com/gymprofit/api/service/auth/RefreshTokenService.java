package com.gymprofit.api.service.auth;

import com.gymprofit.api.entity.RefreshToken;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.exceptions.InvalidCredentialsException;
import com.gymprofit.api.repository.jpa.IRefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

// ============================================================
// RefreshTokenService — ciclo de vida de los refresh tokens
// Crea refresh tokens opacos al hacer login, los valida al renovar la sesión,
// los rota (revoca el usado y emite uno nuevo) en cada refresh y los revoca al
// cerrar sesión. Es la pieza que hace la sesión renovable y revocable.
// ============================================================
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final IRefreshTokenRepository refreshTokenRepository;

    // Duración del refresh token en milisegundos, inyectada desde properties.
    @Value("${jwt.refresh.expiration}")
    private long refreshExpiration;

    // Crea y persiste un nuevo refresh token opaco (UUID) para el usuario dado.
    @Transactional
    public RefreshToken crear(Usuario usuario) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUsuario(usuario);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setFechaExpiracion(LocalDateTime.now().plusNanos(refreshExpiration * 1_000_000));
        refreshToken.setRevocado(false);

        return refreshTokenRepository.save(refreshToken);
    }

    // Valida un refresh token: existe, no está revocado y no ha expirado.
    // Lanza 401 (InvalidCredentialsException) si no es válido → el cliente debe volver a autenticarse.
    @Transactional(readOnly = true)
    public RefreshToken validar(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidCredentialsException("Refresh token inválido"));

        if (refreshToken.isRevocado() || refreshToken.getFechaExpiracion().isBefore(LocalDateTime.now())) {
            throw new InvalidCredentialsException("Refresh token expirado o revocado");
        }

        return refreshToken;
    }

    // Rota el token: revoca el actual y emite uno nuevo para el mismo usuario.
    @Transactional
    public RefreshToken rotar(RefreshToken actual) {
        actual.setRevocado(true);
        refreshTokenRepository.save(actual);

        return crear(actual.getUsuario());
    }

    // Revoca (si existe) el refresh token indicado. Idempotente: usado en el logout.
    @Transactional
    public void revocarPorToken(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(rt -> {
            rt.setRevocado(true);
            refreshTokenRepository.save(rt);
        });
    }

    // Revoca de golpe todos los refresh tokens activos del usuario. Se usa al cambiar
    // la contraseña para invalidar cualquier sesión abierta y forzar un nuevo login.
    @Transactional
    public void revocarTodosDeUsuario(Usuario usuario) {
        refreshTokenRepository.revocarTodosDeUsuario(usuario.getId());
    }
}
