package com.gymprofit.api.controller;

import com.gymprofit.api.dto.entity.comida.ComidaCreateDTO;
import com.gymprofit.api.dto.entity.comida.ComidaDTO;
import com.gymprofit.api.dto.entity.comida.ComidaPatchDTO;
import com.gymprofit.api.enums.TipoComida;
import com.gymprofit.api.exceptions.InvalidDataException;
import com.gymprofit.api.exceptions.NotFoundEntityException;
import com.gymprofit.api.exceptions.Response;
import com.gymprofit.api.service.comida.IComidaService;
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
// ComidaController — CRUD y consultas de las comidas registradas
// Gestiona la creación, modificación y borrado de comidas asociadas
// a un usuario (con su tipo: DESAYUNO, ALMUERZO, COMIDA, MERIENDA,
// CENA o SNACK), y ofrece búsquedas por fecha, tipo y usuario.
// ============================================================
@RestController
@RequestMapping("")
@AllArgsConstructor
@Tag(name = "Comida Controlador", description = "Gestión de las comidas")
public class ComidaController {

    private final IComidaService comidaService;

    @Operation(summary = "Obtiene todas las comidas")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Listado de comidas",
                    content = @Content(schema = @Schema(implementation = ComidaDTO.class))),
            @ApiResponse(responseCode = "404", description = "Comidas no encontradas",
                    content = @Content(schema = @Schema(implementation = Response.class))),
            @ApiResponse(responseCode = "500", description = "Error al obtener las comidas",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping("/comidas")
    public ResponseEntity<List<ComidaDTO>> findAll() {
        List<ComidaDTO> comidas = comidaService.findAll();

        return ResponseEntity.ok(comidas);
    }

    @Operation(summary = "Obtiene una comida por ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Comida encontrada",
                    content = @Content(schema = @Schema(implementation = ComidaDTO.class))),
            @ApiResponse(responseCode = "404", description = "Comida no encontrada",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping("/comidas/{id}")
    public ResponseEntity<ComidaDTO> obtenerComida(@PathVariable Integer id) {
        ComidaDTO comida = comidaService.findById(id);

        return ResponseEntity.ok(comida);
    }

    @Operation(summary = "Crea una nueva comida")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Comida creada correctamente",
                    content = @Content(schema = @Schema(implementation = ComidaDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @PostMapping("/comidas")
    public ResponseEntity<ComidaDTO> guardarComida(@Valid @RequestBody ComidaCreateDTO comidaCreateDTO) {
        validarTipoComida(comidaCreateDTO.getTipoComida());

        ComidaDTO comida = comidaService.save(comidaCreateDTO);

        return ResponseEntity.ok(comida);
    }

    @Operation(summary = "Modifica una comida existente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Comida modificada correctamente",
                    content = @Content(schema = @Schema(implementation = ComidaDTO.class))),
            @ApiResponse(responseCode = "404", description = "Comida no encontrada",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @PutMapping("/comidas")
    public ResponseEntity<ComidaDTO> modificarComida(@Valid @RequestBody ComidaDTO comidaDTO) {
        validarTipoComida(comidaDTO.getTipoComida());

        ComidaDTO comida = comidaService.modify(comidaDTO);

        return ResponseEntity.ok(comida);
    }

    @Operation(summary = "Elimina una comida")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Comida eliminada correctamente"),
            @ApiResponse(responseCode = "404", description = "Comida no encontrada",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @DeleteMapping("/comidas/{id}")
    public ResponseEntity<Map<String, Object>> borrarComida(@PathVariable Integer id) {
        Map<String, Object> respuesta = new HashMap<>();

        comidaService.deleteById(id);

        respuesta.put("mensaje", "Comida eliminada con ÉXITO");

        return new ResponseEntity<>(respuesta, HttpStatus.OK);
    }

    @Operation(summary = "Busca comidas por usuario ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Comidas encontradas",
                    content = @Content(schema = @Schema(implementation = ComidaDTO.class))),
            @ApiResponse(responseCode = "404", description = "No se encontraron comidas",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping("/comidas/usuario/{usuarioId}")
    public ResponseEntity<List<ComidaDTO>> obtenerComidasPorUsuario(@PathVariable Integer usuarioId) {
        List<ComidaDTO> comidas = comidaService.findByUsuarioId(usuarioId);

        if (comidas.isEmpty()) {
            throw new NotFoundEntityException("No se encontraron comidas para el usuario con id " + usuarioId);
        }

        return ResponseEntity.ok(comidas);
    }

    @Operation(summary = "Busca comidas por tipo (DESAYUNO, ALMUERZO, COMIDA, MERIENDA, CENA O SNACK)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Comidas encontradas",
                    content = @Content(schema = @Schema(implementation = ComidaDTO.class))),
            @ApiResponse(responseCode = "400", description = "Tipo de comida inválido",
                    content = @Content(schema = @Schema(implementation = Response.class))),
            @ApiResponse(responseCode = "404", description = "No se encontraron comidas",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping("/comidas/tipo/{tipoComida}")
    public ResponseEntity<List<ComidaDTO>> obtenerComidasPorTipo(@PathVariable String tipoComida) {
        if (tipoComida == null || tipoComida.trim().isEmpty()) {
            throw new InvalidDataException("El tipo de comida no puede estar vacío");
        }

        validarTipoComida(tipoComida);

        List<ComidaDTO> comidas = comidaService.findByTipoComida(tipoComida);

        if (comidas.isEmpty()) {
            throw new NotFoundEntityException("No se encontraron comidas del tipo: " + tipoComida);
        }

        return ResponseEntity.ok(comidas);
    }

    @Operation(summary = "Busca comidas por fecha")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Comidas encontradas",
                    content = @Content(schema = @Schema(implementation = ComidaDTO.class))),
            @ApiResponse(responseCode = "404", description = "No se encontraron comidas",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping("/comidas/fecha/{fecha}")
    public ResponseEntity<List<ComidaDTO>> obtenerComidasPorFecha(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {

        List<ComidaDTO> comidas = comidaService.findByFecha(fecha);

        if (comidas.isEmpty()) {
            throw new NotFoundEntityException("No se encontraron comidas en la fecha: " + fecha);
        }

        return ResponseEntity.ok(comidas);
    }

    @Operation(summary = "Busca comidas de un usuario en una fecha específica")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Comidas encontradas",
                    content = @Content(schema = @Schema(implementation = ComidaDTO.class))),
            @ApiResponse(responseCode = "404", description = "No se encontraron comidas",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping("/comidas/usuario/{usuarioId}/fecha/{fecha}")
    public ResponseEntity<List<ComidaDTO>> obtenerComidasPorUsuarioYFecha(
            @PathVariable Integer usuarioId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {

        List<ComidaDTO> comidas = comidaService.findByUsuarioIdAndFecha(usuarioId, fecha);

        if (comidas.isEmpty()) {
            throw new NotFoundEntityException("No se encontraron comidas para el usuario: " + usuarioId + " en la fecha " + fecha);
        }

        return ResponseEntity.ok(comidas);
    }

    @Operation(summary = "Busca comidas de un usuario por tipo")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Comidas encontradas",
                    content = @Content(schema = @Schema(implementation = ComidaDTO.class))),
            @ApiResponse(responseCode = "400", description = "Tipo de comida inválido",
                    content = @Content(schema = @Schema(implementation = Response.class))),
            @ApiResponse(responseCode = "404", description = "No se encontraron comidas",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping("/comidas/usuario/{usuarioId}/tipo/{tipoComida}")
    public ResponseEntity<List<ComidaDTO>> obtenerComidasPorUsuarioYTipo(@PathVariable Integer usuarioId,
                                                                         @PathVariable String tipoComida) {
        if (tipoComida == null || tipoComida.trim().isEmpty()) {
            throw new InvalidDataException("El tipo de comida no puede estar vacío");
        }

        validarTipoComida(tipoComida);

        List<ComidaDTO> comidas = comidaService.findByUsuarioIdAndTipoComida(usuarioId, tipoComida);

        if (comidas.isEmpty()) {
            throw new NotFoundEntityException("No se encontraron comidas del tipo " + tipoComida);
        }

        return ResponseEntity.ok(comidas);
    }

    @Operation(summary = "Cuenta las comidas de un usuario")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cantidad de comidas del usuario")
    })
    @GetMapping("/comidas/count/usuario/{usuarioId}")
    public ResponseEntity<Map<String, Object>> contarComidasPorUsuario(@PathVariable Integer usuarioId) {
        Map<String, Object> respuesta = new HashMap<>();

        Long count = comidaService.countByUsuarioId(usuarioId);

        respuesta.put("count", count);
        respuesta.put("usuarioId", usuarioId);

        return ResponseEntity.ok(respuesta);
    }

    @Operation(summary = "Cuenta las comidas por tipo")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cantidad de comidas por tipo"),
            @ApiResponse(responseCode = "400", description = "Tipo de comida inválido",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping("/comidas/count/tipo/{tipoComida}")
    public ResponseEntity<Map<String, Object>> contarComidasPorTipo(@PathVariable String tipoComida) {
        validarTipoComida(tipoComida);

        Map<String, Object> respuesta = new HashMap<>();

        Long count = comidaService.countByTipoComida(tipoComida);

        respuesta.put("count", count);
        respuesta.put("tipoComida", tipoComida.toUpperCase());

        return ResponseEntity.ok(respuesta);
    }

    @Operation(summary = "Cuenta las comidas de un usuario por tipo")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cantidad de comidas del usuario por tipo"),
            @ApiResponse(responseCode = "400", description = "Tipo de comida inválido",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping("/comidas/count/usuario/{usuarioId}/tipo/{tipoComida}")
    public ResponseEntity<Map<String, Object>> contarComidasPorUsuarioYTipo(@PathVariable Integer usuarioId,
                                                                            @PathVariable String tipoComida) {
        validarTipoComida(tipoComida);

        Map<String, Object> respuesta = new HashMap<>();

        Long count = comidaService.countByUsuarioIdAndTipoComida(usuarioId, tipoComida);

        respuesta.put("count", count);
        respuesta.put("usuarioId", usuarioId);
        respuesta.put("tipoComida", tipoComida);

        return ResponseEntity.ok(respuesta);
    }

    @Operation(summary = "Actualiza parcialmente una comida")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Comida actualizada",
                    content = @Content(schema = @Schema(implementation = ComidaDTO.class))),
            @ApiResponse(responseCode = "404", description = "Comida no encontrada",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @PatchMapping("/comidas/{id}")
    public ResponseEntity<ComidaDTO> patchComida(@PathVariable Integer id, @RequestBody ComidaPatchDTO patchDTO) {
        return ResponseEntity.ok(comidaService.patch(id, patchDTO));
    }

    // Valida que el tipo de comida recibido coincida con un valor del enum TipoComida.
    private void validarTipoComida(String tipoComida) {
        try {
            TipoComida.valueOf(tipoComida.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidDataException("Tipo de comida inválido: " + tipoComida);
        }
    }
}