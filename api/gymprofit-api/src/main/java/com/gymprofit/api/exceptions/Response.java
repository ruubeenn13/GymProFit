package com.gymprofit.api.exceptions;

import lombok.Data;
import org.springframework.http.HttpStatus;

@Data
public class Response {

    private int code;
    private String message;
    private String cause;

    private Response(int errorCode, String errorMessage) {
        this.code = errorCode;
        this.message = errorMessage;
    }

    private Response(int errorCode, String errorMessage, String cause) {
        this.code = errorCode;
        this.message = errorMessage;
        this.cause = cause;
    }

    public static Response validationError(String errorMessage) {
        return new Response(HttpStatus.BAD_REQUEST.value(), errorMessage);
    }

    public static Response generalError(int code, String mensaje) {
        return new Response(code, mensaje);
    }

    public static Response generalError(int code, String mensaje, String cause) {
        return new Response(code, mensaje, cause);
    }

    public static Response ok(String message) {
        return new Response(HttpStatus.OK.value(), message);
    }
}
