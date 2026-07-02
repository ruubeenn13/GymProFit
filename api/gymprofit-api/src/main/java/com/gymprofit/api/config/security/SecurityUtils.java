package com.gymprofit.api.config.security;

import com.gymprofit.api.entity.Usuario;
import com.gymprofit.api.enums.RoleType;
import com.gymprofit.api.exceptions.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

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
            throw new UnauthorizedException("No hay un usuario autenticado en el contexto de seguridad");
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
     * {@code true} si el usuario autenticado tiene rol ADMIN.
     */
    public boolean isAdmin() {
        return getCurrentUser().getRoles().stream()
                .anyMatch(role -> role.getNombre() == RoleType.ADMIN);
    }

    /**
     * Verifica que el usuario autenticado es el propietario del recurso, o ADMIN.
     *
     * @param ownerId id del usuario propietario del recurso.
     * @throws UnauthorizedException (→ 403) si no es propietario ni ADMIN.
     */
    public void checkOwnership(Integer ownerId) {
        Usuario current = getCurrentUser();

        boolean admin = current.getRoles().stream()
                .anyMatch(role -> role.getNombre() == RoleType.ADMIN);

        if (!admin && !current.getId().equals(ownerId)) {
            throw new UnauthorizedException("No tienes permiso para acceder a este recurso");
        }
    }

    /**
     * Exige que el usuario autenticado sea ADMIN (p. ej. listados globales {@code findAll}).
     *
     * @throws UnauthorizedException (→ 403) si no es ADMIN.
     */
    public void requireAdmin() {
        if (!isAdmin()) {
            throw new UnauthorizedException("Esta operación requiere rol ADMIN");
        }
    }
}
