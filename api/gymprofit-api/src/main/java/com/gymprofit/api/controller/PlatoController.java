package com.gymprofit.api.controller;

import com.gymprofit.api.dto.entity.plato.LoMioDTO;
import com.gymprofit.api.dto.entity.plato.PlatoDTO;
import com.gymprofit.api.dto.entity.plato.PlatoPedidoDTO;
import com.gymprofit.api.exceptions.Response;
import com.gymprofit.api.service.plato.PlatoService;
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

// ============================================================
// PlatoController — los platos propios y la pestaña «Mis platos» (lote 1.6.6)
// Crear, leer uno con sus ingredientes, cambiar y borrar (desactivar). El dueño sale
// del token; el plato de otra cuenta y el que no existe, el mismo 403. Solo USER y
// ADMIN: el invitado no crea nada (SecurityConfig).
// ============================================================
@RestController
@AllArgsConstructor
@Tag(name = "Platos", description = "Los platos propios: varios alimentos en uno")
public class PlatoController {

    private final PlatoService platoService;

    @Operation(summary = "Crea un plato de la cuenta con sus ingredientes")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "El plato, con sus valores",
                    content = @Content(schema = @Schema(implementation = PlatoDTO.class))),
            @ApiResponse(responseCode = "400", description = "Nombre, raciones, peso o cantidades mal; un plato como ingrediente; más de 900 kcal por 100 g",
                    content = @Content(schema = @Schema(implementation = Response.class))),
            @ApiResponse(responseCode = "403", description = "Invitado, o un ingrediente de otra cuenta o que no existe",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @PostMapping("/platos")
    public ResponseEntity<PlatoDTO> crear(@Valid @RequestBody PlatoPedidoDTO pedido) {
        return new ResponseEntity<>(platoService.crear(pedido), HttpStatus.CREATED);
    }

    @Operation(summary = "Un plato de la cuenta, con sus ingredientes")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "El plato",
                    content = @Content(schema = @Schema(implementation = PlatoDTO.class))),
            @ApiResponse(responseCode = "403", description = "No existe o es de otra cuenta",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping("/platos/{id}")
    public ResponseEntity<PlatoDTO> leer(@PathVariable Integer id) {
        return ResponseEntity.ok(platoService.leer(id));
    }

    @Operation(summary = "Cambia un plato: sustituye sus ingredientes y recalcula sus valores")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "El plato cambiado",
                    content = @Content(schema = @Schema(implementation = PlatoDTO.class))),
            @ApiResponse(responseCode = "400", description = "Como al crearlo",
                    content = @Content(schema = @Schema(implementation = Response.class))),
            @ApiResponse(responseCode = "403", description = "No existe, está borrado o es de otra cuenta",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @PutMapping("/platos/{id}")
    public ResponseEntity<PlatoDTO> cambiar(@PathVariable Integer id, @Valid @RequestBody PlatoPedidoDTO pedido) {
        return ResponseEntity.ok(platoService.cambiar(id, pedido));
    }

    @Operation(summary = "Borra un plato (lo desactiva; lo ya apuntado no cambia). Repetible")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Borrado"),
            @ApiResponse(responseCode = "403", description = "No existe o es de otra cuenta",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @DeleteMapping("/platos/{id}")
    public ResponseEntity<Void> borrar(@PathVariable Integer id) {
        platoService.borrar(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Los platos de la cuenta y, aparte, los alimentos que ha creado")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Del usado más recientemente al menos",
                    content = @Content(schema = @Schema(implementation = LoMioDTO.class))),
            @ApiResponse(responseCode = "403", description = "Invitado",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping("/alimentos/mios")
    public ResponseEntity<LoMioDTO> loMio() {
        return ResponseEntity.ok(platoService.loMio());
    }
}
