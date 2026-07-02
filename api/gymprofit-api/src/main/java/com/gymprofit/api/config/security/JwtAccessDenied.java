package com.gymprofit.api.config.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

// ============================================================
// JwtAccessDenied — manejador de accesos denegados (403) en Spring Security
// Se invoca cuando un usuario autenticado no tiene permisos suficientes
// para acceder a un recurso. Registra el error y relanza la excepción para
// que el ControllerExceptionHandler global la traduzca a una respuesta HTTP.
// ============================================================
@Component
public class JwtAccessDenied implements AccessDeniedHandler {

    private final Logger logger = LoggerFactory.getLogger(JwtAccessDenied.class);


    // Registra el intento de acceso denegado y propaga la excepción de seguridad.
    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {
        logger.error("Acceso denegado: {}", accessDeniedException.getMessage());

        throw new AccessDeniedException(accessDeniedException.getMessage());
    }
}
