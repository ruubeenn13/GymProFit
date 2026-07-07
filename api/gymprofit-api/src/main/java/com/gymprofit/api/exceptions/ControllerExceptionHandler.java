package com.gymprofit.api.exceptions;

import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

// ============================================================
// ControllerExceptionHandler — manejador global de excepciones de la API.
// Centraliza la traducción de cada excepción custom (y algunas de
// Spring/JWT) a una respuesta HTTP homogénea (Response) con el código
// de estado correcto, evitando filtrar detalles internos al cliente.
// ============================================================
@ControllerAdvice
public class ControllerExceptionHandler {

    // Logger para registrar la causa real de los errores internos.
    private final Logger logger = LoggerFactory.getLogger(ControllerExceptionHandler.class);

    // Errores de validación de @Valid en DTOs: junta todos los mensajes de campo en un solo string.
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Response> handleValidationArgumentsErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        String mapAsString = errors.keySet().stream()
                .map(key -> key + " = " + errors.get(key))
                .collect(Collectors.joining(", ", "{ ", " }"));

        return new ResponseEntity<>(
                Response.validationError(mapAsString),
                HttpStatus.BAD_REQUEST
        );
    }

    // Cuerpo JSON ausente, mal formado o ilegible (tipo incompatible en un campo, etc.) → 400 (no 500).
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Response> handleNotReadable(HttpMessageNotReadableException ex) {
        logger.warn("Cuerpo de la petición ilegible: {}", ex.getMessage());
        return new ResponseEntity<>(
                Response.validationError("Cuerpo de la petición ausente o mal formado"),
                HttpStatus.BAD_REQUEST
        );
    }

    // Tipo inválido en un path variable o parámetro (p. ej. GET /comidas/abc con id numérico) → 400 (no 500).
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Response> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        logger.warn("Parámetro con tipo inválido: {}", ex.getMessage());
        return new ResponseEntity<>(
                Response.validationError("Parámetro inválido: " + ex.getName()),
                HttpStatus.BAD_REQUEST
        );
    }

    // Falta un parámetro de query obligatorio (@RequestParam sin defaultValue) → 400 (no 500).
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Response> handleMissingParam(MissingServletRequestParameterException ex) {
        logger.warn("Falta parámetro obligatorio: {}", ex.getMessage());
        return new ResponseEntity<>(
                Response.validationError("Falta el parámetro obligatorio: " + ex.getParameterName()),
                HttpStatus.BAD_REQUEST
        );
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(CreateEntityException.class)
    public ResponseEntity<Response> handleCreateEntityException(CreateEntityException ex) {
        // La causa (SQL/Hibernate) se registra en el log, NUNCA se devuelve al cliente.
        logger.error(ex.getMessage(), ex);

        return new ResponseEntity<>(
                Response.generalError(HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getMessage()),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    // Fallo al actualizar una entidad.
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(UpdateEntityException.class)
    public ResponseEntity<Response> handleUpdateEntityException(UpdateEntityException ex) {
        // La causa (SQL/Hibernate) se registra en el log, NUNCA se devuelve al cliente.
        logger.error(ex.getMessage(), ex);

        return new ResponseEntity<>(
                Response.generalError(HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getMessage()),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    // Fallo al eliminar una entidad.
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(DeleteEntityException.class)
    public ResponseEntity<Response> handleDeleteEntityException(DeleteEntityException ex) {
        // La causa (SQL/Hibernate) se registra en el log, NUNCA se devuelve al cliente.
        logger.error(ex.getMessage(), ex);

        return new ResponseEntity<>(
                Response.generalError(HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getMessage()),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    // Entidad no encontrada por id: 404, mensaje directo al cliente.
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(NotFoundEntityException.class)
    public ResponseEntity<Response> handleNotFoundEntityException(NotFoundEntityException ex) {
        return new ResponseEntity<>(
                Response.generalError(HttpStatus.NOT_FOUND.value(), ex.getMessage()),
                HttpStatus.NOT_FOUND
        );
    }

    // Registro duplicado (violación de unicidad): 400.
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(DuplicateEntityException.class)
    public ResponseEntity<Response> handleDuplicateEntityException(DuplicateEntityException ex) {
        return new ResponseEntity<>(
                Response.generalError(HttpStatus.BAD_REQUEST.value(), ex.getMessage()),
                HttpStatus.BAD_REQUEST
        );
    }

    // Datos de entrada inválidos a nivel de negocio: 400.
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(InvalidDataException.class)
    public ResponseEntity<Response> handleInvalidDataException(InvalidDataException ex) {
        return new ResponseEntity<>(
                Response.generalError(HttpStatus.BAD_REQUEST.value(), ex.getMessage()),
                HttpStatus.BAD_REQUEST
        );
    }

    // Fallo de una API externa (Open Food Facts / wger): 502 Bad Gateway.
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<Response> handleExternalServiceException(ExternalServiceException ex) {
        return new ResponseEntity<>(
                Response.generalError(HttpStatus.BAD_GATEWAY.value(), ex.getMessage()),
                HttpStatus.BAD_GATEWAY
        );
    }

    // Credenciales inválidas (lógica propia de negocio): 401.
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Response> handleInvalidCredentialsException(InvalidCredentialsException ex) {
        return new ResponseEntity<>(
                Response.generalError(HttpStatus.UNAUTHORIZED.value(), ex.getMessage()),
                HttpStatus.UNAUTHORIZED
        );
    }

    // Acceso no autorizado a un recurso propio del dominio: 403.
    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Response> handleUnauthorizedException(UnauthorizedException ex) {
        return new ResponseEntity<>(
                Response.generalError(HttpStatus.FORBIDDEN.value(), ex.getMessage()),
                HttpStatus.FORBIDDEN
        );
    }

    // Operación sobre una sesión de entrenamiento no completada: 400.
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(SesionNotCompletedException.class)
    public ResponseEntity<Response> handleSesionNotCompletedException(SesionNotCompletedException ex) {
        return new ResponseEntity<>(
                Response.generalError(HttpStatus.BAD_REQUEST.value(), ex.getMessage()),
                HttpStatus.BAD_REQUEST
        );
    }

    // Objetivo personal ya marcado como completado: 400.
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ObjetivoAlreadyCompletedException.class)
    public ResponseEntity<Response> handleObjetivoAlreadyCompletedException(ObjetivoAlreadyCompletedException ex) {
        return new ResponseEntity<>(
                Response.generalError(HttpStatus.BAD_REQUEST.value(), ex.getMessage()),
                HttpStatus.BAD_REQUEST
        );
    }

    // Error genérico de negocio no cubierto por las excepciones anteriores.
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(ErrorGenericoException.class)
    public ResponseEntity<Response> handleErrorGenericoException(ErrorGenericoException ex) {
        // La causa (SQL/Hibernate) se registra en el log, NUNCA se devuelve al cliente.
        logger.error(ex.getMessage(), ex);

        return new ResponseEntity<>(
                Response.generalError(HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getMessage()),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    // Argumento ilegal (p. ej. un valor de enum inválido en un filtro): es un error del
    // CLIENTE, no del servidor → 400 Bad Request (antes devolvía 500 erróneamente).
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Response> handleIllegalArgumentException(IllegalArgumentException ex) {
        logger.warn("Argumento inválido en la petición: {}", ex.getMessage());

        return new ResponseEntity<>(
                Response.generalError(HttpStatus.BAD_REQUEST.value(), "Parámetro con valor inválido"),
                HttpStatus.BAD_REQUEST
        );
    }

    // Token JWT inválido, malformado o expirado: 401 con mensaje genérico.
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(JwtException.class)
    public ResponseEntity<Response> handleJwtException(JwtException ex) {
        logger.warn(ex.getMessage());
        return new ResponseEntity<>(
                Response.generalError(HttpStatus.UNAUTHORIZED.value(), "Token inválido o expirado"),
                HttpStatus.UNAUTHORIZED
        );
    }

    // Acceso denegado por Spring Security (ej. falta de rol/permiso): 403.
    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Response> handleAccessDeniedException(AccessDeniedException ex) {
        logger.error(ex.getMessage(), ex);
        return new ResponseEntity<>(
                Response.generalError(HttpStatus.FORBIDDEN.value(), "Acceso denegado"),
                HttpStatus.FORBIDDEN
        );
    }

    // Fallback genérico: cualquier excepción no controlada explícitamente arriba.
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Response> handleException(Exception ex) {
        logger.error(ex.getMessage(), ex);

        return new ResponseEntity<>(
                Response.generalError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Error interno del servidor"),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    /**
     * Credenciales inválidas en login (BadCredentialsException y cualquier AuthenticationException
     * que Spring Security lance en authenticationManager.authenticate). Devuelve 401 genérico
     * en vez del 500 que caería en handleException, y sin revelar si el usuario existe.
     */
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Response> handleAuthenticationException(AuthenticationException ex) {
        logger.warn("Fallo de autenticación: {}", ex.getMessage());
        return new ResponseEntity<>(
                Response.generalError(HttpStatus.UNAUTHORIZED.value(), "Credenciales inválidas"),
                HttpStatus.UNAUTHORIZED
        );
    }
}