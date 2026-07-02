package com.gymprofit.api.exceptions;

import lombok.Data;
import org.springframework.http.HttpStatus;

// ============================================================
// Response — DTO genérico de respuesta de error/éxito de la API
// Estructura uniforme (código, mensaje, causa) usada por
// ControllerExceptionHandler para serializar las respuestas de error.
// ============================================================
@Data
public class Response {

    // Código de estado HTTP asociado a la respuesta.
    private int code;
    // Mensaje descriptivo principal.
    private String message;
    // Causa opcional adicional del error.
    private String cause;

    // Constructor privado con código y mensaje.
    private Response(int errorCode, String errorMessage) {
        this.code = errorCode;
        this.message = errorMessage;
    }

    // Constructor privado con código, mensaje y causa.
    private Response(int errorCode, String errorMessage, String cause) {
        this.code = errorCode;
        this.message = errorMessage;
        this.cause = cause;
    }

    // Crea una respuesta de error de validación (400 Bad Request).
    public static Response validationError(String errorMessage) {
        return new Response(HttpStatus.BAD_REQUEST.value(), errorMessage);
    }

    // Crea una respuesta de error genérico con código y mensaje personalizados.
    public static Response generalError(int code, String mensaje) {
        return new Response(code, mensaje);
    }

    // Crea una respuesta de error genérico con código, mensaje y causa.
    public static Response generalError(int code, String mensaje, String cause) {
        return new Response(code, mensaje, cause);
    }

    // Crea una respuesta de éxito (200 OK) con mensaje.
    public static Response ok(String message) {
        return new Response(HttpStatus.OK.value(), message);
    }
}
