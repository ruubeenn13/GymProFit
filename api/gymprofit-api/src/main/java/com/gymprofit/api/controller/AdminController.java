package com.gymprofit.api.controller;

import com.gymprofit.api.dto.admin.AdminEstadisticasDTO;
import com.gymprofit.api.dto.admin.AdminUsuarioDTO;
import com.gymprofit.api.service.usuario.IUsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Panel de administración")
public class AdminController {

    private final IUsuarioService usuarioService;

    @Operation(summary = "Lista usuarios con filtros dinámicos (ADMIN)")
    @ApiResponse(responseCode = "200", description = "Listado de usuarios",
            content = @Content(schema = @Schema(implementation = AdminUsuarioDTO.class)))
    @GetMapping("/usuarios")
    public ResponseEntity<List<AdminUsuarioDTO>> getUsuarios(
            @RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) String rol,
            @RequestParam(required = false) String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(usuarioService.getUsuariosAdmin(activo, rol, username, page, size));
    }

    @Operation(summary = "Estadísticas globales de la aplicación (ADMIN)")
    @ApiResponse(responseCode = "200", description = "Estadísticas globales",
            content = @Content(schema = @Schema(implementation = AdminEstadisticasDTO.class)))
    @GetMapping("/estadisticas-globales")
    public ResponseEntity<AdminEstadisticasDTO> getEstadisticasGlobales() {
        return ResponseEntity.ok(usuarioService.getEstadisticasGlobales());
    }
}
