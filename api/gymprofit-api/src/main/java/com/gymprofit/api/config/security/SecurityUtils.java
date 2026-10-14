package com.gymprofit.api.config.security;

import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.enums.RoleType;
import com.gymprofit.api.exceptions.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Utilidades de autorización a nivel de objeto (ownership).
 * <p>
 * Centraliza la obtención del usuario autenticado desde el {@link SecurityContextHolder}
 * y las comprobaciones de propiedad de recursos. El {@code JwtAuthenticationFilter} coloca
 * la entidad {@link Usuario} como principal, por lo que el id y los roles del usuario
 * autenticado se obtienen del token, nunca del cliente.
 * <p>
 * Regla general de la API: el {@code usuarioId} de un recurso se deriva SIEMPRE del token.
 * Un {@code USER} solo puede acceder a sus propios recursos; un {@code ADMIN} puede a todos.
 */
// ============================================================
// SecurityUtils — utilidades de autorización a nivel de objeto (ownership)
// Centraliza el acceso al usuario autenticado y las comprobaciones de
// propiedad de recursos, evitando confiar en datos enviados por el cliente.
// ============================================================
@Component
public class SecurityUtils {

    /**
     * Devuelve la entidad {@link Usuario} autenticada.
     *
     * @throws UnauthorizedException si no hay autenticación válida en el contexto.
     */
    public Usuario getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof Usuario)) {
            throw new UnauthorizedException("error.acceso.sinUsuario");
        }

        return (Usuario) authentication.getPrincipal();
    }

    /**
     * Id del usuario autenticado (extraído del token).
     */
    public Integer getCurrentUserId() {
        return getCurrentUser().getId();
    }

    /**
     * Id del usuario autenticado, o vacío si la petición llega sin token: para las rutas
     * públicas que responden distinto con cuenta y sin ella (GP-103).
     */
    public Optional<Integer> getCurrentUserIdSiLoHay() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getPrincipal() instanceof Usuario u
                ? Optional.of(u.getId()) : Optional.empty();
    }

    /**
     * {@code true} si el usuario autenticado tiene rol ADMIN.
     */
    public boolean isAdmin() {
        return getCurrentUser().getRoles().stream()
                .anyMatch(role -> role.getNombre() == RoleType.ADMIN);
    }

    /**
     * Verifica que el usuario autenticado es el dueño del recurso, sin excepción para ADMIN.
     * <p>
     * Lo que guarda un usuario —su nutrición (DEC-042), sus entrenamientos, sus medidas, sus
     * objetivos, sus avisos, sus rutinas y su cuenta— es solo suyo, también para un ADMIN
     * (GP-198). Un id de otro da el mismo 403 que a un USER (DEC-014). Lo que es de
     * administración —el catálogo, las rutinas sin dueño, la gestión de cuentas de /admin—
     * se comprueba aparte, con {@link #requireAdmin()}.
     *
     * @param ownerId id del usuario propietario del recurso.
     * @throws UnauthorizedException (→ 403) si quien llama no es el dueño.
     */
    public void checkDueno(Integer ownerId) {
        if (!getCurrentUserId().equals(ownerId)) {
            throw new UnauthorizedException("error.acceso.recurso");
        }
    }

    /**
     * {@link #checkDueno(Integer)} solo cuando el recurso tiene dueño: un {@code ownerId} nulo
     * es catálogo, de todos, y ahí no hay nada que comprobar (DEC-027).
     *
     * @param ownerId id del propietario, o {@code null} si el recurso es de catálogo.
     * @throws UnauthorizedException (→ 403) si el recurso tiene dueño y no es quien llama.
     */
    public void checkDuenoSiLoTiene(Integer ownerId) {
        if (ownerId != null) {
            checkDueno(ownerId);
        }
    }

    /**
     * Exige que el usuario autenticado sea ADMIN (p. ej. listados globales {@code findAll}).
     *
     * @throws UnauthorizedException (→ 403) si no es ADMIN.
     */
    public void requireAdmin() {
        if (!isAdmin()) {
            throw new UnauthorizedException("error.acceso.soloAdmin");
        }
    }
}
