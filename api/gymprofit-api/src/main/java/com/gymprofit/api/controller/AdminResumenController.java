package com.gymprofit.api.controller;

import com.gymprofit.api.dto.admin.AdminResumenDTO;
import com.gymprofit.api.service.admin.IAdminResumenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// ============================================================
// AdminResumenController — la pantalla de Resumen de la web (GP-085)
// Una sola ruta, solo ADMIN (regla /admin/** de SecurityConfig). Solo recuentos.
// ============================================================
@RestController
@RequestMapping("/admin/resumen")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Panel de administración")
public class AdminResumenController {

    private final IAdminResumenService adminResumenService;

    @Operation(summary = "Cuentas, actividad y lo pendiente del catálogo, en hora de Madrid (ADMIN)")
    @ApiResponse(responseCode = "200", description = "Resumen")
    @GetMapping
    public ResponseEntity<AdminResumenDTO> resumen() {
        return ResponseEntity.ok(adminResumenService.resumen());
    }
}
