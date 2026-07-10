package com.gymprofit.api.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymprofit.api.exceptions.Response;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

// ============================================================
// JwtEntryPoint — punto de entrada para peticiones no autenticadas (401)
// Se invoca cuando una petición llega sin credenciales válidas (sin token o
// token inválido) a un endpoint protegido. Escribe directamente una respuesta
// 401 en formato Response (mismo shape que el ControllerExceptionHandler),
// en lugar de propagar una excepción (que quedaría fuera del DispatcherServlet
// y acabaría como un 500).
// ============================================================
@Component
public class JwtEntryPoint implements AuthenticationEntryPoint {

    private final Logger logger = LoggerFactory.getLogger(JwtEntryPoint.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Registra el fallo de autenticación y escribe una respuesta 401 con cuerpo JSON.
    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        logger.error("Acceso no autorizado: {}", authException.getMessage());

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Response cuerpo = Response.generalError(HttpStatus.UNAUTHORIZED.value(), "No autenticado");
        objectMapper.writeValue(response.getWriter(), cuerpo);
    }
}
