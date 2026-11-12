package com.gymprofit.api.controller;

import com.gymprofit.api.dto.admin.AdminEmbudoDTO;
import com.gymprofit.api.service.admin.IAdminEmbudoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// ============================================================
// AdminEmbudoController — el embudo del alta y el núcleo (GP-019)
// Una sola ruta, solo ADMIN (regla /admin/** de SecurityConfig). Solo recuentos.
// ============================================================
@RestController
@RequestMapping("/admin/embudo")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Panel de administración")
public class AdminEmbudoController {

    private final IAdminEmbudoService servicio;

    @Operation(summary = "Llegadas a cada paso del alta y núcleo de las cuentas nuevas, por semana (ADMIN)")
    @ApiResponse(responseCode = "200", description = "Embudo y núcleo de las 8 últimas semanas")
    @GetMapping
    public ResponseEntity<AdminEmbudoDTO> embudo() {
        return ResponseEntity.ok(servicio.embudo());
    }
}
