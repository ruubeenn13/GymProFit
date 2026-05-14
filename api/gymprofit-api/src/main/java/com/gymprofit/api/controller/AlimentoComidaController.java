package com.gymprofit.api.controller;

import com.gymprofit.api.dto.entity.alimentocomida.AlimentoComidaCreateDTO;
import com.gymprofit.api.dto.entity.alimentocomida.AlimentoComidaDTO;
import com.gymprofit.api.dto.entity.alimentocomida.AlimentoComidaPatchDTO;
import com.gymprofit.api.exceptions.NotFoundEntityException;
import com.gymprofit.api.exceptions.Response;
import com.gymprofit.api.service.alimentocomida.IAlimentoComidaService;
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
@Tag(name = "AlimentoComida Controlador", description = "Gestión de la relación entre alimentos y comidas")
public class AlimentoComidaController {

    private final IAlimentoComidaService alimentoComidaService;

    @Operation(summary = "Obtiene todas las relaciones alimento-comida")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Listado de relaciones",
                    content = @Content(schema = @Schema(implementation = AlimentoComidaDTO.class))),
            @ApiResponse(responseCode = "404", description = "No se han encontrado relaciones",
                    content = @Content(schema = @Schema(implementation = Response.class))),
            @ApiResponse(responseCode = "500", description = "Error al obtener las relaciones",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping("/alimentos-comida")
    public ResponseEntity<List<AlimentoComidaDTO>> findAll() {
        List<AlimentoComidaDTO> alimentosComida = alimentoComidaService.findAll();

        return ResponseEntity.ok(alimentosComida);
    }

    @Operation(summary = "Obtiene una relación alimento-comida por ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Relación encontrada",
                    content = @Content(schema = @Schema(implementation = AlimentoComidaDTO.class))),
            @ApiResponse(responseCode = "404", description = "Relación no encontrada",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping("/alimentos-comida/{id}")
    public ResponseEntity<AlimentoComidaDTO> obtenerAlimentoComida(@PathVariable Integer id) {
        AlimentoComidaDTO alimentoComida = alimentoComidaService.findById(id);

        return ResponseEntity.ok(alimentoComida);
    }

    @Operation(summary = "Crea una nueva relación alimento-comida")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Relación creada correctamente",
                    content = @Content(schema = @Schema(implementation = AlimentoComidaDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o relación duplicada",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @PostMapping("/alimentos-comida")
    public ResponseEntity<AlimentoComidaDTO> guardarAlimentoComida(@Valid @RequestBody AlimentoComidaCreateDTO alimentoComidaCreateDTO) {
        AlimentoComidaDTO alimentoComidaDTO = alimentoComidaService.save(alimentoComidaCreateDTO);

        return ResponseEntity.ok(alimentoComidaDTO);
    }

    @Operation(summary = "Modifica una relación alimento-comida existente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Relación modificada correctamente",
                    content = @Content(schema = @Schema(implementation = AlimentoComidaDTO.class))),
            @ApiResponse(responseCode = "404", description = "Relación no encontrada",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @PutMapping("/alimentos-comida")
    public ResponseEntity<AlimentoComidaDTO> modificarAlimentoComida(@Valid @RequestBody AlimentoComidaDTO alimentoComidaDTO) {
        AlimentoComidaDTO alimentoComida = alimentoComidaService.modify(alimentoComidaDTO);

        return ResponseEntity.ok(alimentoComida);
    }

    @Operation(summary = "Elimina una relación alimento-comida")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Relación eliminada correctamente"),
            @ApiResponse(responseCode = "404", description = "Relación no encontrada",
                    content =  @Content(schema = @Schema(implementation = Response.class)))
    })
    @DeleteMapping("/alimentos-comida/{id}")
    public ResponseEntity<Map<String, Object>> borrarAlimentoComida(@PathVariable Integer id) {
        Map<String, Object> respuesta = new HashMap<>();

        try {
            alimentoComidaService.deleteById(id);

            respuesta.put("mensaje", "Relación alimento-comida eliminada con ÉXITO");
        } catch (Exception e) {
            respuesta.put("mensaje", "Error al borrar la relación " + id);
            respuesta.put("error", e.getMessage());

            return new ResponseEntity<>(respuesta, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(respuesta, HttpStatus.OK);
    }

    @Operation(summary = "Obtiene todos los alimentos de una comida")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Alimentos encontrados",
                    content = @Content(schema = @Schema(implementation = AlimentoComidaDTO.class))),
            @ApiResponse(responseCode = "404", description = "No se encontraron alimentos",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping("/alimentos-comida/comida/{comidaId}")
    public ResponseEntity<List<AlimentoComidaDTO>> obtenerAlimentosPorComida(@PathVariable Integer comidaId) {
        List<AlimentoComidaDTO> alimentosComida = alimentoComidaService.findByComidaId(comidaId);

        if (alimentosComida.isEmpty()) {
            throw new NotFoundEntityException("No se encontraron alimentos para la comida " + comidaId);
        }

        return ResponseEntity.ok(alimentosComida);
    }

    @Operation(summary = "Obtiene todas las comidas que contienen un alimento")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Comidas encontradas",
                    content = @Content(schema = @Schema(implementation = AlimentoComidaDTO.class))),
            @ApiResponse(responseCode = "404", description = "No se encontraron comidas",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping("/alimentos-comida/alimento/{alimentoId}")
    public ResponseEntity<List<AlimentoComidaDTO>> obtenerComidasPorAlimento(@PathVariable Integer alimentoId) {
        List<AlimentoComidaDTO> alimentosComida= alimentoComidaService.findByAlimentoId(alimentoId);

        if (alimentosComida.isEmpty()) {
            throw new NotFoundEntityException("No se encontraron comidas con el alimento " + alimentoId);
        }

        return ResponseEntity.ok(alimentosComida);
    }

    @Operation(summary = "Busca la relación específica entre una comida y un alimento")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Relación encontrada",
                    content = @Content(schema = @Schema(implementation = AlimentoComidaDTO.class))),
            @ApiResponse(responseCode = "404", description = "No existe relación entre esta comida y alimento",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping("/alimentos-comida/comida/{comidaId}/alimento/{alimentoId}")
    public ResponseEntity<AlimentoComidaDTO> obtenerRelacionComidaAlimento(@PathVariable Integer comidaId,
                                                                           @PathVariable Integer alimentoId) {
        AlimentoComidaDTO alimentoComidaDTO = alimentoComidaService.findByComidaIdAndAlimentoId(comidaId, alimentoId);

        return ResponseEntity.ok(alimentoComidaDTO);
    }

    @Operation(summary = "Elimina todos los alimentos de una comida")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Alimentos eliminados correctamente"),
            @ApiResponse(responseCode = "500", description = "Error al eliminar los alimentos",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @DeleteMapping("/alimentos-comida/comida/{comidaId}")
    public ResponseEntity<Map<String, Object>> eliminarTodosPorComida(@PathVariable Integer comidaId) {
        Map<String, Object> respuesta = new HashMap<>();

        try {
            alimentoComidaService.deleteByComidaId(comidaId);

            respuesta.put("mensaje", "Todos los alimentos de la comida eliminados con ÉXITO");
        } catch (Exception e) {
            respuesta.put("mensaje", "Error al eliminar los alimentos de la comida " + comidaId);
            respuesta.put("error", e.getMessage());

            return new ResponseEntity<>(respuesta, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(respuesta, HttpStatus.OK);
    }

    @Operation(summary = "Elimina un alimento específico de una comida")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Alimento eliminado correctamente"),
            @ApiResponse(responseCode = "500", description = "Error al eliminar el alimento",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @DeleteMapping("/alimentos-comida/comida/{comidaId}/alimento/{alimentoId}")
    public ResponseEntity<Map<String, Object>> eliminarAlimentoDeComida(@PathVariable Integer comidaId,
                                                                        @PathVariable Integer alimentoId) {
        Map<String, Object> respuesta = new HashMap<>();

        try {
            alimentoComidaService.deleteByComidaIdAndAlimentoId(comidaId, alimentoId);

            respuesta.put("mensaje", "Alimento eliminado de la comida con ÉXITO");
        } catch (Exception e) {
            respuesta.put("mensaje", "Error al eliminar el alimento " + alimentoId + " de la comida " + comidaId);
            respuesta.put("error", e.getMessage());

            return new ResponseEntity<>(respuesta, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(respuesta, HttpStatus.OK);
    }

    @Operation(summary = "Verifica si existe relación entre una comida y un alimento")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Resultado de la verificación")
    })
    @GetMapping("/alimentos-comida/exists/comida/{comidaId}/alimento/{alimentoId}")
    public ResponseEntity<Map<String, Object>> existeRelacion(@PathVariable Integer comidaId,
                                                              @PathVariable Integer alimentoId) {
        Map<String, Object> respuesta = new HashMap<>();

        boolean exists = alimentoComidaService.existsByComidaIdAndAlimentoId(comidaId, alimentoId);

        respuesta.put("exists", exists);
        respuesta.put("comidaId", comidaId);
        respuesta.put("alimentoId", alimentoId);

        return ResponseEntity.ok(respuesta);
    }

    @Operation(summary = "Cuenta cuántos alimentos tiene una comida")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cantidad de alimentos")
    })
    @GetMapping("/alimentos-comida/count/comida/{comidaId}")
    public ResponseEntity<Map<String, Object>> contarAlimentosPorComida(@PathVariable Integer comidaId) {
        Map<String, Object> respuesta = new HashMap<>();

        Long count = alimentoComidaService.countByComidaId(comidaId);

        respuesta.put("count", count);
        respuesta.put("comidaId", count);

        return ResponseEntity.ok(respuesta);
    }

    @Operation(summary = "Cuenta en cuántas comidas se usa un alimento")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cantidad de comidas")
    })
    @GetMapping("/alimentos-comida/count/alimento/{alimentoId}")
    public ResponseEntity<Map<String, Object>> contarComidasPorAlimento(@PathVariable Integer alimentoId) {
        Map<String, Object> respuesta = new HashMap<>();

        Long count = alimentoComidaService.countByAlimentoId(alimentoId);

        respuesta.put("count", count);
        respuesta.put("alimentoId", alimentoId);

        return ResponseEntity.ok(respuesta);
    }

    @Operation(summary = "Actualiza parcialmente una relación alimento-comida")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Relación actualizada",
                    content = @Content(schema = @Schema(implementation = AlimentoComidaDTO.class))),
            @ApiResponse(responseCode = "404", description = "Relación no encontrada",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @PatchMapping("/alimentos-comida/{id}")
    public ResponseEntity<AlimentoComidaDTO> patchAlimentoComida(@PathVariable Integer id, @RequestBody AlimentoComidaPatchDTO patchDTO) {
        return ResponseEntity.ok(alimentoComidaService.patch(id, patchDTO));
    }
}