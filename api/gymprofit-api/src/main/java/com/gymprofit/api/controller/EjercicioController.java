package com.gymprofit.api.controller;

import com.gymprofit.api.dto.entity.ejercicio.EjercicioCreateDTO;
import com.gymprofit.api.dto.entity.ejercicio.EjercicioDTO;
import com.gymprofit.api.exceptions.Response;
import com.gymprofit.api.service.ejercicio.IEjercicioService;
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

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api")
@AllArgsConstructor
@Tag(name = "Ejercicio Controlador", description = "Gestión de los ejercicios")
public class EjercicioController {

    private final IEjercicioService ejercicioService;

    @Operation(summary = "Obtiene todos los ejercicios")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Listado de ejercicios",
                    content = @Content(schema = @Schema(implementation = EjercicioDTO.class))),
            @ApiResponse(responseCode = "404", description = "Ejercicios no encontrados",
                    content = @Content(schema = @Schema(implementation = Response.class))),
            @ApiResponse(responseCode = "500", description = "Error al obtener los ejercicios",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping("/ejercicios")
    public ResponseEntity<List<EjercicioDTO>> findAll() {
        List<EjercicioDTO> ejercicios = ejercicioService.findAll();

        return ResponseEntity.ok(ejercicios);
    }

    @Operation(summary = "Obtiene un ejercicio por ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ejercicio encontrado",
                    content = @Content(schema = @Schema(implementation = EjercicioDTO.class))),
            @ApiResponse(responseCode = "400", description = "Ejercicio no encontrado",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping("/ejercicios/{id}")
    public ResponseEntity<EjercicioDTO> obtenerEjercicio(@PathVariable Integer id) {
        EjercicioDTO ejercicio = ejercicioService.findById(id);

        return ResponseEntity.ok(ejercicio);
    }

    @Operation(summary = "Crea un ejercicio")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ejercicio creado correctamente",
                    content = @Content(schema = @Schema(implementation = EjercicioDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @PostMapping("/ejercicios")
    public ResponseEntity<EjercicioDTO> guardarEjercicio(@Valid @RequestBody EjercicioCreateDTO ejercicioCreateDTO) {
        EjercicioDTO ejercicio = ejercicioService.save(ejercicioCreateDTO);

        return ResponseEntity.ok(ejercicio);
    }

    @Operation(summary = "Modifica un ejercicio existente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ejercicio modificado correctamente",
                    content = @Content(schema = @Schema(implementation = EjercicioDTO.class))),
            @ApiResponse(responseCode = "404", description = "Ejercicio no encontrado",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @PutMapping("/ejercicios")
    public ResponseEntity<EjercicioDTO> modificarEjercicio(@Valid @RequestBody EjercicioDTO ejercicioDTO) {
        EjercicioDTO ejercicio = ejercicioService.modify(ejercicioDTO);

        return ResponseEntity.ok(ejercicio);
    }

    @Operation(summary = "Desactiva un ejercicio")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ejercicio desactivado correctamente"),
            @ApiResponse(responseCode = "404", description = "Ejercicio no encontrado",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @DeleteMapping("/ejercicios/{id}")
    public ResponseEntity<Map<String, Object>> borrarEjercicio(@PathVariable Integer id) {
        Map<String, Object> respuesta = new HashMap<>();

        try {
            ejercicioService.deleteById(id);

            respuesta.put("mensaje", "Ejercicio desactivado con ÉXITO");
        } catch (Exception e) {
            respuesta.put("mensaje", "Error al desactivar el ejercicio " + id);
            respuesta.put("error", e.getMessage());

            return new ResponseEntity<>(respuesta, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(respuesta, HttpStatus.OK);
    }

    @Operation(summary = "Activa un ejercicio desactivado")
    @PutMapping("/ejercicios/{id}/activar")
    public ResponseEntity<Map<String, Object>> activarEjercicio(@PathVariable Integer id) {
        Map<String, Object> respuesta = new HashMap<>();

        try {
            ejercicioService.activateById(id);

            respuesta.put("mensaje", "Ejercicio activado con ÉXITO");
        } catch (Exception e) {
            respuesta.put("mensaje", "Error al activar el ejercicio " + id);
            respuesta.put("error", e.getMessage());

            return new ResponseEntity<>(respuesta, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(respuesta, HttpStatus.OK);
    }

    @Operation(summary = "Elimina permanentemente un ejercicio de la base de datos")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ejercicio eliminado permanentemente"),
            @ApiResponse(responseCode = "404", description = "Ejercicio no encontrado",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @DeleteMapping("/ejercicios/{id}/permanente")
    public ResponseEntity<Map<String, Object>> eliminarPermanente(@PathVariable Integer id) {
        Map<String, Object> respuesta = new HashMap<>();

        try {
            ejercicioService.permanentDeleteById(id);

            respuesta.put("mensaje", "Ejercicio eliminado PERMANENTEMENTE con ÉXITO");
        } catch (Exception e) {
            respuesta.put("mensaje", "Error al eliminar permanentemente el ejercicio " + id);
            respuesta.put("error", e.getMessage());

            return new ResponseEntity<>(respuesta, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(respuesta, HttpStatus.OK);
    }

    @Operation(summary = "Busca ejercicios por grupo muscular")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ejercicios encontrados",
                    content = @Content(schema = @Schema(implementation = EjercicioDTO.class)))
    })
    @GetMapping("/ejercicios/grupo/{grupoMuscular}")
    public ResponseEntity<List<EjercicioDTO>> obtenerEjerciciosPorGrupo(@PathVariable String grupoMuscular) {
        List<EjercicioDTO> ejercicios = ejercicioService.findByGrupoMuscular(grupoMuscular);

        return ResponseEntity.ok(ejercicios);
    }

    @Operation(summary = "Busca ejercicios por dificultad")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ejercicios encontrados",
                    content = @Content(schema = @Schema(implementation = EjercicioDTO.class)))
    })
    @GetMapping("/ejercicios/dificultad/{dificultad}")
    public ResponseEntity<List<EjercicioDTO>> obtenerEjerciciosPorDificultad(@PathVariable String dificultad) {
        List<EjercicioDTO> ejercicios = ejercicioService.findByDificultad(dificultad);

        return ResponseEntity.ok(ejercicios);
    }

    @Operation(summary = "Busca ejercicios por nombre")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ejercicios encontrados",
                    content = @Content(schema = @Schema(implementation = EjercicioDTO.class)))
    })
    @GetMapping("/ejercicios/nombre/{nombre}")
    public ResponseEntity<List<EjercicioDTO>> obtenerEjerciciosPorNombre(@PathVariable String nombre) {
        List<EjercicioDTO> ejercicios = ejercicioService.findByNombre(nombre);

        return ResponseEntity.ok(ejercicios);
    }

    @Operation(summary = "Obtiene todos los ejercicios activos")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Listado de ejercicios",
                    content = @Content(schema = @Schema(implementation = EjercicioDTO.class)))
    })
    @GetMapping("/ejercicios/activos")
    public ResponseEntity<List<EjercicioDTO>> obtenerEjerciciosActivos() {
        List<EjercicioDTO> ejercicios = ejercicioService.findActivos();

        return ResponseEntity.ok(ejercicios);
    }
}
