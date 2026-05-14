package com.gymprofit.api.controller;

import com.gymprofit.api.dto.entity.medicioncorporal.MedicionCorporalCreateDTO;
import com.gymprofit.api.dto.entity.medicioncorporal.MedicionCorporalDTO;
import com.gymprofit.api.dto.entity.medicioncorporal.MedicionCorporalPatchDTO;
import com.gymprofit.api.exceptions.NotFoundEntityException;
import com.gymprofit.api.exceptions.Response;
import com.gymprofit.api.service.medicioncorporal.IMedicionCorporalService;
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

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("")
@AllArgsConstructor
@Tag(name = "MedicionCorporal Controlador", description = "Gestión de las mediciones corporales de los usuarios")
public class MedicionCorporalController {

    private final IMedicionCorporalService medicionCorporalService;

    @Operation(summary = "Obtiene todas las mediciones corporales")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Listado de mediciones corporales",
                    content = @Content(schema = @Schema(implementation = MedicionCorporalDTO.class))),
            @ApiResponse(responseCode = "404", description = "No se encontraron mediciones corporales",
                    content = @Content(schema = @Schema(implementation = Response.class))),
            @ApiResponse(responseCode = "500", description = "Error al obtener las mediciones corporales",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping("/mediciones-corporales")
    public ResponseEntity<List<MedicionCorporalDTO>> findAll() {
        List<MedicionCorporalDTO> medicionesCorporales = medicionCorporalService.findAll();

        return ResponseEntity.ok(medicionesCorporales);
    }

    @Operation(summary = "Obtiene una medición corporal por ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Medición corporal encontrada",
                    content = @Content(schema = @Schema(implementation = MedicionCorporalDTO.class))),
            @ApiResponse(responseCode = "404", description = "Medición corporal no encontrada",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping("/mediciones-corporales/{id}")
    public ResponseEntity<MedicionCorporalDTO> obtenerMedicionCorporal(@PathVariable Integer id) {
        MedicionCorporalDTO medicionCorporalDTO = medicionCorporalService.findById(id);

        return ResponseEntity.ok(medicionCorporalDTO);
    }

    @Operation(summary = "Crea una nueva medición corporal. El IMC se calcula automáticamente si se proporciona peso y altura")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Medición corporal creada correctamente",
                    content = @Content(schema = @Schema(implementation = MedicionCorporalDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(schema = @Schema(implementation = Response.class))),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @PostMapping("/mediciones-corporales")
    public ResponseEntity<MedicionCorporalDTO> guardarMedicionCorporal(@Valid @RequestBody MedicionCorporalCreateDTO medicionCorporalCreateDTO) {
        MedicionCorporalDTO medicionCorporalDTO = medicionCorporalService.save(medicionCorporalCreateDTO);

        return ResponseEntity.ok(medicionCorporalDTO);
    }
    @Operation(summary = "Modifica una medición corporal existente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Medición corporal modificada correctamente"),
            @ApiResponse(responseCode = "404", description = "Medición corporal no encontrada",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @PutMapping("/mediciones-corporales")
    public ResponseEntity<MedicionCorporalDTO> modificarMedicionCorporal(@Valid @RequestBody MedicionCorporalDTO medicionCorporalDTO) {
        MedicionCorporalDTO medicionActualizada = medicionCorporalService.modify(medicionCorporalDTO);

        return ResponseEntity.ok(medicionActualizada);
    }

    @Operation(summary = "Elimina una medición corporal por ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Medición corporal eliminada correctamente"),
            @ApiResponse(responseCode = "404", description = "Medición corporal no encontrada",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @DeleteMapping("/mediciones-corporales/{id}")
    public ResponseEntity<Map<String, Object>> borrarMedicionCorporal(@PathVariable Integer id) {
        Map<String, Object> respuesta = new HashMap<>();

        try {
            medicionCorporalService.deleteById(id);

            respuesta.put("mensaje", "Medición corporal eliminada con ÉXITO " + id);
        } catch (Exception e) {
            respuesta.put("mensjae", "Error al eliminar la medición corporal con id: " + id);
            respuesta.put("error", e.getMessage());

            return new ResponseEntity<>(respuesta, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(respuesta, HttpStatus.OK);
    }

    @Operation(summary = "Obtiene todas las mediciones corporales de un usuario")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Mediciones encontradas",
                    content = @Content(schema = @Schema(implementation = MedicionCorporalDTO.class))),
            @ApiResponse(responseCode = "404", description = "No se encontraron mediciones para este usuario",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping("/mediciones-corporales/usuario/{usuarioId}")
    public ResponseEntity<List<MedicionCorporalDTO>> obtenerMedicionPorUsuario(@PathVariable Integer usuarioId) {
        List<MedicionCorporalDTO> medicionesCorporales = medicionCorporalService.findByUsuarioId(usuarioId);

        if(medicionesCorporales.isEmpty()) {
            throw new NotFoundEntityException("No se encontraron mediciones para el usuario id " + usuarioId);
        }

        return ResponseEntity.ok(medicionesCorporales);
    }

    @Operation(summary = "Obtiene las mediciones de un usuario ordenadas por fecha descendente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Mediciones encontradas ordenadas",
                    content = @Content(schema = @Schema(implementation = MedicionCorporalDTO.class))),
            @ApiResponse(responseCode = "404", description = "No se encontraron mediciones para este usuario",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping("/mediciones-corporales/usuario/{usuarioId}/ordenadas")
    public ResponseEntity<List<MedicionCorporalDTO>> obtenerMedicionPorUsuarioOrdenadas(@PathVariable Integer usuarioId) {
        List<MedicionCorporalDTO> medicionesCorporales = medicionCorporalService.findByUsuarioIdOrdenadas(usuarioId);

        if (medicionesCorporales.isEmpty()) {
            throw new NotFoundEntityException("No se encontraron mediciones para el usuario con id " + usuarioId);
        }

        return ResponseEntity.ok(medicionesCorporales);
    }

    @Operation(summary = "Obtiene las mediciones de un usuario entre dos fechas")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Mediciones encontradas",
                    content = @Content(schema = @Schema(implementation = MedicionCorporalDTO.class))),
            @ApiResponse(responseCode = "404", description = "No se encontraron mediciones en ese rango de fechas",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping("/mediciones-corporales/usuario/{usuarioId}/rango")
    public ResponseEntity<List<MedicionCorporalDTO>> obtenerMedicionesPorUsuarioYFecha(@PathVariable Integer usuarioId,
                                                                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)LocalDateTime inicio,
                                                                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)LocalDateTime fin) {
        List<MedicionCorporalDTO> medicionesCorporales = medicionCorporalService.findByUsuarioIdAndFechaBetween(usuarioId, inicio, fin);

        if (medicionesCorporales.isEmpty()) {
            throw new NotFoundEntityException("No se encontraron mediciones para el usuario " + usuarioId + " en ese rango de fecha");
        }

        return ResponseEntity.ok(medicionesCorporales);
    }

    @Operation(summary = "Obtiene las últimas mediciones de un usuario ordenadas por fecha")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Últimas mediciones encontradas",
                    content = @Content(schema = @Schema(implementation = MedicionCorporalDTO.class))),
            @ApiResponse(responseCode = "404", description = "No se encontraron mediciones para este usuario",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping("/mediciones-corporales/usuario/{usuarioId}/ultimas")
    public ResponseEntity<List<MedicionCorporalDTO>> obtenerUltimasMediciones (@PathVariable Integer usuarioId) {
        List<MedicionCorporalDTO> medicionesCorporales = medicionCorporalService.getUltimasMediciones(usuarioId);

        if (medicionesCorporales.isEmpty()) {
            throw new NotFoundEntityException("No se encontraron mediciones para el usuario con id " + usuarioId);
        }

        return ResponseEntity.ok(medicionesCorporales);
    }

    @Operation(summary = "Actualiza parcialmente una medición corporal")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Medición corporal actualizada",
                    content = @Content(schema = @Schema(implementation = MedicionCorporalDTO.class))),
            @ApiResponse(responseCode = "404", description = "Medición corporal no encontrada",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @PatchMapping("/mediciones-corporales/{id}")
    public ResponseEntity<MedicionCorporalDTO> patchMedicionCorporal(@PathVariable Integer id, @RequestBody MedicionCorporalPatchDTO patchDTO) {
        return ResponseEntity.ok(medicionCorporalService.patch(id, patchDTO));
    }
}
