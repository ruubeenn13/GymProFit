package com.gymprofit.api.controller;

import com.gymprofit.api.dto.common.CountDTO;
import com.gymprofit.api.dto.entity.sesionentrenamiento.SesionEntrenamientoCreateDTO;
import com.gymprofit.api.dto.entity.sesionentrenamiento.SesionEntrenamientoDTO;
import com.gymprofit.api.dto.entity.sesionentrenamiento.SesionEntrenamientoPatchDTO;
import com.gymprofit.api.dto.entity.sesionentrenamiento.VolumenMuscularDTO;
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

// ============================================================
// SesionEntrenamientoController — controlador REST de sesiones de entrenamiento
// Expone endpoints CRUD y consultas filtradas (por usuario, rutina, fecha,
// estado completada/pendiente) sobre las sesiones registradas por los
// usuarios al entrenar en GymProFit.
// ============================================================
@RestController
@RequestMapping("")
@AllArgsConstructor
@Tag(name = "SesionEntrenamiento Controlador", description = "Gestión de las sesiones de entrenamiento")
public class SesionEntrenamientoController {

    // Servicio con la lógica de negocio de sesiones de entrenamiento
    private final ISesionEntrenamientoService sesionEntrenamientoService;

    @Operation(summary = "Obtiene todas las sesiones de entrenamiento")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Listado de sesiones",
                    content = @Content(schema = @Schema(implementation = SesionEntrenamientoDTO.class))),
            @ApiResponse(responseCode = "404", description = "No se encontraron sesiones",
                    content = @Content(schema = @Schema(implementation = Response.class))),
            @ApiResponse(responseCode = "500", description = "Error al obtener las sesiones",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    // Devuelve todas las sesiones de entrenamiento registradas
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
    // Busca una sesión de entrenamiento por su ID
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
    // Crea una nueva sesión de entrenamiento (normalmente al iniciar un entrenamiento)
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
    // Actualiza por completo una sesión de entrenamiento existente
    @PutMapping("/sesiones")
    public ResponseEntity<SesionEntrenamientoDTO> modificarSesion(@Valid @RequestBody SesionEntrenamientoDTO sesionEntrenamientoDTO) {
        SesionEntrenamientoDTO sesion = sesionEntrenamientoService.modify(sesionEntrenamientoDTO);

        return ResponseEntity.ok(sesion);
    }

    @Operation(summary = "Elimina una sesión de entrenamiento")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sesión eliminada correctamente"),
            @ApiResponse(responseCode = "404", description = "Sesión no encontrada",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    // Elimina una sesión de entrenamiento por su ID
    @DeleteMapping("/sesiones/{id}")
    public ResponseEntity<Map<String, Object>> borrarSesion(@PathVariable Integer id) {
        Map<String, Object> respuesta = new HashMap<>();

        sesionEntrenamientoService.deleteById(id);

        respuesta.put("mensaje", "Sesión de entrenamiento eliminada con ÉXITO");

        return new ResponseEntity<>(respuesta, HttpStatus.OK);
    }

    @Operation(summary = "Completa una sesión de entrenamiento")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sesión completada correctamente",
                    content = @Content(schema = @Schema(implementation = SesionEntrenamientoDTO.class))),
            @ApiResponse(responseCode = "404", description = "Sesión no encontrada",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    // Marca una sesión como completada, con notas opcionales. El parámetro
    // caloriasQuemadas se retira: no había con qué estimarlas (DEC-004 / GP-010).
    @PutMapping("/sesiones/{id}/completar")
    public ResponseEntity<SesionEntrenamientoDTO> completarSesion(@PathVariable Integer id,
                                                                  @RequestParam(required = false) String notas) {
        SesionEntrenamientoDTO sesion = sesionEntrenamientoService.completarSesion(id, notas);

        return ResponseEntity.ok(sesion);
    }

    @Operation(summary = "Obtiene todas las sesiones de un usuario")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sesiones encontradas",
                    content = @Content(schema = @Schema(implementation = SesionEntrenamientoDTO.class))),
            @ApiResponse(responseCode = "404", description = "El usuario indicado no existe",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    // Lista todas las sesiones de un usuario concreto
    @GetMapping("/sesiones/usuario/{usuarioId}")
    public ResponseEntity<List<SesionEntrenamientoDTO>> obtenerSesionesPorUsuario(@PathVariable Integer usuarioId) {
        List<SesionEntrenamientoDTO> sesiones = sesionEntrenamientoService.findByUsuarioId(usuarioId);

        return ResponseEntity.ok(sesiones);
    }

    @Operation(summary = "Obtiene todas las sesiones de una rutina")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sesiones encontradas",
                    content = @Content(schema = @Schema(implementation = SesionEntrenamientoDTO.class))),
            @ApiResponse(responseCode = "404", description = "La rutina indicada no existe",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    // Lista todas las sesiones realizadas con una rutina concreta
    @GetMapping("/sesiones/rutina/{rutinaId}")
    public ResponseEntity<List<SesionEntrenamientoDTO>> obtenerSesionesPorRutina(@PathVariable Integer rutinaId) {
        List<SesionEntrenamientoDTO> sesiones = sesionEntrenamientoService.findByRutinaId(rutinaId);

        return ResponseEntity.ok(sesiones);
    }

    @Operation(summary = "Obtiene todas las sesiones completadas")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sesiones completadas",
                    content = @Content(schema = @Schema(implementation = SesionEntrenamientoDTO.class)))
    })
    // Lista todas las sesiones marcadas como completadas
    @GetMapping("/sesiones/completadas")
    public ResponseEntity<List<SesionEntrenamientoDTO>> obtenerSesionesCompletadas() {
        List<SesionEntrenamientoDTO> sesiones = sesionEntrenamientoService.findCompletadas();

        return ResponseEntity.ok(sesiones);
    }

    @Operation(summary = "Obtiene todas las sesiones pendientes")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sesiones pendientes",
                    content = @Content(schema = @Schema(implementation = SesionEntrenamientoDTO.class)))
    })
    // Lista todas las sesiones aún no completadas
    @GetMapping("/sesiones/pendientes")
    public ResponseEntity<List<SesionEntrenamientoDTO>> obtenerSesionesPendientes() {
        List<SesionEntrenamientoDTO> sesiones = sesionEntrenamientoService.findPendientes();

        return ResponseEntity.ok(sesiones);
    }

    @Operation(summary = "Obtiene las sesiones completadas de un usuario")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sesiones encontradas",
                    content = @Content(schema = @Schema(implementation = SesionEntrenamientoDTO.class))),
            @ApiResponse(responseCode = "404", description = "El usuario indicado no existe",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    // Lista las sesiones completadas de un usuario concreto
    @GetMapping("/sesiones/usuario/{usuarioId}/completadas")
    public ResponseEntity<List<SesionEntrenamientoDTO>> obtenerSesionesCompletadasPorUsuario(@PathVariable Integer usuarioId) {
        List<SesionEntrenamientoDTO> sesiones = sesionEntrenamientoService.findByUsuarioIdAndCompletadas(usuarioId);

        return ResponseEntity.ok(sesiones);
    }

    @Operation(summary = "Obtiene las sesiones pendientes de un usuario")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sesiones encontradas",
                    content = @Content(schema = @Schema(implementation = SesionEntrenamientoDTO.class))),
            @ApiResponse(responseCode = "404", description = "El usuario indicado no existe",
                    content = @Content(schema = @Schema(implementation = SesionEntrenamientoDTO.class)))
    })
    // Lista las sesiones pendientes de un usuario concreto
    @GetMapping("/sesiones/usuario/{usuarioId}/pendientes")
    public ResponseEntity<List<SesionEntrenamientoDTO>> obtenerSesionesPendientesPorUsuario(@PathVariable Integer usuarioId) {
        List<SesionEntrenamientoDTO> sesiones = sesionEntrenamientoService.findByUsuarioIdAndPendientes(usuarioId);

        return ResponseEntity.ok(sesiones);
    }

    @Operation(summary = "Obtiene las sesiones de un usuario en una fecha")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sesiones encontradas",
                    content = @Content(schema = @Schema(implementation = SesionEntrenamientoDTO.class))),
            @ApiResponse(responseCode = "404", description = "El usuario indicado no existe",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping("/sesiones/usuario/{usuarioId}/fecha/{fecha}")
    // Lista las sesiones de un usuario realizadas en una fecha concreta
    public ResponseEntity<List<SesionEntrenamientoDTO>> obtenerSesionesPorUsuarioYFecha(@PathVariable Integer usuarioId,
                                                                                        @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        List<SesionEntrenamientoDTO> sesiones = sesionEntrenamientoService.findByUsuarioIdAndFecha(usuarioId, fecha);

        return ResponseEntity.ok(sesiones);
    }

    @Operation(summary = "Obtiene sesiones por fecha")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sesiones encontradas",
                    content = @Content(schema = @Schema(implementation = SesionEntrenamientoDTO.class)))
    })
    @GetMapping("/sesiones/fecha/{fecha}")
    // Lista todas las sesiones realizadas en una fecha concreta (cualquier usuario)
    public ResponseEntity<List<SesionEntrenamientoDTO>> obtenerSesionesPorFecha(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        List<SesionEntrenamientoDTO> sesiones = sesionEntrenamientoService.findByFecha(fecha);

        return ResponseEntity.ok(sesiones);
    }

    @Operation(summary = "Obtiene sesiones de un usuario con una rutina específica")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sesiones encontradas",
                    content = @Content(schema = @Schema(implementation = SesionEntrenamientoDTO.class))),
            @ApiResponse(responseCode = "404", description = "El usuario o la rutina indicados no existen",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping("/sesiones/usuario/{usuarioId}/rutina/{rutinaId}")
    // Lista las sesiones de un usuario asociadas a una rutina concreta
    public ResponseEntity<List<SesionEntrenamientoDTO>> obtenerSesionesPorUsuarioYRutina(@PathVariable Integer usuarioId,
                                                                                         @PathVariable Integer rutinaId) {
        List<SesionEntrenamientoDTO> sesiones = sesionEntrenamientoService.findByUsuarioIdAndRutinaId(usuarioId, rutinaId);

        return ResponseEntity.ok(sesiones);
    }

    @Operation(summary = "Cuenta las sesiones de un usuario")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cantidad de sesiones")
    })
    // Cuenta el total de sesiones de un usuario
    @GetMapping("/sesiones/count/usuario/{usuarioId}")
    public ResponseEntity<CountDTO> contarSesionesPorUsuario(@PathVariable Integer usuarioId) {
        Long count = sesionEntrenamientoService.countByUsuarioId(usuarioId);

        return ResponseEntity.ok(new CountDTO(count));
    }

    @Operation(summary = "Cuenta las sesiones de una rutina")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cantidad de sesiones")
    })
    // Cuenta el total de sesiones asociadas a una rutina
    @GetMapping("/sesiones/count/rutina/{rutinaId}")
    public ResponseEntity<CountDTO> contarSesionesCompletadasPorUsuario(@PathVariable Integer rutinaId) {
        Long count = sesionEntrenamientoService.countByRutinaId(rutinaId);

        return ResponseEntity.ok(new CountDTO(count));
    }

    @Operation(summary = "Obtiene sesiones de un usuario ordenadas por fecha")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sesiones ordenadas",
                    content = @Content(schema = @Schema(implementation = SesionEntrenamientoDTO.class))),
            @ApiResponse(responseCode = "404", description = "El usuario indicado no existe",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    // Lista las sesiones de un usuario ordenadas cronológicamente por fecha
    @GetMapping("/sesiones/usuario/{usuarioId}/ordenadas")
    public ResponseEntity<List<SesionEntrenamientoDTO>> obtenerSesionesOrdenadasPorUsuario(@PathVariable Integer usuarioId) {
        List<SesionEntrenamientoDTO> sesiones = sesionEntrenamientoService.findByUsuarioIdOrderByFecha(usuarioId);

        return ResponseEntity.ok(sesiones);
    }

    @Operation(summary = "Obtiene sesiones completadas de un usuario ordenadas")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sesiones completadas ordenadas",
                    content = @Content(schema = @Schema(implementation = SesionEntrenamientoDTO.class))),
            @ApiResponse(responseCode = "404", description = "El usuario indicado no existe",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    // Lista las sesiones completadas de un usuario ordenadas por fecha
    @GetMapping("/sesiones/usuario/{usuarioId}/completadas/ordenadas")
    public ResponseEntity<List<SesionEntrenamientoDTO>> obtenerSesionesCompletadasOrdenadasPorUsuario(@PathVariable Integer usuarioId) {
        List<SesionEntrenamientoDTO> sesiones = sesionEntrenamientoService.findCompletadasByUsuario(usuarioId);

        return ResponseEntity.ok(sesiones);
    }

    @Operation(summary = "Actualiza parcialmente una sesión de entrenamiento")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sesión actualizada",
                    content = @Content(schema = @Schema(implementation = SesionEntrenamientoDTO.class))),
            @ApiResponse(responseCode = "404", description = "Sesión no encontrada",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    // Actualización parcial de campos de una sesión de entrenamiento
    @PatchMapping("/sesiones/{id}")
    public ResponseEntity<SesionEntrenamientoDTO> patchSesion(@PathVariable Integer id, @RequestBody SesionEntrenamientoPatchDTO patchDTO) {
        return ResponseEntity.ok(sesionEntrenamientoService.patch(id, patchDTO));
    }

    @Operation(summary = "Series por músculo del usuario en los últimos días",
            description = "Alimenta la silueta muscular de la pantalla de inicio: por cada músculo " +
                    "tocado en sesiones completadas dentro de la ventana, cuántas series ha recibido. " +
                    "Los músculos sin trabajar no aparecen; la app los pinta en gris.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Volumen por músculo",
                    content = @Content(schema = @Schema(implementation = VolumenMuscularDTO.class))),
            @ApiResponse(responseCode = "403", description = "No es tu usuario ni eres ADMIN",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    // Devuelve las series acumuladas por músculo en la ventana indicada (7 días por defecto)
    @GetMapping("/sesiones/usuario/{usuarioId}/volumen-muscular")
    public ResponseEntity<List<VolumenMuscularDTO>> obtenerVolumenMuscular(
            @PathVariable Integer usuarioId,
            @RequestParam(defaultValue = "7") int dias) {

        // Lista vacía y no 404: un usuario que no ha entrenado nunca no es un error, es
        // justamente el caso que la silueta en gris está pensada para enseñar.
        return ResponseEntity.ok(sesionEntrenamientoService.getVolumenMuscular(usuarioId, dias));
    }

    @Operation(summary = "Kilos movidos en una sesión",
            description = "Volumen levantado de la sesión, sumando serie a serie cuando hay registro " +
                    "por serie y cayendo al resumen por ejercicio en las sesiones anteriores. Es el " +
                    "número grande del resumen tras entrenar.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Kilos movidos"),
            @ApiResponse(responseCode = "404", description = "Sesión no encontrada",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    // Devuelve los kilos movidos en la sesión indicada
    @GetMapping("/sesiones/{id}/volumen")
    public ResponseEntity<Map<String, Object>> obtenerVolumenSesion(@PathVariable Integer id) {
        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("volumenKg", sesionEntrenamientoService.getVolumenLevantado(id));

        return ResponseEntity.ok(respuesta);
    }
}
