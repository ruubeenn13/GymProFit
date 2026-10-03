package com.gymprofit.api.controller;

import com.gymprofit.api.dto.entity.alimento.AlimentoDTO;
import com.gymprofit.api.dto.entity.comida.ComidaRecienteDTO;
import com.gymprofit.api.dto.entity.comida.CopiaComidaRespuestaDTO;
import com.gymprofit.api.dto.entity.comida.CopiarComidaDTO;
import com.gymprofit.api.exceptions.Response;
import com.gymprofit.api.service.comida.CopiarComidaService;
import com.gymprofit.api.service.comida.HabitualesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

// ============================================================
// AtajosComidaController — apuntar una comida entera o lo de siempre (lote 1.6.4)
// Las comidas recientes que se pueden copiar, copiar una y «Lo que sueles». Todo de la
// cuenta del token (DEC-013); el invitado no añade, así que tampoco esto (SecurityConfig,
// /comidas/** es de USER y ADMIN).
// ============================================================
@RestController
@RequestMapping("/comidas")
@AllArgsConstructor
@Tag(name = "Comidas", description = "Gestión de comidas del usuario")
public class AtajosComidaController {

    private final CopiarComidaService copiarComidaService;
    private final HabitualesService habitualesService;

    @Operation(summary = "Hasta tres comidas recientes que se pueden copiar a la de ese día y ese tipo")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Primero la del mismo tipo de antes de ese día; después, las más recientes",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ComidaRecienteDTO.class)))),
            @ApiResponse(responseCode = "400", description = "Tipo de comida inválido",
                    content = @Content(schema = @Schema(implementation = Response.class))),
            @ApiResponse(responseCode = "403", description = "Invitado",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping("/recientes")
    public ResponseEntity<List<ComidaRecienteDTO>> recientes(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam String tipoComida) {
        return ResponseEntity.ok(copiarComidaService.recientes(fecha, tipoComida));
    }

    @Operation(summary = "Copia una comida entera a la de ese día y ese tipo, que se encuentra o se crea")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "La comida de destino y cada alimento copiado, con su línea y anterior",
                    content = @Content(schema = @Schema(implementation = CopiaComidaRespuestaDTO.class))),
            @ApiResponse(responseCode = "400", description = "Origen y destino iguales, nada que copiar o datos inválidos",
                    content = @Content(schema = @Schema(implementation = Response.class))),
            @ApiResponse(responseCode = "403", description = "La comida de origen no es de la cuenta, o invitado",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @PostMapping("/copiar")
    public ResponseEntity<CopiaComidaRespuestaDTO> copiar(@Valid @RequestBody CopiarComidaDTO pedido) {
        return ResponseEntity.ok(copiarComidaService.copiar(pedido));
    }

    @Operation(summary = "Hasta tres alimentos que la cuenta suele tomar en ese tipo de comida")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Del más repetido al menos, con raciones, favorito y última cantidad en ese tipo",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = AlimentoDTO.class)))),
            @ApiResponse(responseCode = "400", description = "Tipo de comida inválido",
                    content = @Content(schema = @Schema(implementation = Response.class))),
            @ApiResponse(responseCode = "403", description = "Invitado",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping("/habituales")
    public ResponseEntity<List<AlimentoDTO>> habituales(@RequestParam String tipoComida) {
        return ResponseEntity.ok(habitualesService.habituales(tipoComida));
    }
}
