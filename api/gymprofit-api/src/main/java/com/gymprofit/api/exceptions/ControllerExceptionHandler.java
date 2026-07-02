package com.gymprofit.api.exceptions;

import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@ControllerAdvice
public class ControllerExceptionHandler {

    private final Logger logger = LoggerFactory.getLogger(ControllerExceptionHandler.class);

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

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(NotFoundEntityException.class)
    public ResponseEntity<Response> handleNotFoundEntityException(NotFoundEntityException ex) {
        return new ResponseEntity<>(
                Response.generalError(HttpStatus.NOT_FOUND.value(), ex.getMessage()),
                HttpStatus.NOT_FOUND
        );
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(DuplicateEntityException.class)
    public ResponseEntity<Response> handleDuplicateEntityException(DuplicateEntityException ex) {
        return new ResponseEntity<>(
                Response.generalError(HttpStatus.BAD_REQUEST.value(), ex.getMessage()),
                HttpStatus.BAD_REQUEST
        );
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(InvalidDataException.class)
    public ResponseEntity<Response> handleInvalidDataException(InvalidDataException ex) {
        return new ResponseEntity<>(
                Response.generalError(HttpStatus.BAD_REQUEST.value(), ex.getMessage()),
                HttpStatus.BAD_REQUEST
        );
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Response> handleInvalidCredentialsException(InvalidCredentialsException ex) {
        return new ResponseEntity<>(
                Response.generalError(HttpStatus.UNAUTHORIZED.value(), ex.getMessage()),
                HttpStatus.UNAUTHORIZED
        );
    }

    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Response> handleUnauthorizedException(UnauthorizedException ex) {
        return new ResponseEntity<>(
                Response.generalError(HttpStatus.FORBIDDEN.value(), ex.getMessage()),
                HttpStatus.FORBIDDEN
        );
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(SesionNotCompletedException.class)
    public ResponseEntity<Response> handleSesionNotCompletedException(SesionNotCompletedException ex) {
        return new ResponseEntity<>(
                Response.generalError(HttpStatus.BAD_REQUEST.value(), ex.getMessage()),
                HttpStatus.BAD_REQUEST
        );
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ObjetivoAlreadyCompletedException.class)
    public ResponseEntity<Response> handleObjetivoAlreadyCompletedException(ObjetivoAlreadyCompletedException ex) {
        return new ResponseEntity<>(
                Response.generalError(HttpStatus.BAD_REQUEST.value(), ex.getMessage()),
                HttpStatus.BAD_REQUEST
        );
    }

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

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Response> handleIllegalArgumentException(IllegalArgumentException ex) {
        // La causa (SQL/Hibernate) se registra en el log, NUNCA se devuelve al cliente.
        logger.error(ex.getMessage(), ex);

        return new ResponseEntity<>(
                Response.generalError(HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getMessage()),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(JwtException.class)
    public ResponseEntity<Response> handleJwtException(JwtException ex) {
        logger.warn(ex.getMessage());
        return new ResponseEntity<>(
                Response.generalError(HttpStatus.UNAUTHORIZED.value(), "Token inválido o expirado"),
                HttpStatus.UNAUTHORIZED
        );
    }

    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Response> handleAccessDeniedException(AccessDeniedException ex) {
        logger.error(ex.getMessage(), ex);
        return new ResponseEntity<>(
                Response.generalError(HttpStatus.FORBIDDEN.value(), "Acceso denegado"),
                HttpStatus.FORBIDDEN
        );
    }

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