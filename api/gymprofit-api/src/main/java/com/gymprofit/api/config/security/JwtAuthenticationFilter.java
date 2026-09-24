package com.gymprofit.api.config.security;

import com.gymprofit.api.exceptions.CuentaDesactivadaException;
import com.gymprofit.api.service.usuario.IUsuarioService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;

// ============================================================
// JwtAuthenticationFilter — filtro de autenticación JWT por petición
// Se ejecuta una vez por cada petición HTTP: extrae el token del header
// Authorization, valida el usuario y, si es correcto, establece la
// autenticación en el SecurityContext para que Spring Security la use.
// ============================================================
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final HandlerExceptionResolver handlerExceptionResolver;
    private final JwtTokenProvider jwtTokenProvider;
    private final IUsuarioService usuarioService;


    // Intercepta la petición: valida el JWT (si existe) y autentica al usuario en el contexto de seguridad.
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = getTokenFromRequest(request);

            if (token == null) {
                filterChain.doFilter(request, response);
                return;
            }

            String username = jwtTokenProvider.extractUsername(token);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = usuarioService.loadUserByUsername(username);

                if (jwtTokenProvider.isTokenValid(token, userDetails)) {
                    // Un token bien firmado y sin caducar no basta: la cuenta puede haberse
                    // desactivado después de emitirlo (GP-083). Se corta aquí con 401 y no
                    // dejando la petición sin autenticar, porque entonces las rutas públicas
                    // seguirían respondiendo a ese token como si nada.
                    if (!userDetails.isEnabled()) {
                        throw new CuentaDesactivadaException();
                    }

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }

            filterChain.doFilter(request, response);
        } catch (Exception e) {
            handlerExceptionResolver.resolveException(request, response, null, e);
        }
    }

    // Obtiene el token JWT del header Authorization, quitando el prefijo "Bearer ".
    private String getTokenFromRequest(HttpServletRequest request) {
        final String authHeader = request.getHeader(JwtTokenProvider.TOKEN_HEADER);

        if (StringUtils.hasText(authHeader) && authHeader.startsWith(JwtTokenProvider.TOKEN_PREFIX)) {
            return authHeader.substring(JwtTokenProvider.TOKEN_PREFIX.length());
        }

        return null;
    }
}
