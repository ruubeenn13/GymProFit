package com.gymprofit.api.controller;

import com.gymprofit.api.dto.entity.progresoejercicio.ProgresoEjercicioCreateDTO;
import com.gymprofit.api.dto.entity.progresoejercicio.ProgresoEjercicioDTO;
import com.gymprofit.api.dto.entity.progresoejercicio.ProgresoEjercicioPatchDTO;
import com.gymprofit.api.exceptions.NotFoundEntityException;
import com.gymprofit.api.exceptions.Response;
import com.gymprofit.api.service.progresoejercicio.IProgresoEjercicioService;
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
@RequestMapping("")
@AllArgsConstructor
@Tag(name = "ProgresoEjercicio Controlador", description = "Gestión del progreso de los ejercicios de los usuarios")
public class ProgresoEjercicioController {

    private final IProgresoEjercicioService progresoEjercicioService;

    @Operation(summary = "Obtiene todos los progresos de ejercicios")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Listado de progresos",
                    content = @Content(schema = @Schema(implementation = ProgresoEjercicioDTO.class))),
            @ApiResponse(responseCode = "404", description = "No se encontraron progresos",
                    content = @Content(schema = @Schema(implementation = Response.class))),
            @ApiResponse(responseCode = "500", description = "Error al obtener los progresos",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping("/progreso-ejercicios")
    public ResponseEntity<List<ProgresoEjercicioDTO>> findAll() {
        List<ProgresoEjercicioDTO> progresoEjercicioDTOS = progresoEjercicioService.findAll();

        return ResponseEntity.ok(progresoEjercicioDTOS);
    }

    @Operation(summary = "Obtiene un progreso de ejercicio por ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Progreso encontrado",
                    content = @Content(schema = @Schema(implementation = ProgresoEjercicioDTO.class))),
            @ApiResponse(responseCode = "404", description = "Progreso no encontrado",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping("/progreso-ejercicios/{id}")
    public ResponseEntity<ProgresoEjercicioDTO> obtenerProgresoEjercicio(@PathVariable Integer id) {
        ProgresoEjercicioDTO progresoEjercicioDTO = progresoEjercicioService.findById(id);

        return ResponseEntity.ok(progresoEjercicioDTO);
    }

    @Operation(summary = "Registra un nuevo progreso de ejercicio")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Progreso de ejercicio registrado correctamente",
                    content = @Content(schema = @Schema(implementation = ProgresoEjercicioDTO.class))),
            @ApiResponse(responseCode = "404", description = "Usuario o ejercicio no encontrado",
                    content = @Content(schema = @Schema(implementation = Response.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @PostMapping("/progreso-ejercicios")
    public ResponseEntity<ProgresoEjercicioDTO> guardarProgresoEjercicio(@Valid @RequestBody ProgresoEjercicioCreateDTO progresoEjercicioCreateDTO) {
        ProgresoEjercicioDTO progresoEjercicioDTO = progresoEjercicioService.save(progresoEjercicioCreateDTO);

        return ResponseEntity.ok(progresoEjercicioDTO);
    }

    @Operation(summary = "Modifica un progreso de ejercicio existente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Progreso modificado correctamente",
                    content = @Content(schema = @Schema(implementation = ProgresoEjercicioDTO.class))),
            @ApiResponse(responseCode = "404", description = "Progreso no encontrado",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @PutMapping("/progreso-ejercicios")
    public ResponseEntity<ProgresoEjercicioDTO> modificarProgresoEjercicio(@Valid @RequestBody ProgresoEjercicioDTO progresoEjercicioDTO) {
        ProgresoEjercicioDTO progresoModificado = progresoEjercicioService.modify(progresoEjercicioDTO);

        return ResponseEntity.ok(progresoModificado);
    }

    @Operation(summary = "Elimina un progreso de ejercicio por ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Progreso eliminado correctamente"),
            @ApiResponse(responseCode = "404", description = "Progreso no encontrado")
    })
    @DeleteMapping("/progreso-ejercicios/{id}")
    public ResponseEntity<Map<String, Object>> borrarProgresoEjercicio(@PathVariable Integer id) {
        Map<String, Object> respuesta = new HashMap<>();

        try {
            progresoEjercicioService.deleteById(id);

            respuesta.put("mensaje", "Progreso de ejercicio eliminado con ÉXITO");
        } catch (Exception e) {
            respuesta.put("mensaje", "Error al eliminar el progreso con id " + id);
            respuesta.put("error", e.getMessage());

            return new ResponseEntity<>(respuesta, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(respuesta, HttpStatus.OK);
    }

    @Operation(summary = "Obtiene todos los progresos de un usuario")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Progresos encontrados",
                    content = @Content(schema = @Schema(implementation = ProgresoEjercicioDTO.class))),
            @ApiResponse(responseCode = "404", description = "No se encontraron progresos para este usuario",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping("/progreso-ejercicios/usuario/{usuarioId}")
    public ResponseEntity<List<ProgresoEjercicioDTO>> obtenerProgresoPorUsuario(@PathVariable Integer usuarioId) {
        List<ProgresoEjercicioDTO> progresoEjercicioDTOS = progresoEjercicioService.findByUsuarioId(usuarioId);

        if (progresoEjercicioDTOS.isEmpty()) {
            throw new NotFoundEntityException("No se encontraron progresos para el usuario con id " + usuarioId);
        }

        return ResponseEntity.ok(progresoEjercicioDTOS);
    }

    @Operation(summary = "Obtiene los progresos de un ejercicio")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Progresos encontrados",
                    content = @Content(schema = @Schema(implementation = ProgresoEjercicioDTO.class))),
            @ApiResponse(responseCode = "404", description = "No se encontraron progresos para este ejercicio",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping("/progreso-ejercicios/ejercicio/{ejercicioId}")
    public ResponseEntity<List<ProgresoEjercicioDTO>> obtenerProgresoPorEjercicio(@PathVariable Integer ejercicioId) {
        List<ProgresoEjercicioDTO> progresoEjercicioDTOS = progresoEjercicioService.findByEjercicioId(ejercicioId);

        if (progresoEjercicioDTOS.isEmpty()) {
            throw new NotFoundEntityException("No se encontraron progresos para el ejercicio con id " + ejercicioId);
        }

        return ResponseEntity.ok(progresoEjercicioDTOS);
    }

    @Operation(summary = "Obtiene los progresos de un usuario ordenados por fecha descendente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Progresos encontrados ordenados",
                    content = @Content(schema = @Schema(implementation = ProgresoEjercicioDTO.class))),
            @ApiResponse(responseCode = "404", description = "No se encontraron progresos para este usuario",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping("/progreso-ejercicios/usuario/{usuarioId}/ordenados")
    public ResponseEntity<List<ProgresoEjercicioDTO>> obtenerProgresoPorUsuarioOrdenado(@PathVariable Integer usuarioId) {
        List<ProgresoEjercicioDTO> progresoEjercicioDTOS = progresoEjercicioService.findByUsuarioIdOrdenado(usuarioId);

        if (progresoEjercicioDTOS.isEmpty()) {
            throw new NotFoundEntityException("No se encontraron progresos para el usuario con id " + usuarioId);
        }

        return ResponseEntity.ok(progresoEjercicioDTOS);
    }

    @Operation(summary = "Obtiene el progreso de un usuario en un ejercicio concreto")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Progresos encontrados",
                    content = @Content(schema = @Schema(implementation = ProgresoEjercicioDTO.class))),
            @ApiResponse(responseCode = "404", description = "No se encontraron progresos",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping("/progreso-ejercicios/usuario/{usuarioId}/ejercicio/{ejercicioId}")
    public ResponseEntity<List<ProgresoEjercicioDTO>> obtenerProgresoPorUsuarioYEjercicio(@PathVariable Integer usuarioId,
                                                                                          @PathVariable Integer ejercicioId) {
        List<ProgresoEjercicioDTO> progresoEjercicioDTOS = progresoEjercicioService.findByUsuarioIdAndEjercicioId(usuarioId, ejercicioId);

        if (progresoEjercicioDTOS.isEmpty()) {
            throw new NotFoundEntityException("No se encontraron progresos para el usuario " + usuarioId + " en el ejercicio " + ejercicioId);
        }

        return ResponseEntity.ok(progresoEjercicioDTOS);
    }

    @Operation(summary = "Obtiene el progreso de un usuario en un ejercicio ordenado por fecha")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Progreso encontrado",
                    content = @Content(schema = @Schema(implementation = ProgresoEjercicioDTO.class))),
            @ApiResponse(responseCode = "404", description = "No se encontró progreso",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping("/progreso-ejercicio/usuario/{usuarioId}/ejercicio/{ejercicioId}/historial")
    public ResponseEntity<List<ProgresoEjercicioDTO>> obtenerProgresoPorUsuarioYEjercicioOrdenado(@PathVariable Integer usuarioId,
                                                                                                  @PathVariable Integer ejercicioId) {
        List<ProgresoEjercicioDTO> progresoEjercicioDTOS = progresoEjercicioService.getProgresoByUsuarioAndEjercicio(usuarioId, ejercicioId);

        if (progresoEjercicioDTOS.isEmpty()) {
            throw new NotFoundEntityException("No se encontró historial para el usuario " + usuarioId + " en el ejercicio " + ejercicioId);
        }

        return ResponseEntity.ok(progresoEjercicioDTOS);
    }

    @Operation(summary = "Obtiene el último progreso de un usuario en un ejercicio")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Último progreso encontrado",
                    content = @Content(schema = @Schema(implementation = ProgresoEjercicioDTO.class))),
            @ApiResponse(responseCode = "404", description = "No se encontró progreso",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping("/progreso-ejercicios/usuario/{usuarioId}/ejercicio/{ejercicioId}/ultimo")
    public ResponseEntity<ProgresoEjercicioDTO> getUltimoProgreso(@PathVariable Integer usuarioId,
                                                                  @PathVariable Integer ejercicioId) {
        ProgresoEjercicioDTO progresoEjercicioDTO = progresoEjercicioService.getUltimoProgresoByUsuarioAndEjercicio(usuarioId, ejercicioId);

        return ResponseEntity.ok(progresoEjercicioDTO);
    }

    @Operation(summary = "Cuenta los progresos de un usuario")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Total de progresos del usuario")
    })
    @GetMapping("/progreso-ejercicios/count/usuario/{usuarioId}")
    public ResponseEntity<Map<String, Object>> countByUsuarioId(@PathVariable Integer usuarioId) {
        Map<String, Object> respuesta = new HashMap<>();

        Long count = progresoEjercicioService.countByUsuarioId(usuarioId);

        respuesta.put("usuarioId", usuarioId);
        respuesta.put("count", count);

        return ResponseEntity.ok(respuesta);
    }

    @Operation(summary = "Cuenta los progresos de un ejercicio")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Total de progresos del ejercicio")
    })
    @GetMapping("/progreso-ejercicios/count/ejercicio/{ejercicioId}")
    public ResponseEntity<Map<String, Object>> countByEjercicioId(@PathVariable Integer ejercicioId) {
        Map<String, Object> respuesta = new HashMap<>();

        Long count = progresoEjercicioService.countByEjercicioId(ejercicioId);

        respuesta.put("ejercicioId", ejercicioId);
        respuesta.put("count", count);

        return ResponseEntity.ok(respuesta);
    }

    @Operation(summary = "Verifica si existe progreso de un usuario en un ejercicio")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Resultado de la verificación")
    })
    @GetMapping("/progreso-ejercicios/exists/usuario/{usuarioId}/ejercicio/{ejercicioId}")
    public ResponseEntity<Map<String, Object>> existsByUsuarioIdAndEjercicioId(@PathVariable Integer usuarioId,
                                                                               @PathVariable Integer ejercicioId) {
        Map<String, Object> respuesta = new HashMap<>();

        boolean existe = progresoEjercicioService.existsByUsuarioIdAndEjercicioId(usuarioId, ejercicioId);

        respuesta.put("usuarioId", usuarioId);
        respuesta.put("ejercicioId", ejercicioId);
        respuesta.put("existe", existe);

        return ResponseEntity.ok(respuesta);
    }

    @Operation(summary = "Elimina todos los progresos de un usuario")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Progresos eliminados correctamente"),
            @ApiResponse(responseCode = "500", description = "Error al eliminar los progresos",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @DeleteMapping("/progreso-ejercicios/usuario/{usuarioId}")
    public ResponseEntity<Map<String, Object>> deleteByUsuarioId(@PathVariable Integer usuarioId) {
        Map<String, Object> respuesta = new HashMap<>();

        try {
            progresoEjercicioService.deleteByUsuarioId(usuarioId);

            respuesta.put("mensaje", "Progresos del usuario " + usuarioId + " eliminados con ÉXITO");
        } catch (Exception e) {
            respuesta.put("mensaje", "Error al eliminar los progresos del usuario " + usuarioId);
            respuesta.put("error", e.getMessage());

            return new ResponseEntity<>(respuesta, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(respuesta, HttpStatus.OK);
    }

    @Operation(summary = "Elimina los progresos de un usuario en un ejercicio concreto")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Progresos eliminados correctamente"),
            @ApiResponse(responseCode = "500", description = "Error al eliminar los progresos",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @DeleteMapping("/progreso-ejercicios/usuario/{usuarioId}/ejercicio/{ejercicioId}")
    public ResponseEntity<Map<String, Object>> deleteByUsuarioIdAndEjercicioId(@PathVariable Integer usuarioId,
                                                                               @PathVariable Integer ejercicioId) {
        Map<String, Object> respuesta = new HashMap<>();

        try {
            progresoEjercicioService.deleteByUsuarioIdAndEjercicioId(usuarioId, ejercicioId);

            respuesta.put("mensaje", "Progresos del usuario " + usuarioId + " en ejercicio " + ejercicioId + " eliminados con ÉXITO");
        } catch (Exception e) {
            respuesta.put("mensaje", "Error al eliminar los progresos del usuario " + usuarioId + " en ejercicio " + ejercicioId);
            respuesta.put("error", e.getMessage());

            return new ResponseEntity<>(respuesta, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(respuesta, HttpStatus.OK);
    }

    @Operation(summary = "Actualiza parcialmente un progreso de ejercicio")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Progreso actualizado",
                    content = @Content(schema = @Schema(implementation = ProgresoEjercicioDTO.class))),
            @ApiResponse(responseCode = "404", description = "Progreso no encontrado",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @PatchMapping("/progreso-ejercicios/{id}")
    public ResponseEntity<ProgresoEjercicioDTO> patchProgresoEjercicio(@PathVariable Integer id, @RequestBody ProgresoEjercicioPatchDTO patchDTO) {
        return ResponseEntity.ok(progresoEjercicioService.patch(id, patchDTO));
    }
}
