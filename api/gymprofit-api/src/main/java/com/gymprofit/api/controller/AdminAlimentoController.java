package com.gymprofit.api.controller;

import com.gymprofit.api.dto.admin.AdminAlimentoDTO;
import com.gymprofit.api.dto.admin.AdminAlimentosResumenDTO;
import com.gymprofit.api.dto.common.PageDTO;
import com.gymprofit.api.exceptions.Response;
import com.gymprofit.api.service.admin.IAdminAlimentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// ============================================================
// AdminAlimentoController — el catálogo de alimentos en la web (GP-085)
// Solo ADMIN (regla /admin/**) y solo lectura: se edita con PATCH /alimentos/{id}.
// /admin/alimentos/busqueda, la del panel de la app, no cambia.
// ============================================================
@RestController
@RequestMapping("/admin/alimentos")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Panel de administración")
public class AdminAlimentoController {

    private final IAdminAlimentoService adminAlimentoService;

    @Operation(summary = "Alimentos del catálogo paginados, sin los de los usuarios (ADMIN)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Página de alimentos del catálogo, con el total"),
            @ApiResponse(responseCode = "400", description = "Origen o fuente desconocidos",
                    content = @Content(schema = @Schema(implementation = Response.class)))
    })
    @GetMapping
    public ResponseEntity<PageDTO<AdminAlimentoDTO>> listar(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String categoria,
            @RequestParam(defaultValue = "false") boolean sinIngles,
            @RequestParam(required = false) String origen,
            @RequestParam(required = false) String fuente,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminAlimentoService.listar(q, categoria, sinIngles, origen, fuente, page, size));
    }

    @Operation(summary = "Tamaño del catálogo, sin inglés y categorías (ADMIN)")
    @ApiResponse(responseCode = "200", description = "Resumen del catálogo de alimentos")
    @GetMapping("/resumen")
    public ResponseEntity<AdminAlimentosResumenDTO> resumen() {
        return ResponseEntity.ok(adminAlimentoService.resumen());
    }
}
