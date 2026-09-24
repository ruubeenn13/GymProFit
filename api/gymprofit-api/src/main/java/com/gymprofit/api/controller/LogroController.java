package com.gymprofit.api.controller;

import com.gymprofit.api.dto.entity.logro.LogroCreateDTO;
import com.gymprofit.api.dto.entity.logro.LogroDTO;
import com.gymprofit.api.dto.entity.logro.LogroProgresoDTO;
import com.gymprofit.api.dto.entity.logro.UsuarioLogroDTO;
import com.gymprofit.api.exceptions.Response;
import com.gymprofit.api.service.logro.ILogroService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// ============================================================
// LogroController — controlador REST del sistema de logros (achievements)
// Permite consultar el catálogo de logros disponibles, ver los logros
// obtenidos por un usuario concreto, y crear/actualizar logros (ADMIN).
// ============================================================
@RestController
@RequestMapping("/logros")
@RequiredArgsConstructor
@Tag(name = "Logros", description = "Gestión del sistema de logros")
public class LogroController {

    private final ILogroService logroService;

    // Devuelve el catálogo completo de logros disponibles
    @Operation(summary = "Obtiene todos los logros disponibles")
    @ApiResponse(responseCode = "200", description = "Listado de logros",
            content = @Content(schema = @Schema(implementation = LogroDTO.class)))
    @GetMapping
    public ResponseEntity<List<LogroDTO>> findAll() {
        return ResponseEntity.ok(logroService.findAll());
    }

    @Operation(summary = "Obtiene los logros obtenidos por un usuario")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Logros del usuario",
                    content = @Content(schema = @Schema(implementation = UsuarioLogroDTO.class))),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    // Devuelve los logros que ha conseguido un usuario concreto
    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<UsuarioLogroDTO>> findByUsuarioId(@PathVariable Integer usuarioId) {
        return ResponseEntity.ok(logroService.findByUsuarioId(usuarioId));
    }

    @Operation(summary = "Logros del usuario autenticado con su progreso",
            description = "Un elemento por logro del catálogo: si está conseguido y cuándo, y si no, "
                    + "la métrica, el umbral y lo que lleva el usuario. El usuario sale del token: "
                    + "la ruta no recibe id.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Catálogo con el progreso del usuario",
                    content = @Content(schema = @Schema(implementation = LogroProgresoDTO.class))),
            @ApiResponse(responseCode = "403", description = "Token de invitado: no hay progreso que dar",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    // Catálogo con el progreso del usuario del token (GP-079)
    @GetMapping("/progreso")
    public ResponseEntity<List<LogroProgresoDTO>> progreso() {
        return ResponseEntity.ok(logroService.progresoDelUsuarioActual());
    }

    @Operation(summary = "Crea un nuevo logro (ADMIN)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Logro creado",
                    content = @Content(schema = @Schema(implementation = LogroDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    // Crea un nuevo logro en el catálogo (solo ADMIN)
    @PostMapping
    public ResponseEntity<LogroDTO> save(@Valid @RequestBody LogroCreateDTO createDTO) {
        return new ResponseEntity<>(logroService.save(createDTO), HttpStatus.CREATED);
    }

    @Operation(summary = "Actualiza un logro existente (ADMIN)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Logro actualizado",
                    content = @Content(schema = @Schema(implementation = LogroDTO.class))),
            @ApiResponse(responseCode = "404", description = "Logro no encontrado",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    // Actualiza un logro existente del catálogo (solo ADMIN)
    @PutMapping("/{id}")
    public ResponseEntity<LogroDTO> update(@PathVariable Integer id, @Valid @RequestBody LogroCreateDTO updateDTO) {
        return ResponseEntity.ok(logroService.update(id, updateDTO));
    }
}
