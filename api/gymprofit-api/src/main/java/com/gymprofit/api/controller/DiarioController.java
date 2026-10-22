package com.gymprofit.api.controller;

import com.gymprofit.api.dto.entity.diario.AguaDiaDTO;
import com.gymprofit.api.dto.entity.diario.AguaPedidoDTO;
import com.gymprofit.api.dto.entity.diario.AlimentoApuntadoDTO;
import com.gymprofit.api.dto.entity.diario.DiaDiarioDTO;
import com.gymprofit.api.exceptions.Response;
import com.gymprofit.api.service.diario.DiarioService;
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
// DiarioController — el agua, «Hoy no» y los días del diario (lote 1.6.8)
// Todo de la cuenta del token (DEC-013). Solo USER y ADMIN: el invitado no apunta
// comidas, así que tampoco tiene diario (SecurityConfig).
// ============================================================
@RestController
@RequestMapping("/diario")
@AllArgsConstructor
@Tag(name = "Diario", description = "El agua, «Hoy no» y el estado de cada día")
public class DiarioController {

    private final DiarioService diarioService;

    @Operation(summary = "Cada día del rango: comidas apuntadas, «Hoy no», agua y entreno")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Un elemento por día, en orden, ambos incluidos",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = DiaDiarioDTO.class)))),
            @ApiResponse(responseCode = "400", description = "Sin fechas, rango al revés o de más de 31 días",
                    content = @Content(schema = @Schema(implementation = Response.class))),
            @ApiResponse(responseCode = "403", description = "Invitado",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping
    public ResponseEntity<List<DiaDiarioDTO>> dias(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(diarioService.dias(desde, hasta));
    }

    @Operation(summary = "Los 3 alimentos más apuntados del rango, con las veces (tablero 12 de nutrición)",
            description = "Solo lectura, del usuario del token. Hasta 7 días: es «Lo que más has comido» de «Tu semana».")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Hasta tres alimentos, de más a menos; [] si no hay nada",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = AlimentoApuntadoDTO.class)))),
            @ApiResponse(responseCode = "400", description = "Sin fechas, rango al revés o de más de 7 días",
                    content = @Content(schema = @Schema(implementation = Response.class))),
            @ApiResponse(responseCode = "403", description = "Invitado",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping("/mas-apuntados")
    public ResponseEntity<List<AlimentoApuntadoDTO>> masApuntados(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(diarioService.masApuntados(desde, hasta));
    }

    @Operation(summary = "Guarda el agua de un día: el total en mililitros (repetible)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "El agua del día, como quedó",
                    content = @Content(schema = @Schema(implementation = AguaDiaDTO.class))),
            @ApiResponse(responseCode = "400", description = "Sin cifra, fuera de 0 a 20.000 o un día que no ha llegado",
                    content = @Content(schema = @Schema(implementation = Response.class))),
            @ApiResponse(responseCode = "403", description = "Invitado",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @PutMapping("/{fecha}/agua")
    public ResponseEntity<AguaDiaDTO> agua(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                                           @Valid @RequestBody AguaPedidoDTO pedido) {
        return ResponseEntity.ok(diarioService.guardarAgua(fecha, pedido.getMililitros()));
    }

    @Operation(summary = "Marca una comida de un día como «Hoy no» (repetible)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Marcada"),
            @ApiResponse(responseCode = "400", description = "Comida que no es del diario o día que no ha llegado",
                    content = @Content(schema = @Schema(implementation = Response.class))),
            @ApiResponse(responseCode = "403", description = "Invitado",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @PutMapping("/{fecha}/hoy-no/{tipoComida}")
    public ResponseEntity<Void> marcarHoyNo(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                                            @PathVariable String tipoComida) {
        diarioService.marcarHoyNo(fecha, tipoComida);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Quita la marca «Hoy no» (repetible)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Ya no está marcada"),
            @ApiResponse(responseCode = "400", description = "Comida que no es del diario",
                    content = @Content(schema = @Schema(implementation = Response.class))),
            @ApiResponse(responseCode = "403", description = "Invitado",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @DeleteMapping("/{fecha}/hoy-no/{tipoComida}")
    public ResponseEntity<Void> quitarHoyNo(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                                            @PathVariable String tipoComida) {
        diarioService.quitarHoyNo(fecha, tipoComida);
        return ResponseEntity.noContent().build();
    }
}
