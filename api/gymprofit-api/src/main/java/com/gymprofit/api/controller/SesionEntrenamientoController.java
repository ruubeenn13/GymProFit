package com.gymprofit.api.controller;

import com.gymprofit.api.dto.entity.sesionentrenamiento.SesionEntrenamientoCreateDTO;
import com.gymprofit.api.dto.entity.sesionentrenamiento.SesionEntrenamientoDTO;
import com.gymprofit.api.exceptions.NotFoundEntityException;
import com.gymprofit.api.exceptions.Response;
import com.gymprofit.api.service.sesionentrenamiento.ISesionEntrenamientoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api")
@AllArgsConstructor
@Tag(name = "SesionEntrenamiento Controlador", description = "Gestión de las sesiones de entrenamiento")
public class SesionEntrenamientoController {

    private final ISesionEntrenamientoService sesionEntrenamientoService;

    @Operation(summary = "Obtiene todas las sesiones de entrenamiento")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Listado de sesiones",
                    content = @Content(schema = @Schema(implementation = SesionEntrenamientoDTO.class))),
            @ApiResponse(responseCode = "400", description = "No se encontraron sesiones",
                    content = @Content(schema = @Schema(implementation = Response.class))),
            @ApiResponse(responseCode = "500", description = "Error al obtener las sesiones",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping("/sesiones")
    public ResponseEntity<List<SesionEntrenamientoDTO>> findAll() {
        List<SesionEntrenamientoDTO> sesiones = sesionEntrenamientoService.findAll();

        return ResponseEntity.ok(sesiones);
    }

    @Operation(summary = "Obtiene una sesión de entrenamiento por ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sesión encontrada",
                    content = @Content(schema = @Schema(implementation = SesionEntrenamientoDTO.class))),
            @ApiResponse(responseCode = "404", description = "Sesión no encontrada",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping("/sesiones/{id}")
    public ResponseEntity<SesionEntrenamientoDTO> obtenerSesion(@PathVariable Integer id) {
        SesionEntrenamientoDTO sesion = sesionEntrenamientoService.findById(id);

        return ResponseEntity.ok(sesion);
    }

    @Operation(summary = "Crea una nueva sesión de entrenamiento")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sesión creada correctamente",
                    content = @Content(schema = @Schema(implementation = SesionEntrenamientoDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @PostMapping("/sesiones")
    public ResponseEntity<SesionEntrenamientoDTO> guardarSesion(@Valid @RequestBody SesionEntrenamientoCreateDTO sesionEntrenamientoCreateDTO) {
        SesionEntrenamientoDTO sesion = sesionEntrenamientoService.save(sesionEntrenamientoCreateDTO);

        return ResponseEntity.ok(sesion);
    }

    @Operation(summary = "Modifica una sesión de entrenamiento existente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sesión modificada correctamente",
                    content = @Content(schema = @Schema(implementation = SesionEntrenamientoDTO.class))),
            @ApiResponse(responseCode = "404", description = "Sesión no encontrada",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @PutMapping("/sesiones")
    public ResponseEntity<SesionEntrenamientoDTO> modificarSesion(@Valid @RequestBody SesionEntrenamientoDTO sesionEntrenamientoDTO) {
        SesionEntrenamientoDTO sesion = sesionEntrenamientoService.modify(sesionEntrenamientoDTO);

        return ResponseEntity.ok(sesion);
    }

    @Operation(summary = "Elimmina una sesión de entrenamiento")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sesión eliminada correctamente"),
            @ApiResponse(responseCode = "404", description = "Sesión no encontrada",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @DeleteMapping("/sesiones/{id}")
    public ResponseEntity<Map<String, Object>> borrarSesion(@PathVariable Integer id) {
        Map<String, Object> respuesta = new HashMap<>();

        try {
            sesionEntrenamientoService.deleteById(id);

            respuesta.put("mensaje", "Sesión de entrenamiento eliminada con ÉXITO");
        } catch (Exception e) {
            respuesta.put("mensaje", "Error al borrar la sesión " + id);
            respuesta.put("error", e.getMessage());

            return new ResponseEntity<>(respuesta, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(respuesta, HttpStatus.OK);
    }

    @Operation(summary = "Completa una sesión de entrenamiento")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sesión completada correctamente",
                    content = @Content(schema = @Schema(implementation = SesionEntrenamientoDTO.class))),
            @ApiResponse(responseCode = "404", description = "Sesión no encontrada",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @PutMapping("/sesiones/{id}/completar")
    public ResponseEntity<SesionEntrenamientoDTO> completarSesion(@PathVariable Integer id,
                                                                  @RequestParam(required = false) Integer caloriasQuemadas,
                                                                  @RequestParam(required = false) String notas) {
        SesionEntrenamientoDTO sesion = sesionEntrenamientoService.completarSesion(id, caloriasQuemadas, notas);

        return ResponseEntity.ok(sesion);
    }

    @Operation(summary = "Obtiene todas las sesiones de un usuario")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sesiones encontradas",
                    content = @Content(schema = @Schema(implementation = SesionEntrenamientoDTO.class))),
            @ApiResponse(responseCode = "404", description = "No se encontraron sesiones",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping("/sesiones/usuario/{usuarioId}")
    public ResponseEntity<List<SesionEntrenamientoDTO>> obtenerSesionesPorUsuario(@PathVariable Integer usuarioId) {
        List<SesionEntrenamientoDTO> sesiones = sesionEntrenamientoService.findByUsuarioId(usuarioId);

        if (sesiones.isEmpty()) {
            throw new NotFoundEntityException("No se encontraron sesiónes para el usuario con id " + usuarioId);
        }

        return ResponseEntity.ok(sesiones);
    }

    @Operation(summary = "Obtiene todas las sesiones de una rutina")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sesiones encontradas",
                    content = @Content(schema = @Schema(implementation = SesionEntrenamientoDTO.class))),
            @ApiResponse(responseCode = "404", description = "No se encontraron sesiones",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping("/sesiones/rutina/{rutinaId}")
    public ResponseEntity<List<SesionEntrenamientoDTO>> obtenerSesionesPorRutina(@PathVariable Integer rutinaId) {
        List<SesionEntrenamientoDTO> sesiones = sesionEntrenamientoService.findByRutinaId(rutinaId);

        if (sesiones.isEmpty()) {
            throw new NotFoundEntityException("No se encontraron sesiones para la rutina con id " + rutinaId);
        }

        return ResponseEntity.ok(sesiones);
    }

    @Operation(summary = "Obtiene todas las sesiones completadas")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sesiones completadas",
                    content = @Content(schema = @Schema(implementation = SesionEntrenamientoDTO.class))),
            @ApiResponse(responseCode = "404", description = "No se encontraron sesiones",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping("/sesiones/completadas")
    public ResponseEntity<List<SesionEntrenamientoDTO>> obtenerSesionesCompletadas() {
        List<SesionEntrenamientoDTO> sesiones = sesionEntrenamientoService.findCompletadas();

        if (sesiones.isEmpty()) {
            throw new NotFoundEntityException("No se encontraron sesiones completadas");
        }

        return ResponseEntity.ok(sesiones);
    }

    @Operation(summary = "Obtiene todas las sesiones pendientes")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sesiones pendientes",
                    content = @Content(schema = @Schema(implementation = SesionEntrenamientoDTO.class))),
            @ApiResponse(responseCode = "404", description = "No se encontraron sesiones",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping("/sesiones/pendientes")
    public ResponseEntity<List<SesionEntrenamientoDTO>> obtenerSesionesPendientes() {
        List<SesionEntrenamientoDTO> sesiones = sesionEntrenamientoService.findPendientes();

        if (sesiones.isEmpty()) {
            throw new NotFoundEntityException("No se encontraron seisons pendientes");
        }

        return ResponseEntity.ok(sesiones);
    }

    @Operation(summary = "Obtiene las sesiones completadas de un usuario")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sesiones encontradas",
                    content = @Content(schema = @Schema(implementation = SesionEntrenamientoDTO.class))),
            @ApiResponse(responseCode = "404", description = "No se encontraron sesiones",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping("/sesiones/usuario/{usuarioId}/completadas")
    public ResponseEntity<List<SesionEntrenamientoDTO>> obtenerSesionesCompletadasPorUsuario(@PathVariable Integer usuarioId) {
        List<SesionEntrenamientoDTO> sesiones = sesionEntrenamientoService.findByUsuarioIdAndCompletadas(usuarioId);

        if (sesiones.isEmpty()) {
            throw new NotFoundEntityException("No se encontraron sesiones completadas para el usuario " + usuarioId);
        }

        return ResponseEntity.ok(sesiones);
    }

    @Operation(summary = "Obtiene las sesiones pendientes de un usuario")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sesiones encontradas",
                    content = @Content(schema = @Schema(implementation = SesionEntrenamientoDTO.class))),
            @ApiResponse(responseCode = "404", description = "No se encontraron sesiones",
                    content = @Content(schema = @Schema(implementation = SesionEntrenamientoDTO.class)))
    })
    @GetMapping("/sesiones/usuario/{usuarioId}/pendientes")
    public ResponseEntity<List<SesionEntrenamientoDTO>> obtenerSesionesPendientesPorUsuario(@PathVariable Integer usuarioId) {
        List<SesionEntrenamientoDTO> sesiones = sesionEntrenamientoService.findByUsuarioIdAndPendientes(usuarioId);

        if (sesiones.isEmpty()) {
            throw new NotFoundEntityException("No se encontraron sesiones pendientes para el usuario " + usuarioId);
        }

        return ResponseEntity.ok(sesiones);
    }

    @Operation(summary = "Obtiene las sesiones de un usuario en una fecha")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sesiones encontradas",
                    content = @Content(schema = @Schema(implementation = SesionEntrenamientoDTO.class))),
            @ApiResponse(responseCode = "404", description = "No se encontraron sesiones",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping("/sesiones/usuario/{usuarioId}/fecha/{fecha}")
    public ResponseEntity<List<SesionEntrenamientoDTO>> obtenerSesionesPorUsuarioYFecha(@PathVariable Integer usuarioId,
                                                                                        @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        List<SesionEntrenamientoDTO> sesiones = sesionEntrenamientoService.findByUsuarioIdAndFecha(usuarioId, fecha);

        if (sesiones.isEmpty()) {
            throw new NotFoundEntityException("No se encontraron sesiones para el usuario " + usuarioId + " en la fecha " + fecha);
        }

        return ResponseEntity.ok(sesiones);
    }

    @Operation(summary = "Obtiene sesiones por fecha")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sesiones encontradas",
                    content = @Content(schema = @Schema(implementation = SesionEntrenamientoDTO.class))),
            @ApiResponse(responseCode = "404", description = "No se encontraron sesiones",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping("/sesiones/fecha/{fecha}")
    public ResponseEntity<List<SesionEntrenamientoDTO>> obtenerSesionesPorFecha(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        List<SesionEntrenamientoDTO> sesiones = sesionEntrenamientoService.findByFecha(fecha);

        if (sesiones.isEmpty()) {
            throw new NotFoundEntityException("No se encontraron sesiones en la fecha " + fecha);
        }

        return ResponseEntity.ok(sesiones);
    }

    @Operation(summary = "Obtiene sesiones de un usuario co una rutina específica")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sesiones encontradas",
                    content = @Content(schema = @Schema(implementation = SesionEntrenamientoDTO.class))),
            @ApiResponse(responseCode = "404", description = "No se encontraron sesiones",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping("/sesiones/usuario/{usuarioId}/rutina/{rutinaId}")
    public ResponseEntity<List<SesionEntrenamientoDTO>> obtenerSesionesPorUsuarioYRutina(@PathVariable Integer usuarioId,
                                                                                         @PathVariable Integer rutinaId) {
        List<SesionEntrenamientoDTO> sesiones = sesionEntrenamientoService.findByUsuarioIdAndRutinaId(usuarioId, rutinaId);

        if (sesiones.isEmpty()) {
            throw new NotFoundEntityException("No se encontraron sesiones para el usuario con id " + usuarioId + " con la rutina " + rutinaId);
        }

        return ResponseEntity.ok(sesiones);
    }

    @Operation(summary = "Cuenta las sesiones de un usuario")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cantidad de sesiones")
    })
    @GetMapping("/sesiones/count/usuario/{usuarioId}")
    public ResponseEntity<Map<String, Object>> contarSesionesPorUsuario(@PathVariable Integer usuarioId) {
        Map<String, Object> respuesta = new HashMap<>();

        Long count = sesionEntrenamientoService.countByUsuarioId(usuarioId);

        respuesta.put("count", count);
        respuesta.put("usuarioId", usuarioId);

        return ResponseEntity.ok(respuesta);
    }

    @Operation(summary = "Cuenta las sesiones de una rutina")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cantidad de sesiones")
    })
    @GetMapping("/sesiones/count/rutina/{rutinaId}")
    public ResponseEntity<Map<String, Object>> contarSesionesCompletadasPorUsuario(@PathVariable Integer rutinaId) {
        Map<String, Object> respuesta = new HashMap<>();

        Long count = sesionEntrenamientoService.countByRutinaId(rutinaId);

        respuesta.put("count", count);
        respuesta.put("rutinaId", rutinaId);

        return ResponseEntity.ok(respuesta);
    }

    @Operation(summary = "Obtiene sesiones de un usuario ordenadas por fecha")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sesiones ordenadas",
                    content = @Content(schema = @Schema(implementation = SesionEntrenamientoDTO.class))),
            @ApiResponse(responseCode = "404", description = "No se encontraron sesiones",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping("/sesiones/usuario/{usuarioId}/ordenadas")
    public ResponseEntity<List<SesionEntrenamientoDTO>> obtenerSesionesOrdenadasPorUsuario(@PathVariable Integer usuarioId) {
        List<SesionEntrenamientoDTO> sesiones = sesionEntrenamientoService.findByUsuarioIdOrderByFecha(usuarioId);

        if (sesiones.isEmpty()) {
            throw new NotFoundEntityException("No se encontraron sesiones para el usuario " + usuarioId);
        }

        return ResponseEntity.ok(sesiones);
    }

    @Operation(summary = "Obtiene sesiones completadas de un usuario ordenadas")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sesiones completadas ordenadas",
                    content = @Content(schema = @Schema(implementation = SesionEntrenamientoDTO.class))),
            @ApiResponse(responseCode = "404", description = "No se encontraron sesiones",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping("/sesiones/usuario/{usuarioId}/completadas/ordeenadas")
    public ResponseEntity<List<SesionEntrenamientoDTO>> obtenerSesionesCompletadasOrdenadasPorUsuario(@PathVariable Integer usuarioId) {
        List<SesionEntrenamientoDTO> sesiones = sesionEntrenamientoService.findCompletadasByUsuario(usuarioId);

        if (sesiones.isEmpty()) {
            throw new NotFoundEntityException("No se encontraron sesiones completadas para el usuario con id " + usuarioId);
        }

        return ResponseEntity.ok(sesiones);
    }
}
