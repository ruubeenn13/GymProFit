package com.gymprofit.api.exceptions;

public class ErrorGenericoException extends RuntimeException {

    public ErrorGenericoException(String message) {
        super(message);
    }

    public ErrorGenericoException(String message, Throwable cause) {
        super(message, cause);
    }
}