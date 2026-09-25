package com.gymprofit.api.controller;

import com.gymprofit.api.dto.entity.record.PuntoProgresionDTO;
import com.gymprofit.api.dto.entity.record.RecordsDTO;
import com.gymprofit.api.exceptions.Response;
import com.gymprofit.api.service.record.IRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

// ============================================================
// RecordController — récords y progresión del usuario del token (GP-088)
//
// Ninguna ruta lleva id de usuario: el usuario sale del token (DEC-013), como en
// /logros/progreso. Los récords se calculan a partir de las series de las sesiones
// completadas; no hay tabla de récords que escribir.
// ============================================================
@RestController
@RequestMapping("/records")
@RequiredArgsConstructor
@Tag(name = "Records Controlador", description = "Récords personales y progresión por ejercicio")
public class RecordController {

    private final IRecordService recordService;

    @Operation(summary = "Récords del usuario autenticado",
            description = "El récord vigente de cada ejercicio que tiene alguno y, si se pasa "
                    + "`desde`, los récords batidos desde ese día. La primera marca de un "
                    + "ejercicio no es un récord: es el punto de partida.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Récords; listas vacías si no hay ninguno",
                    content = @Content(schema = @Schema(implementation = RecordsDTO.class))),
            @ApiResponse(responseCode = "403", description = "Token de invitado: no hay récords que dar",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping
    public ResponseEntity<RecordsDTO> records(
            @Parameter(description = "Primer día del periodo de recientes (AAAA-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde) {
        return ResponseEntity.ok(recordService.recordsDelUsuarioActual(desde));
    }

    @Operation(summary = "Progresión del usuario autenticado en un ejercicio",
            description = "La mejor serie de cada sesión completada, en orden cronológico. Lista "
                    + "vacía si nunca lo ha entrenado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Puntos de la gráfica",
                    content = @Content(schema = @Schema(implementation = PuntoProgresionDTO.class))),
            @ApiResponse(responseCode = "404", description = "El ejercicio no existe",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping("/ejercicio/{ejercicioId}/progresion")
    public ResponseEntity<List<PuntoProgresionDTO>> progresion(@PathVariable Integer ejercicioId) {
        return ResponseEntity.ok(recordService.progresionDelUsuarioActual(ejercicioId));
    }
}
