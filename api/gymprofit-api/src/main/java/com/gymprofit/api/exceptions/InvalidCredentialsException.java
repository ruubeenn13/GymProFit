package com.gymprofit.api.exceptions;

// ============================================================
// InvalidCredentialsException — excepción de credenciales incorrectas
// Se lanza durante el login cuando el usuario/contraseña no coinciden.
// Capturada por ControllerExceptionHandler para devolver 401.
// ============================================================
public class InvalidCredentialsException extends RuntimeException {

  // Constructor con mensaje personalizado.
  public InvalidCredentialsException(String message) {
    super(message);
  }

  // Constructor con mensaje por defecto genérico.
  public InvalidCredentialsException() {
    super("Credenciales inválidas");
  }
}