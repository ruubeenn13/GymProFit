package com.gymprofit.api.config.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.AccessDeniedException;

// ============================================================
// JwtEntryPoint — punto de entrada para peticiones no autenticadas (401)
// Se invoca cuando una petición llega sin credenciales válidas a un
// endpoint protegido. Registra el error y relanza como AccessDeniedException
// para que el manejador global de excepciones genere la respuesta HTTP.
// ============================================================
@Component
public class JwtEntryPoint implements AuthenticationEntryPoint {

    private final Logger logger = LoggerFactory.getLogger(JwtEntryPoint.class);

    // Registra el fallo de autenticación y propaga la excepción de acceso denegado.
    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        logger.error("Acceso no autorizado: {}", authException.getMessage());

        throw new AccessDeniedException(authException.getMessage());
    }
}
