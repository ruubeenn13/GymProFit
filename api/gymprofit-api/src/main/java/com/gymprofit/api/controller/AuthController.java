package com.gymprofit.api.controller;

import com.gymprofit.api.dto.auth.ChangePasswordDTO;
import com.gymprofit.api.dto.auth.ForgotPasswordDTO;
import com.gymprofit.api.dto.auth.LoginDTO;
import com.gymprofit.api.dto.auth.RefreshRequestDTO;
import com.gymprofit.api.dto.auth.RegisterDTO;
import com.gymprofit.api.dto.auth.ResetPasswordDTO;
import com.gymprofit.api.dto.auth.TokenDTO;
import com.gymprofit.api.exceptions.Response;
import com.gymprofit.api.service.auth.IAuthService;
import com.gymprofit.api.service.auth.IPasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

// ============================================================
// AuthController — endpoints públicos de autenticación
// Expone login (genera JWT), registro de nuevos usuarios (rol USER
// por defecto) y acceso como invitado (rol GUEST) sin necesidad de
// credenciales, para la app GymProFit.
// ============================================================
@RestController
@RequestMapping("/auth")
@AllArgsConstructor
@Tag(name = "Auth Controlador", description = "Gestión de autenticación y registro de usuarios")
public class AuthController {

    private final IAuthService authService;
    private final IPasswordResetService passwordResetService;

    @Operation(summary = "Inicia sesión y devuelve un token JWT")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login correcto, token generado",
                    content = @Content(schema = @Schema(implementation = TokenDTO.class))),
            @ApiResponse(responseCode = "401", description = "Credenciales incorrectas",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<TokenDTO> login(@Valid @RequestBody LoginDTO loginDTO) {
        TokenDTO token = authService.login(loginDTO);

        return ResponseEntity.ok(token);
    }

    @Operation(summary = "Registra un nuevo usuario con el rol USER por defecto")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Usuario registrado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o usuario ya existente",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterDTO registerDTO) {
        Map<String, Object> respuesta = new HashMap<>();

        authService.register(registerDTO);

        respuesta.put("mensaje", "Usuario registrado correctamente");

        return new ResponseEntity<>(respuesta, HttpStatus.CREATED);
    }

    @Operation(summary = "Accede como invitado sin necesidad de registro")
    @ApiResponse(responseCode = "200", description = "Token JWT con rol GUEST",
            content = @Content(schema = @Schema(implementation = TokenDTO.class)))
    @PostMapping("/guest")
    public ResponseEntity<TokenDTO> guest() {
        return ResponseEntity.ok(authService.loginAsGuest());
    }

    @Operation(summary = "Renueva el access token a partir de un refresh token válido")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Nuevo access token y refresh token (rotado)",
                    content = @Content(schema = @Schema(implementation = TokenDTO.class))),
            @ApiResponse(responseCode = "401", description = "Refresh token inválido, expirado o revocado",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @PostMapping("/refresh")
    public ResponseEntity<TokenDTO> refresh(@Valid @RequestBody RefreshRequestDTO refreshRequestDTO) {
        return ResponseEntity.ok(authService.refresh(refreshRequestDTO.getRefreshToken()));
    }

    @Operation(summary = "Cierra sesión revocando el refresh token")
    @ApiResponse(responseCode = "200", description = "Sesión cerrada correctamente")
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(@Valid @RequestBody RefreshRequestDTO refreshRequestDTO) {
        authService.logout(refreshRequestDTO.getRefreshToken());

        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("mensaje", "Sesión cerrada correctamente");

        return ResponseEntity.ok(respuesta);
    }

    @Operation(summary = "Cambia la contraseña del usuario autenticado",
            description = "Requiere estar autenticado (Bearer). Verifica la contraseña actual, guarda la " +
                    "nueva y revoca todas las sesiones abiertas del usuario, que deberá volver a iniciar sesión.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Contraseña cambiada correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos (nueva igual a la actual, formato)",
                    content = @Content(schema = @Schema(implementation = Response.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado o contraseña actual incorrecta",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @PostMapping("/change-password")
    public ResponseEntity<Map<String, Object>> changePassword(@Valid @RequestBody ChangePasswordDTO changePasswordDTO,
                                                              Authentication authentication) {
        // El username se toma del token autenticado, nunca del cuerpo: un usuario solo cambia SU propia contraseña.
        authService.changePassword(authentication.getName(), changePasswordDTO);

        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("mensaje", "Contraseña cambiada correctamente");

        return ResponseEntity.ok(respuesta);
    }

    @Operation(summary = "Pide un código para recuperar la contraseña olvidada",
            description = "Público. Admite el nombre de usuario o el correo. Si la cuenta existe y está " +
                    "activa, envía a su correo un código de 6 dígitos que caduca en 15 minutos y sirve una " +
                    "sola vez. La respuesta es SIEMPRE la misma, exista la cuenta o no: así este endpoint no " +
                    "sirve para averiguar quién está registrado.")
    @ApiResponse(responseCode = "200", description = "Solicitud recibida (haya cuenta o no)")
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, Object>> forgotPassword(@Valid @RequestBody ForgotPasswordDTO forgotPasswordDTO) {
        passwordResetService.solicitarCodigo(forgotPasswordDTO.getIdentificador());

        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("mensaje", "Si la cuenta existe, se ha enviado un código a su correo");

        return ResponseEntity.ok(respuesta);
    }

    @Operation(summary = "Restablece la contraseña con el código recibido por correo",
            description = "Público. Canjea el código de 6 dígitos por una contraseña nueva, sujeta a la " +
                    "misma política que el registro, y revoca todas las sesiones abiertas de la cuenta.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Contraseña restablecida correctamente"),
            @ApiResponse(responseCode = "400", description = "Código inválido o caducado, o contraseña que " +
                    "no cumple la política",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(@Valid @RequestBody ResetPasswordDTO resetPasswordDTO) {
        passwordResetService.restablecer(resetPasswordDTO);

        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("mensaje", "Contraseña restablecida correctamente");

        return ResponseEntity.ok(respuesta);
    }
}
