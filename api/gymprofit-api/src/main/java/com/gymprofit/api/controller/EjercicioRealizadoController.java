package com.gymprofit.api.controller;

import com.gymprofit.api.dto.common.CountDTO;
import com.gymprofit.api.dto.common.ExistsDTO;
import com.gymprofit.api.dto.entity.ejerciciorealizado.EjercicioRealizadoCreateDTO;
import com.gymprofit.api.dto.entity.ejerciciorealizado.EjercicioRealizadoDTO;
import com.gymprofit.api.dto.entity.ejerciciorealizado.EjercicioRealizadoPatchDTO;
import com.gymprofit.api.exceptions.NotFoundEntityException;
import com.gymprofit.api.exceptions.Response;
import com.gymprofit.api.service.ejerciciorealizado.IEjercicioRealizadoService;
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
// EjercicioRealizadoController — controlador REST de ejercicios realizados
// Gestiona el CRUD de los ejercicios efectivamente ejecutados dentro de una
// sesión de entrenamiento (series, repeticiones, etc.), incluyendo consultas
// por sesión/ejercicio, conteos y comprobaciones de existencia.
// ============================================================
@RestController
@RequestMapping("")
@AllArgsConstructor
@Tag(name = "EjercicioRealizado Controlador", description = "Gestión de los ejercicios realizados en sesiones")
public class EjercicioRealizadoController {

    private final IEjercicioRealizadoService ejercicioRealizadoService;

    // Devuelve todos los ejercicios realizados registrados
    @Operation(summary = "Obtiene todos los ejercicios realizados")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Listado de ejercicios realizados",
                    content = @Content(schema = @Schema(implementation = EjercicioRealizadoDTO.class))),
            @ApiResponse(responseCode = "404", description = "No se encontraron ejercicios realizados",
                    content = @Content(schema = @Schema(implementation = Response.class))),
            @ApiResponse(responseCode = "500", description = "Error al obtener los ejercicios realizados",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping("/ejercicios-realizados")
    public ResponseEntity<List<EjercicioRealizadoDTO>> findAll() {
        List<EjercicioRealizadoDTO> ejerciciosRealizados = ejercicioRealizadoService.findAll();

        return ResponseEntity.ok(ejerciciosRealizados);
    }

    @Operation(summary = "Obtiene un ejercicio realizado por ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ejercicio realizado encontrado",
                    content = @Content(schema = @Schema(implementation = EjercicioRealizadoDTO.class))),
            @ApiResponse(responseCode = "404", description = "Ejercicio realizado no encontrado",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    // Busca un ejercicio realizado por su ID
    @GetMapping("/ejercicios-realizados/{id}")
    public ResponseEntity<EjercicioRealizadoDTO> obtenerEjercicioRealizado(@PathVariable Integer id) {
        EjercicioRealizadoDTO ejercicioRealizado = ejercicioRealizadoService.findById(id);

        return ResponseEntity.ok(ejercicioRealizado);
    }

    @Operation(summary = "Crea un ejercicio realizado")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ejercicio realizado creado correctamente",
                    content = @Content(schema = @Schema(implementation = EjercicioRealizadoDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    // Registra un nuevo ejercicio realizado dentro de una sesión
    @PostMapping("/ejercicios-realizados")
    public ResponseEntity<EjercicioRealizadoDTO> guardarEjercicioRealizado(@Valid @RequestBody EjercicioRealizadoCreateDTO ejercicioRealizadoCreateDTO) {
        EjercicioRealizadoDTO ejercicioRealizado = ejercicioRealizadoService.save(ejercicioRealizadoCreateDTO);

        return ResponseEntity.ok(ejercicioRealizado);
    }

    @Operation(summary = "Modifica un ejercicio realizado existente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ejercicio realizado modificado correctamente",
                    content = @Content(schema = @Schema(implementation = EjercicioRealizadoDTO.class))),
            @ApiResponse(responseCode = "404", description = "Ejercicio realizado no encontrado",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    // Actualiza completamente un ejercicio realizado existente
    @PutMapping("/ejercicios-realizados")
    public ResponseEntity<EjercicioRealizadoDTO> modificarEjercicioRealizado(@Valid @RequestBody EjercicioRealizadoDTO ejercicioRealizadoDTO) {
        EjercicioRealizadoDTO ejercicioRealizado = ejercicioRealizadoService.modify(ejercicioRealizadoDTO);

        return ResponseEntity.ok(ejercicioRealizado);
    }

    @Operation(summary = "Elimina un ejercicio realizado por ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ejercicio realizado eliminado correctamente"),
            @ApiResponse(responseCode = "404", description = "Ejercicio realizado no encontrado",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    // Elimina un ejercicio realizado por ID, capturando errores en la respuesta
    @DeleteMapping("/ejercicios-realizados/{id}")
    public ResponseEntity<Map<String, Object>> borrarEjercicioRealizado(@PathVariable Integer id) {
        Map<String, Object> respuesta = new HashMap<>();

        ejercicioRealizadoService.deleteById(id);

        respuesta.put("mensaje", "Ejercicio realizado eliminado con ÉXITO");

        return new ResponseEntity<>(respuesta, HttpStatus.OK);
    }

    @Operation(summary = "Obtiene todos los ejercicios realizados de una sesión")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ejercicios realizados encontrados",
                    content = @Content(schema = @Schema(implementation = EjercicioRealizadoDTO.class))),
            @ApiResponse(responseCode = "404", description = "No se encontraron ejercicios realizados para esa sesión",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    // Lista los ejercicios realizados de una sesión concreta
    @GetMapping("/ejercicios-realizados/sesion/{sesionId}")
    public ResponseEntity<List<EjercicioRealizadoDTO>> obtenerPorSesion(@PathVariable Integer sesionId) {
        List<EjercicioRealizadoDTO> ejerciciosRealizados = ejercicioRealizadoService.findBySesionId(sesionId);

        if (ejerciciosRealizados.isEmpty()) {
            throw new NotFoundEntityException("No se encontraron ejercicios realizdos para la sesión con id " + sesionId);
        }

        return ResponseEntity.ok(ejerciciosRealizados);
    }

    @Operation(summary = "Obtiene todos los ejercicios realizados de un ejercicio")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ejercicios realizados encontrados",
                    content = @Content(schema = @Schema(implementation = EjercicioRealizadoDTO.class))),
            @ApiResponse(responseCode = "404", description = "No se encontraron ejercicios realizados para ese ejercicio",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    // Lista todas las ejecuciones registradas de un ejercicio concreto
    @GetMapping("/ejercicios-realizados/ejercicio/{ejercicioId}")
    public ResponseEntity<List<EjercicioRealizadoDTO>> obtenerPorEjercicio(@PathVariable Integer ejercicioId) {
        List<EjercicioRealizadoDTO> ejerciciosRealizados = ejercicioRealizadoService.findByEjercicioId(ejercicioId);

        if (ejerciciosRealizados.isEmpty()) {
            throw new NotFoundEntityException("No se encontraron ejercicios realizados para el ejercicio con id " + ejercicioId);
        }

        return ResponseEntity.ok(ejerciciosRealizados);
    }

    @Operation(summary = "Obtiene ejercicios realizados por sesión y ejercicio")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ejercicios realizados encontrados",
                    content = @Content(schema = @Schema(implementation = EjercicioRealizadoDTO.class))),
            @ApiResponse(responseCode = "404", description = "No se encontraron ejercicios realizados para esa sesión y ejercicio",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    // Lista las ejecuciones de un ejercicio concreto dentro de una sesión concreta
    @GetMapping("/ejercicios-realizados/sesion/{sesionId}/ejercicio/{ejercicioId}")
    public ResponseEntity<List<EjercicioRealizadoDTO>> obtenerPorSesionYEjercicio(@PathVariable Integer sesionId,
                                                                                  @PathVariable Integer ejercicioId) {
        List<EjercicioRealizadoDTO> ejerciciosRealizados = ejercicioRealizadoService.findBySesionIdAndEjercicioId(sesionId, ejercicioId);

        if (ejerciciosRealizados.isEmpty()) {
            throw new NotFoundEntityException("No se encontraron ejercicios realizados para la sesión " + sesionId + " y ejercicio " + ejercicioId);
        }

        return ResponseEntity.ok(ejerciciosRealizados);
    }

    @Operation(summary = "Cuenta los ejercicios realizados de una sesión")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Total de ejercicios realizados en la sesión"),
            @ApiResponse(responseCode = "500", description = "Error al contar los ejercicios realizados",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    // Cuenta cuántos ejercicios se han realizado en una sesión
    @GetMapping("/ejercicios-realizados/count/sesion/{sesionId}")
    public ResponseEntity<CountDTO> contarSesiones(@PathVariable Integer sesionId) {
        Long count = ejercicioRealizadoService.countBySesionId(sesionId);

        return ResponseEntity.ok(new CountDTO(count));
    }

    @Operation(summary = "Cuenta los ejercicios realizados de un ejercicio")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Total de veces que se ha realizado el ejercicio"),
            @ApiResponse(responseCode = "500", description = "Error al contar los ejercicios realizados",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    // Cuenta cuántas veces se ha realizado un ejercicio concreto
    @GetMapping("/ejercicios-realizados/count/ejercicio/{ejercicioId}")
    public ResponseEntity<CountDTO> contarEjercicios(@PathVariable Integer ejercicioId) {
        Long count = ejercicioRealizadoService.countByEjercicioId(ejercicioId);

        return ResponseEntity.ok(new CountDTO(count));
    }

    @Operation(summary = "Verifica si existe un ejercicio realizado para una sesión y un ejercicio concretos")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Resultado de la verificación"),
            @ApiResponse(responseCode = "500", description = "Error al verificar la existencia",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    // Comprueba si existe un registro para la combinación sesión-ejercicio dada
    @GetMapping("/ejercicios-realizados/exists/sesion/{sesionId}/ejercicio/{ejercicioId}")
    public ResponseEntity<ExistsDTO> existsBySesionAndEjercicio(@PathVariable Integer sesionId,
                                                                @PathVariable Integer ejercicioId) {
        boolean existe = ejercicioRealizadoService.existsBySesionIdAndEjercicioId(sesionId, ejercicioId);

        return ResponseEntity.ok(new ExistsDTO(existe));
    }

    @Operation(summary = "Elimina todos los ejercicios realizados de una sesión")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ejercicios realizados eliminados correctamente"),
            @ApiResponse(responseCode = "404", description = "Sesión no encontrada",
                    content = @Content(schema = @Schema(implementation = Response.class))),
            @ApiResponse(responseCode = "500", description = "Error al eliminar los ejercicios realizados",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    // Elimina todos los ejercicios realizados asociados a una sesión
    @DeleteMapping("/ejercicios-realizados/sesion/{sesionId}")
    public ResponseEntity<Map<String, Object>> borrarPorSesion(@PathVariable Integer sesionId) {
        Map<String, Object> respuesta = new HashMap<>();

        ejercicioRealizadoService.deleteBySesionId(sesionId);

        respuesta.put("mensaje", "Ejercicios realizados de la sesión " + sesionId + " eliminados con ÉXITO");

        return new ResponseEntity<>(respuesta, HttpStatus.OK);
    }

    @Operation(summary = "Elimina ejercicios realizados de una sesión y ejercicio concretos")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ejercicios realizados eliminados correctamente"),
            @ApiResponse(responseCode = "404", description = "Sesión o ejercicio no encontrado",
                    content = @Content(schema = @Schema(implementation = Response.class))),
            @ApiResponse(responseCode = "500", description = "Error al eliminar los ejercicios realizados",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    // Elimina los registros de un ejercicio concreto dentro de una sesión concreta
    @DeleteMapping("/ejercicios-realizados/sesion/{sesionId}/ejercicio/{ejercicioId}")
    public ResponseEntity<Map<String, Object>> borrarPorSesionYEjercicio(@PathVariable Integer sesionId,
                                                                         @PathVariable Integer ejercicioId) {
        Map<String, Object> respuesta = new HashMap<>();

        ejercicioRealizadoService.deleteBySesionIdAndEjercicioId(sesionId, ejercicioId);

        respuesta.put("mensaje", "Ejercicios realizados de la sesión " + sesionId + " y ejercicio " + ejercicioId + " eliminados con ÉXITO");

        return new ResponseEntity<>(respuesta, HttpStatus.OK);
    }

    @Operation(summary = "Actualiza parcialmente un ejercicio realizado")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ejercicio realizado actualizado",
                    content = @Content(schema = @Schema(implementation = EjercicioRealizadoDTO.class))),
            @ApiResponse(responseCode = "404", description = "Ejercicio realizado no encontrado",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    // Actualiza parcialmente campos de un ejercicio realizado (PATCH)
    @PatchMapping("/ejercicios-realizados/{id}")
    public ResponseEntity<EjercicioRealizadoDTO> patchEjercicioRealizado(@PathVariable Integer id, @RequestBody EjercicioRealizadoPatchDTO patchDTO) {
        return ResponseEntity.ok(ejercicioRealizadoService.patch(id, patchDTO));
    }
}