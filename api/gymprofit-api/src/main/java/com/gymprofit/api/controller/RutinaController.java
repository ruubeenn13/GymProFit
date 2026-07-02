package com.gymprofit.api.controller;

import com.gymprofit.api.dto.entity.rutina.RutinaCreateDTO;
import com.gymprofit.api.dto.entity.rutina.RutinaDTO;
import com.gymprofit.api.dto.entity.rutina.RutinaPatchDTO;
import com.gymprofit.api.exceptions.InvalidDataException;
import com.gymprofit.api.exceptions.NotFoundEntityException;
import com.gymprofit.api.exceptions.Response;
import com.gymprofit.api.service.rutina.IRutinaService;
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
// RutinaController — controlador REST de rutinas de entrenamiento
// Gestiona el CRUD de rutinas (propias de un usuario o predefinidas por la
// app), su activación/desactivación (borrado lógico) y eliminación
// permanente, con búsquedas por usuario, nivel y nombre.
// ============================================================
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("")
@AllArgsConstructor
@Tag(name = "Rutina Controlador", description = "Gestión de las rutinas")
public class RutinaController {

    private final IRutinaService rutinaService;

    // Devuelve todas las rutinas registradas
    @Operation(summary = "Obtiene todas las rutinas")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Listado de rutinas",
                    content = @Content(schema = @Schema(implementation = RutinaDTO.class))),
            @ApiResponse(responseCode = "404", description = "Rutinas no encontradas",
                    content = @Content(schema = @Schema(implementation = Response.class))),
            @ApiResponse(responseCode = "500", description = "Error al obtener las rutinas",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping("/rutinas")
    public ResponseEntity<List<RutinaDTO>> findAll() {
        List<RutinaDTO> rutinas = rutinaService.findAll();

        return ResponseEntity.ok(rutinas);
    }

    @Operation(summary = "Obtiene una rutina por ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rutina encontrada",
                    content = @Content(schema = @Schema(implementation = RutinaDTO.class))),
            @ApiResponse(responseCode = "404", description = "Rutina no encontrada",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    // Busca una rutina por su ID
    @GetMapping("/rutinas/{id}")
    public ResponseEntity<RutinaDTO> obtenerRutina(@PathVariable Integer id) {
        RutinaDTO rutina = rutinaService.findById(id);

        return ResponseEntity.ok(rutina);
    }

    @Operation(summary = "Crea una nueva rutina")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rutina creada correctamente",
                    content = @Content(schema = @Schema(implementation = RutinaDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    // Crea una nueva rutina
    @PostMapping("/rutinas")
    public ResponseEntity<RutinaDTO> guardarRutina(@Valid @RequestBody RutinaCreateDTO rutinaCreateDTO) {
        RutinaDTO rutina = rutinaService.save(rutinaCreateDTO);

        return ResponseEntity.ok(rutina);
    }

    @Operation(summary = "Modifica una rutina existente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rutina modificada correctamente",
                    content = @Content(schema = @Schema(implementation = RutinaDTO.class))),
            @ApiResponse(responseCode = "404", description = "Rutina no encontrada",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    // Actualiza completamente una rutina existente
    @PutMapping("/rutinas")
    public ResponseEntity<RutinaDTO> modificarRutina(@Valid @RequestBody RutinaDTO rutinaDTO) {
        RutinaDTO rutina = rutinaService.modify(rutinaDTO);

        return ResponseEntity.ok(rutina);
    }

    @Operation(summary = "Desactiva una rutina")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rutina desactivada correctamente",
                    content = @Content(schema = @Schema(implementation = RutinaDTO.class))),
            @ApiResponse(responseCode = "404", description = "Rutina no encontrada",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    // Desactiva una rutina (borrado lógico, no elimina el registro)
    @DeleteMapping("/rutinas/{id}")
    public ResponseEntity<Map<String, Object>> borrarRutina(@PathVariable Integer id) {
        Map<String, Object> respuesta = new HashMap<>();

        rutinaService.deleteById(id);

        respuesta.put("mensaje", "Rutina desactivada con ÉXITO");

        return new ResponseEntity<>(respuesta, HttpStatus.OK);
    }

    @Operation(summary = "Activa una rutina desactivada")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rutina activada correctamente",
                    content = @Content(schema = @Schema(implementation = RutinaDTO.class))),
            @ApiResponse(responseCode = "404", description = "Rutina no encontrada",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    // Reactiva una rutina previamente desactivada
    @PutMapping("/rutinas/{id}/activar")
    public ResponseEntity<Map<String, Object>> activarRutina(@PathVariable Integer id) {
        Map<String, Object> respuesta = new HashMap<>();

        rutinaService.activateById(id);

        respuesta.put("mensaje", "Rutina activada con ÉXITO");

        return new ResponseEntity<>(respuesta, HttpStatus.OK);
    }

    @Operation(summary = "Elimina permanentemente una rutina de la base de datos")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rutina eliminada permanentemente"),
            @ApiResponse(responseCode = "404", description = "Rutina no encontrada",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    // Elimina definitivamente una rutina de la base de datos (borrado físico)
    @DeleteMapping("/rutinas/{id}/permanente")
    public ResponseEntity<Map<String, Object>> eliminarPermanente(@PathVariable Integer id) {
        Map<String, Object> respuesta = new HashMap<>();

        rutinaService.permanentDeleteById(id);

        respuesta.put("mensaje", "Rutina eliminada PERMANENTEMENTE con ÉXITO");

        return new ResponseEntity<>(respuesta, HttpStatus.OK);
    }

    @Operation(summary = "Busca rutinas por usuario ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rutinas encontradas",
                    content = @Content(schema = @Schema(implementation = RutinaDTO.class))),
            @ApiResponse(responseCode = "404", description = "No se encontraron rutinas para este usuario",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    // Lista todas las rutinas de un usuario
    @GetMapping("/rutinas/usuario/{usuarioId}")
    public ResponseEntity<List<RutinaDTO>> obtenerRutinasPorUsuario(@PathVariable Integer usuarioId) {
        List<RutinaDTO> rutinas = rutinaService.findByUsuarioId(usuarioId);

        if (rutinas.isEmpty()) {
            throw new NotFoundEntityException("No se encontraron rutinas para el usuario con id " + usuarioId);
        }

        return ResponseEntity.ok(rutinas);
    }

    @Operation(summary = "Busca rutinas por nivel")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rutinas encontradas",
                    content = @Content(schema = @Schema(implementation = RutinaDTO.class))),
            @ApiResponse(responseCode = "404", description = "No se encontraron rutinas para este nivel",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    // Filtra rutinas por nivel de dificultad
    @GetMapping("/rutinas/nivel/{nivel}")
    public ResponseEntity<List<RutinaDTO>> obtenerRutinasPorNivel(@PathVariable String nivel) {
        if (nivel == null || nivel.trim().isEmpty()) {
            throw new InvalidDataException("El nivel no puede estar vacío");
        }

        List<RutinaDTO> rutinas = rutinaService.findByNivel(nivel);

        if (rutinas.isEmpty()) {
            throw new NotFoundEntityException("No se encontraron rutinas para el nivel " + nivel);
        }

        return ResponseEntity.ok(rutinas);
    }

    @Operation(summary = "Busca rutinas por nombre")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rutinas encontradas",
                    content = @Content(schema = @Schema(implementation = RutinaDTO.class))),
            @ApiResponse(responseCode = "404", description = "No se encontraron rutinas con ese nombre",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    // Busca rutinas cuyo nombre coincida con el indicado
    @GetMapping("/rutinas/nombre/{nombre}")
    public ResponseEntity<List<RutinaDTO>> obtenerRutinasPorNombre(@PathVariable String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new InvalidDataException("El nombre no puede estar vacío");
        }

        List<RutinaDTO> rutinas = rutinaService.findByNombre(nombre);

        if (rutinas.isEmpty()) {
            throw new NotFoundEntityException("No se encontraron rutinas con el nombre: " + nombre);
        }

        return ResponseEntity.ok(rutinas);
    }

    @Operation(summary = "Obtiene todas las rutinas activas")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Listado de rutinas activas",
                    content = @Content(schema = @Schema(implementation = RutinaDTO.class))),
            @ApiResponse(responseCode = "404", description = "No se encontraron rutinas activas",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    // Devuelve todas las rutinas activas (no desactivadas)
    @GetMapping("/rutinas/activas")
    public ResponseEntity<List<RutinaDTO>> obtenerRutinasActivas() {
        List<RutinaDTO> rutinas = rutinaService.findActivas();

        if (rutinas.isEmpty()) {
            throw new NotFoundEntityException("No se encontraron rutinas activas");
        }

        return ResponseEntity.ok(rutinas);
    }

    @Operation(summary = "Obtiene todas las rutinas predefinidas")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Listado de rutinas predefinidas",
                    content = @Content(schema = @Schema(implementation = RutinaDTO.class))),
            @ApiResponse(responseCode = "404", description = "No se encontraron rutinas predefinidas",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    // Devuelve las rutinas predefinidas de la aplicación (no creadas por usuarios)
    @GetMapping("/rutinas/predefinidas")
    public ResponseEntity<List<RutinaDTO>> obtenerRutinasPredefinidas() {
        List<RutinaDTO> rutinas = rutinaService.findPredefinidas();

        if (rutinas.isEmpty()) {
            throw new NotFoundEntityException("No se encontraron rutinas predefinidas");
        }

        return ResponseEntity.ok(rutinas);
    }

    @Operation(summary = "Busca rutinas activas de un usuario")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rutinas activas encontradas",
                    content = @Content(schema = @Schema(implementation = RutinaDTO.class))),
            @ApiResponse(responseCode = "404", description = "No se encontraron rutinas activas para este usuario",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    // Lista las rutinas activas de un usuario concreto
    @GetMapping("/rutinas/usuario/{usuarioId}/activas")
    public ResponseEntity<List<RutinaDTO>> obtenerRutinasActivasPorUsuario(@PathVariable Integer usuarioId) {
        List<RutinaDTO> rutinas = rutinaService.findByUsuarioIdAndActivas(usuarioId);

        if (rutinas.isEmpty()) {
            throw new NotFoundEntityException("No se encontraron rutinas activas para el usuario con id " + usuarioId);
        }

        return ResponseEntity.ok(rutinas);
    }

    @Operation(summary = "Busca rutinas predefinidas por nivel")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rutinas predefinidas encontradas",
                    content = @Content(schema = @Schema(implementation = RutinaDTO.class))),
            @ApiResponse(responseCode = "404", description = "No se encontraron rutinas predefinidas para este nivel",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    // Filtra las rutinas predefinidas por nivel de dificultad
    @GetMapping("/rutinas/predefinidas/nivel/{nivel}")
    public ResponseEntity<List<RutinaDTO>> obtenerRutinasPredefinidasPorNivel(@PathVariable String nivel) {
        if (nivel == null || nivel.trim().isEmpty()) {
            throw new InvalidDataException("El nivel no puede estar vacío");
        }

        List<RutinaDTO> rutinas = rutinaService.findPredefinidasByNivel(nivel);

        if (rutinas.isEmpty()) {
            throw new NotFoundEntityException("No se encontraron rutinas predefinidas para el nivel " + nivel);
        }

        return ResponseEntity.ok(rutinas);
    }

    @Operation(summary = "Actualiza parcialmente una rutina")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rutina actualizada",
                    content = @Content(schema = @Schema(implementation = RutinaDTO.class))),
            @ApiResponse(responseCode = "404", description = "Rutina no encontrada",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    // Actualiza parcialmente campos de una rutina (PATCH)
    @PatchMapping("/rutinas/{id}")
    public ResponseEntity<RutinaDTO> patchRutina(@PathVariable Integer id, @RequestBody RutinaPatchDTO patchDTO) {
        return ResponseEntity.ok(rutinaService.patch(id, patchDTO));
    }
}