package com.gymprofit.api.controller;

import com.gymprofit.api.dto.common.CountDTO;
import com.gymprofit.api.dto.common.ExistsDTO;
import com.gymprofit.api.dto.entity.rutinaejercicio.RutinaEjercicioCreateDTO;
import com.gymprofit.api.dto.entity.rutinaejercicio.RutinaEjercicioDTO;
import com.gymprofit.api.dto.entity.rutinaejercicio.RutinaEjercicioPatchDTO;
import com.gymprofit.api.exceptions.NotFoundEntityException;
import com.gymprofit.api.exceptions.Response;
import com.gymprofit.api.service.rutinaejercicio.IRutinaEjercicioService;
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
// RutinaEjercicioController — controlador REST de la relación rutina-ejercicio
// Expone endpoints CRUD y de consulta para los ejercicios que componen cada
// rutina (orden, conteos, existencia). Es la tabla intermedia entre
// Rutina y Ejercicio dentro de GymProFit.
// ============================================================
@RestController
@RequestMapping("")
@AllArgsConstructor
@Tag(name = "RutinaEjercicio Controlador", description = "Gestión de los ejercicios asociados a rutinas")
public class RutinaEjercicioController {

    // Servicio con la lógica de negocio de rutina-ejercicio
    private final IRutinaEjercicioService rutinaEjercicioService;

    @Operation(summary = "Obtiene todos los ejercicios de las rutinas")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Listado de ejercicios de las rutinas",
                    content = @Content(schema = @Schema(implementation = RutinaEjercicioDTO.class))),
            @ApiResponse(responseCode = "404", description = "No se encontraron ejercicios en las rutinas",
                    content = @Content(schema = @Schema(implementation = Response.class))),
            @ApiResponse(responseCode = "500", description = "Error al obtener los ejercicios de las rutinas",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    // Devuelve todas las relaciones rutina-ejercicio existentes
    @GetMapping("/rutinas-ejercicios")
    public ResponseEntity<List<RutinaEjercicioDTO>> findAll() {
        List<RutinaEjercicioDTO> rutinasEjercicios = rutinaEjercicioService.findAll();

        return ResponseEntity.ok(rutinasEjercicios);
    }

    @Operation(summary = "Obtiene un ejercicio de una rutina por el ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ejercicio de rutina encontrado",
                    content = @Content(schema = @Schema(implementation = RutinaEjercicioDTO.class))),
            @ApiResponse(responseCode = "404", description = "Ejercicio de rutina no encontrado",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    // Busca una relación rutina-ejercicio por su ID
    @GetMapping("/rutinas-ejercicios/{id}")
    public ResponseEntity<RutinaEjercicioDTO> obtenerRutinaEjercicio(@PathVariable Integer id) {
        RutinaEjercicioDTO rutinaEjercicioDTO = rutinaEjercicioService.findById(id);

        return ResponseEntity.ok(rutinaEjercicioDTO);
    }

    @Operation(summary = "Añade un ejercicio a una rutina")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ejercicio añadido a la rutina correctamente",
                    content = @Content(schema = @Schema(implementation = RutinaEjercicioDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(schema = @Schema(implementation = Response.class))),
            @ApiResponse(responseCode = "404", description = "Rutina o ejercicio no encontrado",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    // Crea una nueva asociación entre una rutina y un ejercicio
    @PostMapping("/rutinas-ejercicios")
    public ResponseEntity<RutinaEjercicioDTO> guardarRutinaEjercicio(@Valid @RequestBody RutinaEjercicioCreateDTO rutinaEjercicioCreateDTO) {
        RutinaEjercicioDTO rutinaEjercicioDTO = rutinaEjercicioService.save(rutinaEjercicioCreateDTO);

        return ResponseEntity.ok(rutinaEjercicioDTO);
    }

    @Operation(summary = "Modifica un ejercicio de una rutina existente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ejercicio de rutina modificado correctamente",
                    content = @Content(schema = @Schema(implementation = RutinaEjercicioDTO.class))),
            @ApiResponse(responseCode = "404", description = "Ejercicio de rutina no encontrado",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    // Actualiza por completo una relación rutina-ejercicio existente
    @PutMapping("/rutinas-ejercicios")
    public ResponseEntity<RutinaEjercicioDTO> modificarRutinaEjercicio(@Valid @RequestBody RutinaEjercicioDTO rutinaEjercicioDTO) {
        RutinaEjercicioDTO actualizado = rutinaEjercicioService.modify(rutinaEjercicioDTO);

        return ResponseEntity.ok(actualizado);
    }

    @Operation(summary = "Elimina un ejercicio de la rutina por ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ejercicio de la rutina eliminado correctamente"),
            @ApiResponse(responseCode = "404", description = "Ejercicio de la rutina no encontrado",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    // Elimina un ejercicio de una rutina por su ID de relación
    @DeleteMapping("/rutinas-ejercicios/{id}")
    public ResponseEntity<Map<String, Object>> borrarRutinaEjercicio(@PathVariable Integer id) {
        Map<String, Object> respuesta = new HashMap<>();

        rutinaEjercicioService.deleteById(id);

        respuesta.put("mensaje", "Ejercicio de la rutina eliminado con ÉXITO");

        return new ResponseEntity<>(respuesta, HttpStatus.OK);
    }

    @Operation(summary = "Obtiene todos los ejercicios de una rutina")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ejercicios encontrados",
                    content = @Content(schema = @Schema(implementation = RutinaEjercicioDTO.class))),
            @ApiResponse(responseCode = "404", description = "No se encontraron ejercicios para esta rutina",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    // Lista los ejercicios pertenecientes a una rutina concreta
    @GetMapping("/rutinas-ejercicios/rutina/{rutinaId}")
    public ResponseEntity<List<RutinaEjercicioDTO>> obtenerPorRutina(@PathVariable Integer rutinaId) {
        List<RutinaEjercicioDTO> ejerciciosRutinas = rutinaEjercicioService.findByRutinaId(rutinaId);

        if (ejerciciosRutinas.isEmpty()) {
            throw new NotFoundEntityException("No se encontraron ejercicios para la rutina con id" + rutinaId);
        }

        return ResponseEntity.ok(ejerciciosRutinas);
    }

    @Operation(summary = "Obtiene todos los ejercicios de una rutina ordenados por posición")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ejercicios encontrados ordenados",
                    content = @Content(schema = @Schema(implementation = RutinaEjercicioDTO.class))),
            @ApiResponse(responseCode = "404", description = "No se encontraron ejercicios para esta rutina",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    // Lista los ejercicios de una rutina respetando el orden de ejecución
    @GetMapping("/rutinas-ejercicios/rutina/{rutinaId}/ordenados")
    public ResponseEntity<List<RutinaEjercicioDTO>> obtenerPorRutinaOrdenado(@PathVariable Integer rutinaId) {
        List<RutinaEjercicioDTO> ejerciciosRutinas = rutinaEjercicioService.findByRutinaIdOrdenado(rutinaId);

        if (ejerciciosRutinas.isEmpty()) {
            throw new NotFoundEntityException("No se encontraron ejercicios para la rutina con id " + rutinaId);
        }

        return ResponseEntity.ok(ejerciciosRutinas);
    }

    @Operation(summary = "Obtiene las rutinas que contienen un ejercicio")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rutinas encontradas",
                    content = @Content(schema = @Schema(implementation = RutinaEjercicioDTO.class))),
            @ApiResponse(responseCode = "404", description = "No se encontraron rutinas con este ejercicio",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    // Lista las rutinas en las que aparece un ejercicio dado
    @GetMapping("/rutinas-ejercicios/ejercicio/{ejercicioId}")
    public ResponseEntity<List<RutinaEjercicioDTO>> obtenerPorEjercicio(@PathVariable Integer ejercicioId) {
        List<RutinaEjercicioDTO> ejerciciosRutinas = rutinaEjercicioService.findByEjercicioId(ejercicioId);

        if (ejerciciosRutinas.isEmpty()) {
            throw new NotFoundEntityException("No se encontraron rutinas con el ejercicio " + ejercicioId);
        }

        return ResponseEntity.ok(ejerciciosRutinas);
    }

    @Operation(summary = "Obtiene un ejercicio específico de una rutina")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ejercicio encontrado de la rutina",
                    content = @Content(schema = @Schema(implementation = RutinaEjercicioDTO.class))),
            @ApiResponse(responseCode = "404", description = "No existe ese ejercicio en la rutina",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping("/rutinas-ejercicios/rutina/{rutinaId}/ejercicio/{ejercicioId}")
    // Busca la relación concreta entre una rutina y un ejercicio determinados
    public ResponseEntity<RutinaEjercicioDTO> obtenerPorRutinaYEjercicio(@PathVariable Integer rutinaId,
                                                                         @PathVariable Integer ejercicioId) {
        RutinaEjercicioDTO rutinaEjercicioDTO = rutinaEjercicioService.findByRutinaIdAndEjercicioId(rutinaId, ejercicioId);

        return ResponseEntity.ok(rutinaEjercicioDTO);
    }

    @Operation(summary = "Cuenta los ejercicios de una rutina")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Total de ejercicios en la rutina")
    })
    // Cuenta cuántos ejercicios tiene asignados una rutina
    @GetMapping("/rutinas-ejercicios/count/rutina/{rutinaId}")
    public ResponseEntity<CountDTO> contarEjerciciosRutina(@PathVariable Integer rutinaId) {
        Long count = rutinaEjercicioService.countByRutinaId(rutinaId);

        return ResponseEntity.ok(new CountDTO(count));
    }

    @Operation(summary = "Cuenta las rutinas que contienen un ejercicio")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Total de rutinas con el ejercicio")
    })
    // Cuenta en cuántas rutinas aparece un ejercicio
    @GetMapping("/rutinas-ejercicios/count/ejercicio/{ejercicioId}")
    public ResponseEntity<CountDTO> contarRutinasEjercicios(@PathVariable Integer ejercicioId) {
        Long count = rutinaEjercicioService.countByEjercicioId(ejercicioId);

        return ResponseEntity.ok(new CountDTO(count));
    }

    @Operation(summary = "Verifica si un ejercicio existe en una rutina")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Resultado de la verificación")
    })
    @GetMapping("/rutinas-ejercicios/exists/rutina/{rutinaId}/ejercicio/{ejercicioId}")
    // Comprueba si existe la relación entre una rutina y un ejercicio
    public ResponseEntity<ExistsDTO> existsByRutinaIdAndEjercicioId(@PathVariable Integer rutinaId,
                                                                    @PathVariable Integer ejercicioId) {
        boolean existe = rutinaEjercicioService.existsByRutinaIdAndEjercicioId(rutinaId, ejercicioId);

        return ResponseEntity.ok(new ExistsDTO(existe));
    }

    @Operation(summary = "Elimina los ejercicios de una rutina")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ejercicios de la rutina eliminados correctamente"),
            @ApiResponse(responseCode = "500", description = "Error al eliminar los ejercicios de la rutina",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    // Elimina todos los ejercicios asociados a una rutina (limpieza masiva)
    @DeleteMapping("/rutinas-ejercicios/rutina/{rutinaId}")
    public ResponseEntity<Map<String, Object>> borrarPorRutina(@PathVariable Integer rutinaId) {
        Map<String, Object> respuesta = new HashMap<>();

        rutinaEjercicioService.deleteByRutinaId(rutinaId);

        respuesta.put("mensaje", "Ejercicios de la rutina " + rutinaId + " eliminados con ÉXITO");

        return new ResponseEntity<>(respuesta, HttpStatus.OK);
    }

    @Operation(summary = "Elimina un ejercicio específico de una rutina")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ejercicio eliminado de la rutina correctamente"),
            @ApiResponse(responseCode = "500", description = "Error al eliminar el ejercicio de la rutina",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @DeleteMapping("/rutinas-ejercicios/rutina/{rutinaId}/ejercicio/{ejercicioId}")
    // Elimina un ejercicio concreto de una rutina concreta
    public ResponseEntity<Map<String, Object>> borrarPorRutinaYEjercicio(@PathVariable Integer rutinaId,
                                                                         @PathVariable Integer ejercicioId) {
        Map<String, Object> respuesta = new HashMap<>();

        rutinaEjercicioService.deleteByRutinaIdAndEjercicioId(rutinaId, ejercicioId);

        respuesta.put("mensaje", "Ejercicio " + ejercicioId + " eliminado de la rutina " + rutinaId + " con ÉXITO");

        return new ResponseEntity<>(respuesta, HttpStatus.OK);
    }

    @Operation(summary = "Actualiza parcialmente un ejercicio de una rutina")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ejercicio de rutina actualizado",
                    content = @Content(schema = @Schema(implementation = RutinaEjercicioDTO.class))),
            @ApiResponse(responseCode = "404", description = "Ejercicio de rutina no encontrado",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    // Actualización parcial de campos de una relación rutina-ejercicio
    @PatchMapping("/rutinas-ejercicios/{id}")
    public ResponseEntity<RutinaEjercicioDTO> patchRutinaEjercicio(@PathVariable Integer id, @RequestBody RutinaEjercicioPatchDTO patchDTO) {
        return ResponseEntity.ok(rutinaEjercicioService.patch(id, patchDTO));
    }
}
