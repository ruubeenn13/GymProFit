package com.gymprofit.api.controller;

import com.gymprofit.api.dto.entity.devicetoken.DeviceTokenCreateDTO;
import com.gymprofit.api.exceptions.Response;
import com.gymprofit.api.service.devicetoken.IDeviceTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

// ============================================================
// DeviceTokenController — registro/baja del token FCM del dispositivo (push).
// El usuario propietario se toma del JWT (no del body). Rutas bajo /notificaciones,
// ya protegidas para USER/ADMIN en SecurityConfig (un invitado no registra push).
// ============================================================
@RestController
@RequestMapping("")
@AllArgsConstructor
@Tag(name = "Device Token Controlador", description = "Registro de tokens FCM para notificaciones push")
public class DeviceTokenController {

    private final IDeviceTokenService deviceTokenService;

    @Operation(summary = "Registra (o actualiza) el token FCM del dispositivo del usuario autenticado")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token registrado correctamente"),
            @ApiResponse(responseCode = "400", description = "Token ausente o inválido",
                    content = @Content(schema = @Schema(implementation = Response.class))),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    // Registra el token FCM del dispositivo para el usuario autenticado.
    @PostMapping("/notificaciones/token")
    public ResponseEntity<Map<String, Object>> registrar(@Valid @RequestBody DeviceTokenCreateDTO dto) {
        deviceTokenService.registrar(dto);

        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("mensaje", "Token registrado correctamente");
        return ResponseEntity.ok(respuesta);
    }

    @Operation(summary = "Da de baja el token FCM del dispositivo (logout)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token eliminado correctamente"),
            @ApiResponse(responseCode = "403", description = "El token no pertenece al usuario autenticado",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    // Elimina el token del dispositivo (al cerrar sesión), solo si es del usuario autenticado.
    @DeleteMapping("/notificaciones/token")
    public ResponseEntity<Map<String, Object>> eliminar(@RequestParam String token) {
        deviceTokenService.eliminar(token);

        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("mensaje", "Token eliminado correctamente");
        return ResponseEntity.ok(respuesta);
    }
}
