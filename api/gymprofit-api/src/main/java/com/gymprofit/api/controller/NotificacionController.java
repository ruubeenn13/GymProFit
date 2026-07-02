package com.gymprofit.api.controller;

import com.gymprofit.api.dto.entity.notificacion.NotificacionCreateDTO;
import com.gymprofit.api.dto.entity.notificacion.NotificacionDTO;
import com.gymprofit.api.dto.entity.notificacion.NotificacionPatchDTO;
import com.gymprofit.api.exceptions.InvalidDataException;
import com.gymprofit.api.exceptions.NotFoundEntityException;
import com.gymprofit.api.exceptions.Response;
import com.gymprofit.api.service.notificacion.INotificacionService;
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
import java.util.List;
import java.util.Map;

// ============================================================
// NotificacionController — controlador REST de notificaciones de usuario
// Gestiona el CRUD de notificaciones (RECORDATORIO, LOGRO, OBJETIVO, SISTEMA),
// su marcado como leídas/no leídas, conteos y consultas filtradas por
// usuario y tipo.
// ============================================================
@RestController
@RequestMapping("")
@AllArgsConstructor
@Tag(name = "Notificación Controlador", description = "Gestión de las notificaciones de los usuarios")
public class NotificacionController {

    private final INotificacionService notificacionService;

    // Devuelve todas las notificaciones registradas
    @Operation(summary = "Obtiene todas las notificaciones")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Listado de notificaciones",
                    content = @Content(schema = @Schema(implementation = NotificacionDTO.class))),
            @ApiResponse(responseCode = "404", description = "No se encontraron notificaciones",
                    content = @Content(schema = @Schema(implementation = Response.class))),
            @ApiResponse(responseCode = "500", description = "Error al obtener las notificaciones",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping("/notificaciones")
    public ResponseEntity<List<NotificacionDTO>> findAll() {
        List<NotificacionDTO> notificaciones = notificacionService.findAll();

        return ResponseEntity.ok(notificaciones);
    }

    @Operation(summary = "Obtiene una notificación por ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Notificación encontrada",
                    content = @Content(schema = @Schema(implementation = NotificacionDTO.class))),
            @ApiResponse(responseCode = "404", description = "Notificación no encontrada",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    // Busca una notificación por su ID
    @GetMapping("/notificaciones/{id}")
    public ResponseEntity<NotificacionDTO> obtenerNotificacion(@PathVariable Integer id) {
        NotificacionDTO notificacion = notificacionService.findById(id);

        return ResponseEntity.ok(notificacion);
    }

    @Operation(summary = "Crea una nueva notificación. Tipos válidos: RECORDATORIO, LOGRO, OBJETIVO, SISTEMA")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Notificación creada correctamente",
                    content = @Content(schema = @Schema(implementation = NotificacionDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o tipo de notificación incorrecto",
                    content = @Content(schema = @Schema(implementation = Response.class))),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    // Crea una nueva notificación para un usuario
    @PostMapping("/notificaciones")
    public ResponseEntity<NotificacionDTO> save(@Valid @RequestBody NotificacionCreateDTO notificacionCreateDTO) {
        NotificacionDTO notificacion = notificacionService.save(notificacionCreateDTO);

        return ResponseEntity.ok(notificacion);
    }

    @Operation(summary = "Elimina una notificación por ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Notificación eliminada correctamente"),
            @ApiResponse(responseCode = "404", description = "Notificación no encontrada",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    // Elimina una notificación por ID
    @DeleteMapping("/notificaciones/{id}")
    public ResponseEntity<Map<String, Object>> borrarNotificacion(@PathVariable Integer id) {
        Map<String, Object> respuesta = new HashMap<>();

        notificacionService.deleteById(id);

        respuesta.put("mensaje", "Notificación eliminada con ÉXITO");

        return new ResponseEntity<>(respuesta, HttpStatus.OK);
    }

    @Operation(summary = "Obtiene todas las notificaciones de un usuario")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Notificaciones encontradas",
                    content = @Content(schema = @Schema(implementation = NotificacionDTO.class))),
            @ApiResponse(responseCode = "404", description = "No se encontraron notificaciones para este usuario",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    // Lista todas las notificaciones de un usuario
    @GetMapping("/notificaciones/usuario/{usuarioId}")
    public ResponseEntity<List<NotificacionDTO>> obtenerNotificacionPorUsuario(@PathVariable Integer usuarioId) {
        List<NotificacionDTO> notificaciones = notificacionService.findByUsuarioId(usuarioId);

        if (notificaciones.isEmpty()) {
            throw new NotFoundEntityException("No se encontraron notificaciones para el usuario con id " + usuarioId);
        }

        return ResponseEntity.ok(notificaciones);
    }

    @Operation(summary = "Obtiene las notificaciones de un usuario ordenadas por fecha")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Notificaciones encontradas ordenadas",
                    content = @Content(schema = @Schema(implementation = NotificacionDTO.class))),
            @ApiResponse(responseCode = "404", description = "No se encontraron notificaciones",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    // Lista las notificaciones de un usuario ordenadas por fecha
    @GetMapping("/notificaciones/usuario/{usuarioId}/ordenadas")
    public ResponseEntity<List<NotificacionDTO>> obtenerNotificacionPorUsuarioOrdenadas(@PathVariable Integer usuarioId) {
        List<NotificacionDTO> notificaciones = notificacionService.findByUsuarioIdOrdenadas(usuarioId);

        if (notificaciones.isEmpty()) {
            throw new NotFoundEntityException("No se encontraron notificaciones para el usuario con id " + usuarioId);
        }

        return ResponseEntity.ok(notificaciones);
    }

    @Operation(summary = "Obtiene las notificaciones no leídas de un usuario")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Notificaciones no leídas encontradas",
                    content = @Content(schema = @Schema(implementation = NotificacionDTO.class))),
            @ApiResponse(responseCode = "404", description = "No se encontraron notificaciones no leídas",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    // Lista las notificaciones no leídas de un usuario
    @GetMapping("/notificaciones/usuario/{usuarioId}/no-leidas")
    public ResponseEntity<List<NotificacionDTO>> obtenerNoLeidasPorUsuario(@PathVariable Integer usuarioId) {
        List<NotificacionDTO> notificaciones = notificacionService.findNoLeidasByUsuarioId(usuarioId);

        if (notificaciones.isEmpty()) {
            throw new NotFoundEntityException("No se encontraron notificaciones no leídas para el usuario con id " + usuarioId);
        }

        return ResponseEntity.ok(notificaciones);
    }

    @Operation(summary = "Obtiene las notificaciones leídas de un usuario")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Notificaciones leídas encontradas",
                    content = @Content(schema = @Schema(implementation = NotificacionDTO.class))),
            @ApiResponse(responseCode = "404", description = "No se encontraron notificaciones leídas",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    // Lista las notificaciones ya leídas de un usuario
    @GetMapping("/notificaciones/usuario/{usuarioId}/leidas")
    public ResponseEntity<List<NotificacionDTO>> obtenerLeidasPorUsuario(@PathVariable Integer usuarioId) {
        List<NotificacionDTO> notificaciones = notificacionService.findLeidasByUsuarioId(usuarioId);

        if (notificaciones.isEmpty()) {
            throw new NotFoundEntityException("No se encontraron notificaciones leídas para el usuario con id " + usuarioId);
        }

        return ResponseEntity.ok(notificaciones);
    }

    @Operation(summary = "Obtiene todas las notificaciones de un usuario por tipo")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Notificaciones encontradas",
                    content = @Content(schema = @Schema(implementation = NotificacionDTO.class))),
            @ApiResponse(responseCode = "400", description = "Tipo de notificación inválido",
                    content = @Content(schema = @Schema(implementation = Response.class))),
            @ApiResponse(responseCode = "404", description = "No se encontraron notificaciones de ese tipo")
    })
    // Filtra las notificaciones de un usuario por tipo, validando que el tipo no esté vacío
    @GetMapping("/notificaciones/usuario/{usuarioId}/tipo/{tipo}")
    public ResponseEntity<List<NotificacionDTO>> obtenerNotificacionPorUsuarioYTipo(@PathVariable Integer usuarioId,
                                                                                    @PathVariable String tipo) {
        if (tipo == null ||tipo.trim().isEmpty()) {
            throw new InvalidDataException("El tipo no puede estar vacío");
        }

        List<NotificacionDTO> notificaciones = notificacionService.findByUsuarioIdAndTipo(usuarioId, tipo);

        if (notificaciones.isEmpty()) {
            throw new InvalidDataException("No se encontraron notificaciones de tipo " + tipo + " para el usuario " + usuarioId);
        }

        return ResponseEntity.ok(notificaciones);
    }

    @Operation(summary = "Marca una notificación como leída")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Notificación marcada como leída",
                    content = @Content(schema = @Schema(implementation = NotificacionDTO.class))),
            @ApiResponse(responseCode = "404", description = "Notificación no encontrada",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    // Marca una notificación concreta como leída
    @PutMapping("/notificaciones/{id}/leer")
    public ResponseEntity<NotificacionDTO> marcarComoLeida(@PathVariable Integer id) {
        NotificacionDTO notificacionDTO = notificacionService.marcarComoLeida(id);

        return ResponseEntity.ok(notificacionDTO);
    }

    @Operation(summary = "Marca todas las notificaciones de un usuario como leídas")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Todas las notificaciones marcadas como leídas")
    })
    // Marca todas las notificaciones de un usuario como leídas
    @PutMapping("/notificaciones/usuario/{usuarioId}/leer-todas")
    public ResponseEntity<Map<String, Object>> marcarTodasComoLeidas(@PathVariable Integer usuarioId) {
        Map<String, Object> respuesta = new HashMap<>();

        notificacionService.marcarTodasComoLeidas(usuarioId);

        respuesta.put("mensaje", "Todas las notificaciones del usuario " + usuarioId + " marcadas como leídas con ÉXITO");

        return new ResponseEntity<>(respuesta, HttpStatus.OK);
    }

    @Operation(summary = "Elimina todas las notificaciones de un usuario")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Notificaciones eliminadas correctamente"),
            @ApiResponse(responseCode = "500", description = "Error al eliminar las notificaciones",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    // Elimina todas las notificaciones de un usuario
    @DeleteMapping("/notificaciones/usuario/{usuarioId}")
    public ResponseEntity<Map<String, Object>> borrarNotificacionesUsuario(@PathVariable Integer usuarioId) {
        Map<String, Object> respuesta = new HashMap<>();

        notificacionService.deleteByUsuarioId(usuarioId);

        respuesta.put("mensaje", "Notificaciones del usuario " + usuarioId + " eliminadas con ÉXITO");

        return new ResponseEntity<>(respuesta, HttpStatus.OK);
    }

    @Operation(summary = "Cuenta todas las notificaciones de un usuario")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Total de notificaciones del usuario")
    })
    // Cuenta el total de notificaciones de un usuario
    @GetMapping("/notificaciones/count/usuario/{usuarioId}")
    public ResponseEntity<Map<String, Object>> contarNotificacionesUsuario(@PathVariable Integer usuarioId) {
        Map<String, Object> respuesta = new HashMap<>();

        Long count = notificacionService.countByUsuarioId(usuarioId);

        respuesta.put("usuarioId", usuarioId);
        respuesta.put("total", count);

        return ResponseEntity.ok(respuesta);
    }

    @Operation(summary = "Cuenta las notificaciones no leídas de un usuario")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Total de notificaciones no leídas")
    })
    // Cuenta las notificaciones no leídas de un usuario
    @GetMapping("/notificaciones/count/usuario/{usuarioId}/no-leidas")
    public ResponseEntity<Map<String, Object>> contarNotificacionesNoLeidasUsuario(@PathVariable Integer usuarioId) {
        Map<String, Object> respuesta = new HashMap<>();

        Long count = notificacionService.countNoLeidasByUsuarioId(usuarioId);

        respuesta.put("usuarioId", usuarioId);
        respuesta.put("total", count);

        return ResponseEntity.ok(respuesta);
    }

    @Operation(summary = "Verifica si un usuario tiene notificaciones no leídas")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Total de notificaciones no leídas")
    })
    // Comprueba si un usuario tiene alguna notificación pendiente de leer
    @GetMapping("/notificaciones/exists/usuario/{usuarioId}/no-leidas")
    public ResponseEntity<Map<String, Object>> existenNotificacionesNoLeidas(@PathVariable Integer usuarioId) {
        Map<String, Object> respuesta = new HashMap<>();

        boolean existe = notificacionService.existenNoLeidasByUsuarioId(usuarioId);

        respuesta.put("usuarioId", usuarioId);
        respuesta.put("tieneNoLeidas", existe);

        return ResponseEntity.ok(respuesta);
    }

    @Operation(summary = "Actualiza parcialmente una notificación")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Notificación actualizada",
                    content = @Content(schema = @Schema(implementation = NotificacionDTO.class))),
            @ApiResponse(responseCode = "404", description = "Notificación no encontrada",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    // Actualiza parcialmente campos de una notificación (PATCH)
    @PatchMapping("/notificaciones/{id}")
    public ResponseEntity<NotificacionDTO> patchNotificacion(@PathVariable Integer id, @RequestBody NotificacionPatchDTO patchDTO) {
        return ResponseEntity.ok(notificacionService.patch(id, patchDTO));
    }
}
