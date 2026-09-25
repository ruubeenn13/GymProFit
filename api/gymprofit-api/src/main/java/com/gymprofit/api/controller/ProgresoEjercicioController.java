package com.gymprofit.api.controller;

import com.gymprofit.api.dto.entity.progresoejercicio.ProgresoEjercicioDTO;
import com.gymprofit.api.dto.entity.progresoejercicio.RecordDestacadoDTO;
import com.gymprofit.api.exceptions.Response;
import com.gymprofit.api.service.record.IRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// ============================================================
// ProgresoEjercicioController — las dos rutas viejas de progreso (GP-088)
//
// La tabla progreso_ejercicios ya no existe: ningún flujo real la escribía, así que
// un usuario que solo entrenaba no veía nunca su récord ni su gráfica. Estas dos
// rutas son las únicas que leía la app y se quedan para las builds ya repartidas,
// con la misma forma de respuesta, pero leyendo de las series de las sesiones. Las
// builds nuevas usan /records. El resto del CRUD de la tabla se fue con ella.
// ============================================================
@RestController
@AllArgsConstructor
@Tag(name = "ProgresoEjercicio Controlador", description = "Rutas heredadas; las nuevas están en /records")
public class ProgresoEjercicioController {

    private final IRecordService recordService;

    @Operation(summary = "Progresión de un usuario en un ejercicio (heredada)",
            description = "La mejor serie de cada sesión completada, de la más reciente a la más "
                    + "antigua, con la forma de ProgresoEjercicioDTO. Sustituida por "
                    + "/records/ejercicio/{ejercicioId}/progresion.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Progresión encontrada",
                    content = @Content(schema = @Schema(implementation = ProgresoEjercicioDTO.class))),
            @ApiResponse(responseCode = "403", description = "El usuario no es el del token",
                    content = @Content(schema = @Schema(implementation = Response.class))),
            @ApiResponse(responseCode = "404", description = "El usuario o el ejercicio indicados no existen",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping("/progreso-ejercicios/usuario/{usuarioId}/ejercicio/{ejercicioId}/historial")
    public ResponseEntity<List<ProgresoEjercicioDTO>> historial(@PathVariable Integer usuarioId,
                                                                @PathVariable Integer ejercicioId) {
        return ResponseEntity.ok(recordService.historialLegado(usuarioId, ejercicioId));
    }

    @Operation(summary = "Mejor récord de peso del usuario (heredada)",
            description = "El récord de más peso del usuario con el nombre del ejercicio ya resuelto. "
                    + "204 si todavía no ha batido ninguno: la primera marca no es un récord. "
                    + "Sustituida por /records.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Récord encontrado",
                    content = @Content(schema = @Schema(implementation = RecordDestacadoDTO.class))),
            @ApiResponse(responseCode = "204", description = "El usuario aún no tiene ningún récord"),
            @ApiResponse(responseCode = "403", description = "El usuario no es el del token",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping("/progreso-ejercicios/usuario/{usuarioId}/record-destacado")
    public ResponseEntity<RecordDestacadoDTO> recordDestacado(@PathVariable Integer usuarioId) {
        return recordService.recordDestacado(usuarioId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
