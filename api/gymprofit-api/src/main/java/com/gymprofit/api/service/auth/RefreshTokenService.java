package com.gymprofit.api.service.auth;

import com.gymprofit.api.entity.RefreshToken;
import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.exceptions.InvalidCredentialsException;
import com.gymprofit.api.repository.jpa.IRefreshTokenRepository;
import com.gymprofit.api.repository.jpa.IUsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.CannotAcquireLockException;
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
    private final IUsuarioRepository usuarioRepository;

    // Duración del refresh token en milisegundos, inyectada desde properties.
    @Value("${jwt.refresh.expiration}")
    private long refreshExpiration;

    // Crea y persiste un nuevo refresh token opaco (UUID) para el usuario dado, sellado con
    // su versión de sesión actual (GP-236). Es la vía de un login.
    @Transactional
    public RefreshToken crear(Usuario usuario) {
        return crear(usuario, usuario.getVersionSesion());
    }

    // Crea el refresh con la versión indicada: la rotación pasa la del refresh usado.
    private RefreshToken crear(Usuario usuario, int versionSesion) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUsuario(usuario);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setFechaExpiracion(LocalDateTime.now().plusNanos(refreshExpiration * 1_000_000));
        refreshToken.setRevocado(false);
        refreshToken.setVersionSesion(versionSesion);

        return refreshTokenRepository.save(refreshToken);
    }

    // Valida un refresh token: existe, no está revocado y no ha expirado.
    // Lanza 401 (InvalidCredentialsException) si no es válido → el cliente debe volver a autenticarse.
    @Transactional(readOnly = true)
    public RefreshToken validar(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidCredentialsException("error.refresh.invalido"));

        if (refreshToken.isRevocado() || refreshToken.getFechaExpiracion().isBefore(LocalDateTime.now())) {
            throw new InvalidCredentialsException("error.refresh.caducado");
        }

        // Sellado antes del último cierre de sesiones de su usuario (GP-236): ya no vale,
        // aunque nadie lo haya revocado.
        if (refreshToken.getVersionSesion() < refreshToken.getUsuario().getVersionSesion()) {
            throw new InvalidCredentialsException("error.refresh.caducado");
        }

        return refreshToken;
    }

    /**
     * Rota el token: revoca el actual y emite uno nuevo para el mismo usuario, con la
     * versión de sesión del actual (GP-236).
     *
     * <p>La revocación es condicional y en una sola sentencia: si entre validar y rotar
     * alguien lo ha revocado (un cierre de todas las sesiones, u otra rotación del mismo
     * token), no se emite nada y responde 401. MySQL lo cuenta como 0 filas; MariaDB con
     * {@code innodb_snapshot_isolation} (la de local y CI), como error 1020, que llega
     * como CannotAcquireLockException porque la fila cambió después de leerla. Los dos
     * casos son el mismo, y se contestan igual. La transacción se deshace entera al lanzar.
     *
     * @throws InvalidCredentialsException si el token ya no estaba vigente al revocarlo.
     */
    @Transactional
    public RefreshToken rotar(RefreshToken actual) {
        int revocados;
        try {
            revocados = refreshTokenRepository.revocarSiVigente(actual.getId());
        } catch (CannotAcquireLockException e) {
            throw new InvalidCredentialsException("error.refresh.caducado");
        }
        if (revocados == 0) {
            throw new InvalidCredentialsException("error.refresh.caducado");
        }

        return crear(actual.getUsuario(), actual.getVersionSesion());
    }

    // Revoca (si existe) el refresh token indicado. Idempotente: usado en el logout.
    @Transactional
    public void revocarPorToken(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(rt -> {
            rt.setRevocado(true);
            refreshTokenRepository.save(rt);
        });
    }

    /**
     * Cierra todas las sesiones del usuario (GP-236): sube su versión de sesión, con lo que
     * dejan de valer los access tokens y los refresh ya emitidos, y revoca sus refresh.
     *
     * <p>Es el único sitio que sube la versión. Lo usan cambiar y restablecer la
     * contraseña y desactivar la cuenta.
     */
    @Transactional
    public void cerrarTodasLasSesiones(Usuario usuario) {
        usuarioRepository.subirVersionSesion(usuario.getId());
        refreshTokenRepository.revocarTodosDeUsuario(usuario.getId());
    }
}
