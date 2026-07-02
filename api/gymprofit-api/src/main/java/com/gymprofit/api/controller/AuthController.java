package com.gymprofit.api.controller;

import com.gymprofit.api.dto.auth.LoginDTO;
import com.gymprofit.api.dto.auth.RefreshRequestDTO;
import com.gymprofit.api.dto.auth.RegisterDTO;
import com.gymprofit.api.dto.auth.TokenDTO;
import com.gymprofit.api.exceptions.Response;
import com.gymprofit.api.service.auth.IAuthService;
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
}
