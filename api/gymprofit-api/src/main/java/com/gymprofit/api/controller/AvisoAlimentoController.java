package com.gymprofit.api.controller;

import com.gymprofit.api.dto.admin.AdminAvisoAlimentoDTO;
import com.gymprofit.api.dto.common.PageDTO;
import com.gymprofit.api.dto.entity.alimento.AvisoAlimentoCreateDTO;
import com.gymprofit.api.exceptions.Response;
import com.gymprofit.api.service.alimento.AvisoAlimentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// ============================================================
// AvisoAlimentoController — reportar un alimento y los avisos pendientes (lote 1.6.1)
// POST /alimentos/avisos: USER (regla de POST /alimentos/**; el invitado no).
// /admin/avisos-alimento: solo ADMIN (regla /admin/**).
// ============================================================
@RestController
@RequiredArgsConstructor
@Tag(name = "Avisos de alimentos", description = "Reportar un alimento y revisar los avisos")
public class AvisoAlimentoController {

    private final AvisoAlimentoService avisoAlimentoService;

    @Operation(summary = "Reporta un problema de un alimento del catálogo, sin guardar quién")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Recibido"),
            @ApiResponse(responseCode = "400", description = "Datos que no cuadran, motivo desconocido o alimento propio",
                    content = @Content(schema = @Schema(implementation = Response.class))),
            @ApiResponse(responseCode = "403", description = "El alimento es de otro usuario",
                    content = @Content(schema = @Schema(implementation = Response.class))),
            @ApiResponse(responseCode = "404", description = "El alimento o el código no existen",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @PostMapping("/alimentos/avisos")
    public ResponseEntity<Void> reportar(@Valid @RequestBody AvisoAlimentoCreateDTO pedido) {
        avisoAlimentoService.reportar(pedido);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Avisos de alimentos pendientes, los más repetidos primero (ADMIN)")
    @ApiResponse(responseCode = "200", description = "Página de avisos pendientes")
    @GetMapping("/admin/avisos-alimento")
    public ResponseEntity<PageDTO<AdminAvisoAlimentoDTO>> pendientes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(avisoAlimentoService.pendientes(page, size));
    }

    @Operation(summary = "Marca un aviso como resuelto (ADMIN)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Resuelto"),
            @ApiResponse(responseCode = "404", description = "El aviso no existe",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @PutMapping("/admin/avisos-alimento/{id}/resuelto")
    public ResponseEntity<Void> resolver(@PathVariable Integer id) {
        avisoAlimentoService.resolver(id);
        return ResponseEntity.noContent().build();
    }
}
