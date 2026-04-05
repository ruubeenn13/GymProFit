package com.gymprofit.api.controller;

import com.gymprofit.api.dto.entity.objetivopersonal.ObjetivoPersonalCreateDTO;
import com.gymprofit.api.dto.entity.objetivopersonal.ObjetivoPersonalDTO;
import com.gymprofit.api.dto.entity.objetivopersonal.ObjetivoPersonalUpdateDTO;
import com.gymprofit.api.enums.TipoObjetivo;
import com.gymprofit.api.exceptions.InvalidDataException;
import com.gymprofit.api.exceptions.NotFoundEntityException;
import com.gymprofit.api.exceptions.Response;
import com.gymprofit.api.service.objetivopersonal.IObjetivoPersonalService;
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
@Tag(name = "ObjetivoPersonal Controlador", description = "Gestión de los objetivos personales de los usuarios")
public class ObjetivoPersonalController {

    private final IObjetivoPersonalService objetivoPersonalService;

    @Operation(summary = "Obtiene todos los objetivos personales")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Listado de los objetivos personales",
                    content = @Content(schema = @Schema(implementation = ObjetivoPersonalDTO.class))),
            @ApiResponse(responseCode = "404", description = "No se encontraron objetivos personales",
                    content = @Content(schema = @Schema(implementation = Response.class))),
            @ApiResponse(responseCode = "500", description = "Error al obtener los objetivos personales",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping("/objetivos-personales")
    public ResponseEntity<List<ObjetivoPersonalDTO>> findAll() {
        List<ObjetivoPersonalDTO> objetivosPersonales = objetivoPersonalService.findAll();

        return ResponseEntity.ok(objetivosPersonales);
    }

    @Operation(summary = "Obtiene un objetivo personal por ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Objetivo personal encontrado",
                    content = @Content(schema = @Schema(implementation = ObjetivoPersonalDTO.class))),
            @ApiResponse(responseCode = "404", description = "Objetivo personal no encontrado",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping("/objetivos-personales/{id}")
    public ResponseEntity<ObjetivoPersonalDTO> obtenerObjetivoPersonal(@PathVariable Integer id) {
        ObjetivoPersonalDTO objetivoPersonalDTO = objetivoPersonalService.findById(id);

        return ResponseEntity.ok(objetivoPersonalDTO);
    }

    @Operation(summary = "Crea un nuevo objetivo personal para un usuario")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Objetivo personal creado correctamente",
                    content = @Content(schema = @Schema(implementation = ObjetivoPersonalDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(schema = @Schema(implementation = Response.class))),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    @PostMapping("/objetivos-personales")
    public ResponseEntity<ObjetivoPersonalDTO> guardarObjetivoPersonal(@Valid @RequestBody ObjetivoPersonalCreateDTO objetivoPersonalCreateDTO) {
        ObjetivoPersonalDTO objetivoPersonalDTO = objetivoPersonalService.save(objetivoPersonalCreateDTO);

        return ResponseEntity.ok(objetivoPersonalDTO);
    }

    @Operation(summary = "Actualiza el valor objetivo o el estado de completado")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Objetivo personal actualizado correctamente",
                    content = @Content(schema = @Schema(implementation = ObjetivoPersonalDTO.class))),
            @ApiResponse(responseCode = "404", description = "Objetivo personal no encontrado",
                    content = @Content(schema = @Schema(implementation = ObjetivoPersonalDTO.class)))
    })
    @PutMapping("/objetivos-personales")
    public ResponseEntity<ObjetivoPersonalDTO> actualizarObjetivoPersonal(@Valid @RequestBody ObjetivoPersonalUpdateDTO objetivoPersonalUpdateDTO) {
        ObjetivoPersonalDTO objetivoPersonalDTO = objetivoPersonalService.update(objetivoPersonalUpdateDTO);

        return ResponseEntity.ok(objetivoPersonalDTO);
    }

    @Operation(summary = "Elimina un objetivo personal con ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Objetivo personal eliminado correctamente"),
            @ApiResponse(responseCode = "404", description = "Objetivo personal no encontrado",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @DeleteMapping("/objetivos-personales/{id}")
    public ResponseEntity<Map<String, Object>> borrarObjetivoPersonal(@PathVariable Integer id) {
        Map<String, Object> respuesta = new HashMap<>();

        try {
            objetivoPersonalService.deleteById(id);

            respuesta.put("mensaje", "Objetivo personal eliminado con ÉXITO");
        } catch (Exception e) {
            respuesta.put("mensaje", "Error al eliminar el objetivo personal con id " + id);
            respuesta.put("error", e.getMessage());

            return new ResponseEntity<>(respuesta, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(respuesta, HttpStatus.OK);
    }

    @Operation(summary = "Obtiene todos los objetivos personales de un usuario")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Objetivos encontrados",
                    content = @Content(schema = @Schema(implementation = ObjetivoPersonalDTO.class))),
            @ApiResponse(responseCode = "404", description = "No se encontraron objetivos para este usuario",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping("/objetivos-personales/usuario/{usuarioId}")
    public ResponseEntity<List<ObjetivoPersonalDTO>> obtenerPorUsuario(@PathVariable Integer usuarioId) {
        List<ObjetivoPersonalDTO> objetivosPersonales = objetivoPersonalService.findByUsuarioId(usuarioId);

        if (objetivosPersonales.isEmpty()) {
            throw new NotFoundEntityException("No se encontraron objetivos personales para el usuario con id " + usuarioId);
        }

        return ResponseEntity.ok(objetivosPersonales);
    }

    @Operation(summary = "Obtiene todos los objetivos personales ordenados por fecha de inicio de un usuario")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Objetivos encontrados",
                    content = @Content(schema = @Schema(implementation = ObjetivoPersonalDTO.class))),
            @ApiResponse(responseCode = "404", description = "No se encontraron objetivos para este usuario",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping("/objetivos-personales/usuario/{usuarioId}/ordenados")
    public ResponseEntity<List<ObjetivoPersonalDTO>> obtenerPorUsuarioOrdenados(@PathVariable Integer usuarioId) {
        List<ObjetivoPersonalDTO> objetivosPersonales = objetivoPersonalService.findByUsuarioIdOrdenados(usuarioId);

        if (objetivosPersonales.isEmpty()) {
            throw new NotFoundEntityException("No se encontraron objetivos personales para el usuario con id " + usuarioId);
        }

        return ResponseEntity.ok(objetivosPersonales);
    }

    @Operation(summary = "Obtiene los objetivos pendientes de un usuario")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Objetivos pendientes encontrados",
                    content = @Content(schema = @Schema(implementation = ObjetivoPersonalDTO.class))),
            @ApiResponse(responseCode = "404", description = "No se encontraron objetivos pendientes",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping("/objetivos-personales/usuario/{usuarioId}/pendientes")
    public ResponseEntity<List<ObjetivoPersonalDTO>> obtenerPendientesPorUsuario(@PathVariable Integer usuarioId) {
        List<ObjetivoPersonalDTO> objetivosPersonales = objetivoPersonalService.findPendientesByUsuarioId(usuarioId);

        if (objetivosPersonales.isEmpty()) {
            throw new NotFoundEntityException("No se encontraron objetivos pendientes para el usuario con id " + usuarioId);
        }

        return ResponseEntity.ok(objetivosPersonales);
    }

    @Operation(summary = "Obtiene los objetivos completados de un usuario")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Objetivos completados encontrados",
                    content = @Content(schema = @Schema(implementation = ObjetivoPersonalDTO.class))),
            @ApiResponse(responseCode = "404", description = "No se encontraron objetivos completados",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping("/objetivos-personales/usuario/{usuarioId}/completados")
    public ResponseEntity<List<ObjetivoPersonalDTO>> obtenerCompletadosPorUsuario(@PathVariable Integer usuarioId) {
        List<ObjetivoPersonalDTO> objetivosPersonales = objetivoPersonalService.findCompletadosByUsuarioId(usuarioId);

        if (objetivosPersonales.isEmpty()) {
            throw new NotFoundEntityException("No se encontraron objetivos completados para el usuario con id " + usuarioId);
        }

        return ResponseEntity.ok(objetivosPersonales);
    }

    @Operation(summary = "Obtiene objetivos por tipo")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Objetivos encontrados",
                    content = @Content(schema = @Schema(implementation = ObjetivoPersonalDTO.class))),
            @ApiResponse(responseCode = "404", description = "No se encontraron objetivos para este tipo",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping("/objetivos-personales/tipo/{tipoObjetivo}")
    public ResponseEntity<List<ObjetivoPersonalDTO>> obtenerPorTipoObjetivo(@PathVariable String tipoObjetivo) {
        if (tipoObjetivo == null || tipoObjetivo.trim().isEmpty()) {
            throw new InvalidDataException("El tipo de objetivo no puede estar vacío");
        }

        try {
            TipoObjetivo.valueOf(tipoObjetivo.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidDataException("Tipo de objetivo inválido: \" + tipoObjetivo + \". Valores válidos: PERDER_PESO, GANAR_MASA_MUSCULAR, " +
                    "MEJORAR_RESISTENCIA, MEJORAR_FLEXIBILIDAD, MEJORAR_FUERZA, MANTENER_PESO, REDUCIR_GRASA_CORPORAL, MEJORAR_VELOCIDAD, " +
                    "AUMENTAR_CALORIAS, REDUCIR_CALORIAS, MEJORAR_MOVILIDAD, COMPLETAR_RETO, OTRO");
        }

        List<ObjetivoPersonalDTO> objetivosPersonales = objetivoPersonalService.findByTipoObjetivo(tipoObjetivo);

        if (objetivosPersonales.isEmpty()) {
            throw new NotFoundEntityException("No se encontraron objetivos del tipo " + tipoObjetivo);
        }

        return ResponseEntity.ok(objetivosPersonales);
    }

    @Operation(summary = "Marca un objetivo personal como completado")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Objetivo marcado como completado",
                    content = @Content(schema = @Schema(implementation = ObjetivoPersonalDTO.class))),
            @ApiResponse(responseCode = "400", description = "El objetivo ya estaba completado",
                    content = @Content(schema = @Schema(implementation = Response.class))),
            @ApiResponse(responseCode = "404", description = "Objetivo personal no encontrado",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @PutMapping("/objetivos-personales/{id}/completar")
    public ResponseEntity<ObjetivoPersonalDTO> completar(@PathVariable Integer id) {
        ObjetivoPersonalDTO objetivoPersonalDTO = objetivoPersonalService.completar(id);

        return ResponseEntity.ok(objetivoPersonalDTO);
    }

    @Operation(summary = "Cuenta los objetivos de un usuario")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Total de objetivos del usuario")
    })
    @GetMapping("/objetivos-personales/count/usuario/{usuarioId}")
    public ResponseEntity<Map<String, Object>> contarObjetivosPersonales(@PathVariable Integer usuarioId) {
        Map<String, Object> respuesta = new HashMap<>();

        Long count = objetivoPersonalService.countByUsuarioId(usuarioId);

        respuesta.put("usuarioId", usuarioId);
        respuesta.put("count", count);

        return ResponseEntity.ok(respuesta);
    }

    @Operation(summary = "Cuenta los objetivos personales completados del usuario")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Total de objetivos completados del usuario")
    })
    @GetMapping("/objetivos-personales/count/usuario/{usuarioId}/completados")
    public ResponseEntity<Map<String, Object>> contarObjetivosCompletados(@PathVariable Integer usuarioId) {
        Map<String, Object> respuesta = new HashMap<>();

        Long count = objetivoPersonalService.countCompletadosByUsuarioId(usuarioId);

        respuesta.put("usuarioId", usuarioId);
        respuesta.put("count", count);

        return ResponseEntity.ok(respuesta);
    }

    @Operation(summary = "Cuenta los objetivos personales pendientes del usuario")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Total de objetivos pendientes del usuario")
    })
    @GetMapping("/objetivos-personales/count/usuario/{usuarioId}/pendientes")
    public ResponseEntity<Map<String, Object>> contarObjetivosPendientes(@PathVariable Integer usuarioId) {
        Map<String, Object> respuesta = new HashMap<>();

        Long count = objetivoPersonalService.countPendientesByUsuarioId(usuarioId);

        respuesta.put("usuarioId", usuarioId);
        respuesta.put("count", count);

        return ResponseEntity.ok(respuesta);
    }
}